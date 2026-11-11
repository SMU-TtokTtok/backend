"""Measure missing mails and recovery time when the SMTP server stops answering for a while (#460).

Local only: one app jar, Mailpit (SMTP 127.0.0.1:1025, API 127.0.0.1:8025) and the dedicated
PostgreSQL container. Reuses the helpers of measure-result-mail-crash.py (same environment, seed, jars).

Flow per repetition: reset Mailpit and seed -> start the app -> call send-email once -> once Mailpit
holds more than --pause-after mails, `docker pause` the Mailpit container (connections still open,
nothing answers, so the app's SMTP read timeout of #435 U2 fires) -> `docker unpause` after
--pause-seconds -> wait until the work is done (new code: no PENDING/SENDING row; old code: the mail
count stops growing) -> count missing and duplicate mails and measure unpause -> last mail.

Mailpit's API is unreachable while the container is paused, so nothing reads it during the pause.
The JWT secret is generated per run and passed only on the app command line. App logs go to --log-dir.
"""
import argparse
import base64
import importlib.util
import json
import re
import secrets
import statistics
import subprocess
import time
from datetime import datetime
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("crash", REPO / "load-test/scripts/measure-result-mail-crash.py")
crash = importlib.util.module_from_spec(spec)
spec.loader.exec_module(crash)

ATTEMPT_QUERY = ("select status || ':' || attempt_count || '=' || count(*) from result_mail_deliveries "
                 "group by status, attempt_count order by status, attempt_count")
SEND_FAILURE = re.compile(rb"- (Mail\w*Exception)")


def attempt_distribution(args):
    """e.g. {'SENT:1': 292, 'SENT:2': 8}. Empty for the old code (no rows)."""
    rows = crash.psql(args, ATTEMPT_QUERY)
    return {name: int(count) for name, count in (row.split("=") for row in rows.splitlines())}


def sent_at_offsets(args, unpaused_wall):
    """Seconds from unpause to the first-attempt mails' last send and to the retried mails' first and last send."""
    out = crash.psql(args, "select coalesce(max(sent_at) filter (where attempt_count = 1)::text, '') || '|' || "
                           "coalesce(min(sent_at) filter (where attempt_count > 1)::text, '') || '|' || "
                           "coalesce(max(sent_at) filter (where attempt_count > 1)::text, '') "
                           "from result_mail_deliveries")
    names = ("seconds_unpause_to_last_first_attempt_mail", "seconds_unpause_to_first_retried_mail",
             "seconds_unpause_to_last_retried_mail")
    return {name: None if not value else round((datetime.fromisoformat(value) - unpaused_wall).total_seconds(), 1)
            for name, value in zip(names, out.split("|"))}


def docker(action, container):
    subprocess.run(["docker", action, container], capture_output=True, check=True)


def send_failures_in_log(path):
    """Failed send attempts the app logged (EmailService warns '<masked address> - <exception class>')."""
    counts = {}
    for name in SEND_FAILURE.findall(path.read_bytes()):
        counts[name.decode()] = counts.get(name.decode(), 0) + 1
    return counts


def wait_for_completion(args, unpaused_at, is_after):
    """Returns (stop_reason, last_mail_monotonic, seen, drained_monotonic).

    New code ends once no PENDING/SENDING row is left; old code once the count has not grown for
    --stall-seconds. The DB is read before Mailpit so the last mail is not missed (#460 task 6).
    """
    total = crash.mail_total()
    last_increase, stalled_from = None, unpaused_at
    while True:
        now = time.monotonic()
        states = crash.delivery_states(args) if is_after else None
        current = crash.mail_total()
        if current > total:
            total, last_increase, stalled_from = current, now, now
        if now - unpaused_at > args.max_wait_seconds:
            return "timeout", last_increase, total, None
        if is_after and states.get("PENDING", 0) + states.get("SENDING", 0) == 0:
            drained_at = now
            if crash.settled_total() > total:
                last_increase = time.monotonic()
                total = crash.mail_total()
            return "drained", last_increase, total, drained_at
        if not is_after and now - stalled_from >= args.stall_seconds:
            return "stalled", last_increase, total, None
        time.sleep(.3)


def run_once(args, repetition):
    free = crash.free_memory_gb()
    if free < crash.MIN_FREE_GB:
        raise RuntimeError(f"free memory {free:.2f} GB is below {crash.MIN_FREE_GB} GB; not starting repetition {repetition}")
    is_after = args.variant == "after"
    secret = base64.b64encode(secrets.token_bytes(32)).decode()
    crash.reset(args)
    log_name = f"outage-{args.variant}-r{repetition}.log"
    process, _, ready_seconds = crash.start_app(args, secret, log_name)
    paused = False
    try:
        status, response, request_seconds = crash.send_email(args, secret)
        while crash.mail_total() <= args.pause_after:
            if process.poll() is not None:
                raise RuntimeError("app exited before the pause threshold")
            time.sleep(crash.POLL_SECONDS)
        detected = crash.mail_total()
        docker("pause", args.mailpit_container)
        paused = True
        paused_at = time.monotonic()
        timeline = []
        while time.monotonic() - paused_at < args.pause_seconds:
            if is_after:
                timeline.append({"seconds_since_pause": round(time.monotonic() - paused_at, 1),
                                 "delivery_states": crash.delivery_states(args)})
            time.sleep(min(10, max(0, args.pause_seconds - (time.monotonic() - paused_at))))
        states_at_unpause = attempt_distribution(args) if is_after else None
        docker("unpause", args.mailpit_container)
        paused = False
        unpaused_at = time.monotonic()
        unpaused_wall = datetime.now()  # compared with sent_at (app local time, same machine)
        stop_reason, last_increase, seen, drained_at = wait_for_completion(args, unpaused_at, is_after)
    finally:
        if paused:
            docker("unpause", args.mailpit_container)
        crash.kill_app(process)  # the run is over; nothing is in flight

    recipients = crash.mail_recipients()
    distinct = len(set(recipients))
    expected = args.pass_count + args.fail_count
    row = {
        "repetition": repetition,
        "expected_recipients": expected,
        "received_total": len(recipients),
        "received_distinct": distinct,
        "missing": expected - distinct,
        "duplicates": len(recipients) - distinct,
        "received_at_pause_detected": detected,
        "pause_seconds": args.pause_seconds,
        "seconds_unpause_to_last_mail": None if last_increase is None else round(last_increase - unpaused_at, 1),
        "stop_reason": stop_reason,
        "send_failures_logged": send_failures_in_log(args.log_dir / log_name),
        "send_email_http_status": status,
        "send_email_response_seconds": round(request_seconds, 3),
        "app_ready_seconds": round(ready_seconds, 1),
    }
    if is_after:
        row["seconds_unpause_to_queue_empty"] = None if drained_at is None else round(drained_at - unpaused_at, 1)
        row["delivery_attempts_at_unpause"] = states_at_unpause
        row["delivery_attempts_final"] = attempt_distribution(args)
        row["final_failed"] = crash.delivery_states(args).get("FAILED", 0)
        row["rows_with_attempt_count_over_1"] = int(crash.psql(
            args, "select count(*) from result_mail_deliveries where attempt_count > 1"))
        row.update(sent_at_offsets(args, unpaused_wall))
        row["timeline_during_pause"] = timeline
    return row


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--variant", required=True, choices=("before", "after"),
                        help="before = old code (@Async mail executor), after = DB outbox worker")
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--code-ref", default="unknown", help="commit of the jar, recorded in the result")
    parser.add_argument("--output", type=Path, required=True, help="result JSON")
    parser.add_argument("--resume", action="store_true",
                        help="keep the runs already in --output and measure only the missing repetitions")
    parser.add_argument("--repetitions", type=int, default=3)
    parser.add_argument("--pause-after", type=int, default=100, help="pause once Mailpit holds more than this many mails")
    parser.add_argument("--pause-seconds", type=int, default=60)
    parser.add_argument("--stall-seconds", type=int, default=30, help="old code: no growth for this long ends the run")
    parser.add_argument("--max-wait-seconds", type=int, default=1200, help="after unpause, give up after this long")
    parser.add_argument("--poll-delay-ms", type=int, default=5000, help="after only: result-mail.worker.poll-delay-ms")
    parser.add_argument("--heap", default="512m", help="app JVM -Xmx")
    parser.add_argument("--pass-count", type=int, default=150)
    parser.add_argument("--fail-count", type=int, default=150)
    parser.add_argument("--mailpit-container", default="ttokttok-460-mailpit")
    parser.add_argument("--pg-container", default="ttokttok-426-repro-pg")
    parser.add_argument("--pg-port", type=int, help="host port of the container's 5432 (default: docker port)")
    parser.add_argument("--database", default=crash.MEASURE_DATABASE, help="must be ttokttok460")
    parser.add_argument("--redis-port", type=int, default=6460)
    parser.add_argument("--app-port", type=int, default=18460)
    parser.add_argument("--config-dir", type=Path, default=REPO / "src/main/resources",
                        help="--spring.config.additional-location; the gitignored yml files are read from here, never copied")
    parser.add_argument("--firebase-file",
                        help="service account key file name inside --config-dir (default: the one *firebase*.json found there)")
    parser.add_argument("--log-dir", type=Path, default=Path("build/outage-measure-logs"))
    args = parser.parse_args()
    if args.database != crash.MEASURE_DATABASE:
        parser.error(f"this tool truncates and drops tables; only the {crash.MEASURE_DATABASE} database is allowed")
    if args.mailpit_container != "ttokttok-460-mailpit":
        parser.error("only the ttokttok-460-mailpit container may be paused")
    if args.firebase_file is None:
        keys = sorted(args.config_dir.glob("*firebase*.json"))
        if len(keys) != 1:
            parser.error(f"expected exactly one *firebase*.json in {args.config_dir}; pass --firebase-file")
        args.firebase_file = keys[0].name
    if args.pg_port is None:
        out = subprocess.run(["docker", "port", args.pg_container, "5432"], capture_output=True, text=True,
                             check=True).stdout.splitlines()[0]
        args.pg_port = int(out.rsplit(":", 1)[1])
    args.log_dir.mkdir(parents=True, exist_ok=True)
    rows = json.loads(args.output.read_text(encoding="utf-8"))["runs"] if args.resume and args.output.exists() else []
    for repetition in range(len(rows) + 1, args.repetitions + 1):
        row = run_once(args, repetition)
        rows.append(row)
        print(f"{args.variant} r{repetition}: expected={row['expected_recipients']} total={row['received_total']} "
              f"distinct={row['received_distinct']} missing={row['missing']} duplicates={row['duplicates']} "
              f"recovery={row['seconds_unpause_to_last_mail']} stop={row['stop_reason']}", flush=True)
        report = {
            "variant": args.variant, "code_ref": args.code_ref, "pause_after_mails": args.pause_after,
            "pause_seconds": args.pause_seconds, "app_heap": args.heap, "stall_seconds": args.stall_seconds,
            "max_wait_seconds": args.max_wait_seconds,
            "poll_delay_ms": args.poll_delay_ms if args.variant == "after" else None,
            "mail_host": f"{crash.MAIL_HOST}:{crash.SMTP_PORT}", "runs": rows,
            "summary": {key: {"median": statistics.median(r[key] for r in rows),
                              "min": min(r[key] for r in rows), "max": max(r[key] for r in rows)}
                        for key in ("received_total", "received_distinct", "missing", "duplicates",
                                    "seconds_unpause_to_last_mail")},
        }
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
