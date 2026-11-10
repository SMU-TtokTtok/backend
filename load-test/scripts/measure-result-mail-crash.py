"""Measure missing and duplicated result mails when the app process is force-killed mid-send (#460).

Local only: one app jar, Mailpit (SMTP 127.0.0.1:1025, API 127.0.0.1:8025) and the dedicated
PostgreSQL container. Python uses only its standard library; psql runs inside the container.

Flow per repetition: reset Mailpit and seed -> start the app -> call send-email once -> kill the app
(taskkill /F, the Windows equivalent of kill -9) once Mailpit holds more than --kill-after mails ->
start the same jar again -> wait until the mail count stops growing (and, for the new code, until
no PENDING/SENDING row is left) -> count missing and duplicate mails.

The JWT secret is generated per run and passed only on the app command line. It is never written
to a result file. App logs go to --log-dir, not to the result file.
"""
import argparse
import base64
import hashlib
import hmac
import json
import secrets
import statistics
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
MAIL_HOST = "127.0.0.1"
SMTP_PORT = 1025
MAILPIT = "http://127.0.0.1:8025"
CLUB_ID = "crash460-club"
ADMIN_USERNAME = "crash460-admin"
POLL_SECONDS = 0.02
MIN_FREE_GB = 0.7
MAILPIT_PAGE = 500
STATE_QUERY = "select status || '=' || count(*) from result_mail_deliveries group by status order by status"


def http(method, url, body=None, headers=None, timeout=30):
    request = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.status, response.read()


def mail_total():
    return json.loads(http("GET", f"{MAILPIT}/api/v1/messages?limit=1")[1])["total"]


def mail_recipients():
    """Every received message's recipient address, in arrival order."""
    recipients, start = [], 0
    while True:
        page = json.loads(http("GET", f"{MAILPIT}/api/v1/messages?start={start}&limit={MAILPIT_PAGE}")[1])
        for message in page["messages"]:
            recipients.extend(to["Address"] for to in message["To"])
        start += MAILPIT_PAGE
        if start >= page["total"]:
            return recipients


def psql(args, sql=None, stdin=None):
    command = ["docker", "exec", "-i", args.pg_container, "psql", "-U", "postgres", "-d", args.database,
               "-v", "ON_ERROR_STOP=1", "-At"]
    if sql is not None:
        command += ["-c", sql]
    completed = subprocess.run(command, input=stdin, capture_output=True, text=True, encoding="utf-8")
    if completed.returncode != 0:
        raise RuntimeError(f"psql failed: {completed.stderr.strip()[:300]}")
    return completed.stdout.strip()


def delivery_states(args):
    """e.g. {'SENT': 1990, 'SENDING': 10}. Empty for the old code (no table rows)."""
    rows = psql(args, STATE_QUERY)
    return {name: int(count) for name, count in (row.split("=") for row in rows.splitlines())}


def jwt(secret_b64, issuer):
    def part(value):
        return base64.urlsafe_b64encode(json.dumps(value, separators=(",", ":")).encode()).rstrip(b"=")

    now = int(time.time())
    signing_input = part({"alg": "HS256"}) + b"." + part(
        {"iss": issuer, "sub": ADMIN_USERNAME, "role": "ROLE_ADMIN", "iat": now, "exp": now + 3600})
    signature = hmac.new(base64.b64decode(secret_b64), signing_input, hashlib.sha256).digest()
    return (signing_input + b"." + base64.urlsafe_b64encode(signature).rstrip(b"=")).decode()


def app_command(args, secret, ddl_auto):
    command = [
        "java", f"-Xmx{args.heap}", "-jar", str(args.jar), "--spring.profiles.active=loadtest",
        f"--spring.config.additional-location=file:{args.config_dir.as_posix().rstrip('/')}/",
        f"--server.port={args.app_port}",
        f"--spring.datasource.url=jdbc:postgresql://127.0.0.1:{args.pg_port}/{args.database}",
        "--spring.datasource.username=postgres", "--spring.datasource.password=",
        "--spring.datasource.driver-class-name=org.postgresql.Driver",
        f"--spring.jpa.hibernate.ddl-auto={ddl_auto}", "--spring.flyway.enabled=false",
        f"--spring.mail.host={MAIL_HOST}", f"--spring.mail.port={SMTP_PORT}",
        "--spring.mail.username=", "--spring.mail.password=",
        "--spring.mail.properties.mail.smtp.auth=false",
        "--spring.mail.properties.mail.smtp.starttls.enable=false",
        "--spring.mail.properties.mail.smtp.ssl.enable=false",
        f"--spring.data.redis.host=127.0.0.1", f"--spring.data.redis.port={args.redis_port}",
        "--spring.data.redis.password=",
        "--jwt.issuer=loadtest460", f"--jwt.secret={secret}",
        "--email.from.address=sender@example.test", "--email.from.name=ttokttok460",
        "--email.reply-to=sender@example.test",
        "--firebase.json=not-used",
        f"--firebase.credentials-location=file:{args.config_dir.as_posix().rstrip('/')}/{args.firebase_file}",
        "--cloud.aws.credentials.access-key=x", "--cloud.aws.credentials.secret-key=x",
        "--cloud.aws.region.static=ap-northeast-2", "--cloud.aws.s3.bucket=x",
        "--cloud.aws.s3.endpoint=http://127.0.0.1:1", "--file-cloud.url=http://127.0.0.1:1",
        f"--server.url=http://127.0.0.1:{args.app_port}",
        "--google.oauth.jwk-set-uri=http://127.0.0.1:1", "--google.oauth.client-id=x",
        "--logging.level.root=INFO",
    ]
    if args.variant == "after":
        command.append(f"--result-mail.worker.poll-delay-ms={args.poll_delay_ms}")
    # Never let a run reach a real SMTP server.
    assert f"--spring.mail.host={MAIL_HOST}" in command and f"--spring.mail.port={SMTP_PORT}" in command
    return command


def start_app(args, secret, log_name, ddl_auto="validate"):
    log = open(args.log_dir / log_name, "wb")
    started = time.monotonic()
    process = subprocess.Popen(app_command(args, secret, ddl_auto), stdout=log, stderr=subprocess.STDOUT)
    log.close()
    deadline = started + 180
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"app exited during startup (see {log_name}), exit code {process.returncode}")
        try:
            http("GET", f"http://127.0.0.1:{args.app_port}/", timeout=2)
        except urllib.error.HTTPError:
            break  # any HTTP answer means the server is up
        except OSError:
            time.sleep(.3)
            continue
        break
    else:
        process.kill()
        raise RuntimeError("app did not start within 180 seconds")
    return process, started, time.monotonic() - started


def kill_app(process):
    command = ["taskkill", "/F", "/PID", str(process.pid)]
    subprocess.run(command, capture_output=True, check=True)
    process.wait(timeout=30)
    return " ".join(command)


def settled_total(quiet_seconds=2.0):
    """Mailpit keeps ingesting mails the SMTP side already accepted; wait until the count is stable."""
    last, since = mail_total(), time.monotonic()
    while time.monotonic() - since < quiet_seconds:
        time.sleep(.2)
        current = mail_total()
        if current != last:
            last, since = current, time.monotonic()
    return last


def prepare_schema(args):
    """ddl-auto=create builds the whole schema once; then swap the generated table for the V32 SQL."""
    secret = base64.b64encode(secrets.token_bytes(32)).decode()
    process, _, _ = start_app(args, secret, "schema-create.log", ddl_auto="create")
    kill_app(process)
    migration = REPO / "src/main/resources/db/migration/V32__create_result_mail_deliveries_table.sql"
    psql(args, "DROP TABLE IF EXISTS result_mail_deliveries")
    psql(args, stdin=migration.read_text(encoding="utf-8"))
    print("schema prepared: ddl-auto=create, then result_mail_deliveries replaced by V32", flush=True)


def reset(args):
    http("DELETE", f"{MAILPIT}/api/v1/messages")
    seed = (REPO / "load-test/sql/seed-result-mail.sql").read_text(encoding="utf-8")
    psql_args = ["-v", f"pass_count={args.pass_count}", "-v", f"fail_count={args.fail_count}"]
    command = ["docker", "exec", "-i", args.pg_container, "psql", "-U", "postgres", "-d", args.database,
               "-v", "ON_ERROR_STOP=1", "-q"] + psql_args
    completed = subprocess.run(command, input=seed, capture_output=True, text=True, encoding="utf-8")
    if completed.returncode != 0:
        raise RuntimeError(f"seed failed: {completed.stderr.strip()[:300]}")
    if mail_total() != 0:
        raise RuntimeError("Mailpit was not emptied")


def send_email(args, secret):
    body = json.dumps({"pass": {"title": "crash460 pass", "body": "crash460 pass body"},
                       "fail": {"title": "crash460 fail", "body": "crash460 fail body"}}).encode()
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {jwt(secret, 'loadtest460')}"}
    started = time.monotonic()
    status, response = http(
        "POST", f"http://127.0.0.1:{args.app_port}/api/admin/applies/{CLUB_ID}/send-email?kind=DOCUMENT",
        body, headers, timeout=120)
    return status, response.decode("utf-8"), time.monotonic() - started


def wait_for_completion(args, restart_started, ready_at, is_after):
    """Ends after a stall of --stall-seconds (after: also no PENDING/SENDING row).

    Returns (stop_reason, last_mail_monotonic, pending_zero_monotonic). pending_zero is the first time no
    PENDING row was left, so only SENDING rows (waiting for lease recovery) remain; None for the old code.
    """
    total = mail_total()
    last_increase = None
    pending_zero = None
    stalled_from = ready_at
    next_db_check = 0
    while True:
        now = time.monotonic()
        current = mail_total()
        if current > total:
            total, last_increase, stalled_from = current, now, now
        if now - restart_started > args.max_wait_seconds:
            return "timeout", last_increase, pending_zero
        if is_after and now >= next_db_check:
            next_db_check = now + 2
            states = delivery_states(args)
            if pending_zero is None and states.get("PENDING", 0) == 0:
                pending_zero = now
            if now - stalled_from >= args.stall_seconds and states.get("PENDING", 0) + states.get("SENDING", 0) == 0:
                return "drained", last_increase, pending_zero
        if not is_after and now - stalled_from >= args.stall_seconds:
            return "stalled", last_increase, None
        time.sleep(.5)


def free_memory_gb():
    out = subprocess.run(["powershell", "-NoProfile", "-Command",
                          "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1MB"],
                         capture_output=True, text=True, check=True).stdout
    return float(out.strip().replace(",", "."))


def run_once(args, repetition):
    free = free_memory_gb()
    if free < MIN_FREE_GB:
        raise RuntimeError(f"free memory {free:.2f} GB is below {MIN_FREE_GB} GB; not starting repetition {repetition}")
    is_after = args.variant == "after"
    secret = base64.b64encode(secrets.token_bytes(32)).decode()
    reset(args)
    process, _, ready_seconds = start_app(args, secret, f"{args.variant}-r{repetition}-first.log")
    expected = args.pass_count + args.fail_count
    try:
        status, response, request_seconds = send_email(args, secret)
        while mail_total() <= args.kill_after:
            if process.poll() is not None:
                raise RuntimeError("app exited before the kill threshold")
            time.sleep(POLL_SECONDS)
        detected = mail_total()
        sending_seen = None
        if is_after:
            # Kill while a claimed batch is in flight (a SENDING row exists); give up waiting after --sending-wait-seconds.
            deadline = time.monotonic() + args.sending_wait_seconds
            sending_seen = False
            while not sending_seen and time.monotonic() < deadline:
                sending_seen = delivery_states(args).get("SENDING", 0) > 0
        kill_command = kill_app(process)
    finally:
        if process.poll() is None:
            process.kill()
    received_at_kill = settled_total()
    db_at_kill = delivery_states(args) if is_after else None

    restart_started = time.monotonic()
    process, _, restart_ready_seconds = start_app(args, secret, f"{args.variant}-r{repetition}-second.log")
    try:
        ready_at = time.monotonic()
        stop_reason, last_increase, pending_zero = wait_for_completion(args, restart_started, ready_at, is_after)
        settled_total()
    finally:
        kill_app(process)  # the run is over; nothing is in flight

    recipients = mail_recipients()
    distinct = len(set(recipients))
    row = {
        "repetition": repetition,
        "expected_recipients": expected,
        "received_total": len(recipients),
        "received_distinct": distinct,
        "missing": expected - distinct,
        "duplicates": len(recipients) - distinct,
        "received_at_kill_detected": detected,
        "received_after_kill_settled": received_at_kill,
        "seconds_restart_to_last_mail": None if last_increase is None else round(last_increase - restart_started, 1),
        "stop_reason": stop_reason,
        "send_email_http_status": status,
        "send_email_response_seconds": round(request_seconds, 3),
        "app_ready_seconds": round(ready_seconds, 1),
        "restart_ready_seconds": round(restart_ready_seconds, 1),
        "kill_command": kill_command,
    }
    if is_after:
        row["sending_row_seen_before_kill"] = sending_seen
        row["seconds_restart_to_pending_zero"] = None if pending_zero is None else round(pending_zero - restart_started, 1)
    if is_after:
        row["delivery_states_at_kill"] = db_at_kill
        row["delivery_states_final"] = delivery_states(args)
        row["rows_with_attempt_count_over_1"] = int(psql(
            args, "select count(*) from result_mail_deliveries where attempt_count > 1"))
    return row


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--variant", required=True, choices=("before", "after"),
                        help="before = old code (@Async mail executor), after = DB outbox worker")
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--code-ref", default="unknown", help="commit of the jar, recorded in the result")
    parser.add_argument("--output", type=Path, help="result JSON (required unless --prepare-schema)")
    parser.add_argument("--prepare-schema", action="store_true",
                        help="build the schema with ddl-auto=create, then apply V32; --jar must be the new code")
    parser.add_argument("--repetitions", type=int, default=3)
    parser.add_argument("--kill-after", type=int, default=500, help="kill once Mailpit holds more than this many mails")
    parser.add_argument("--stall-seconds", type=int, default=30)
    parser.add_argument("--max-wait-seconds", type=int, default=360)
    parser.add_argument("--sending-wait-seconds", type=int, default=5,
                        help="after only: how long to wait for a SENDING row before killing anyway")
    parser.add_argument("--poll-delay-ms", type=int, default=5000, help="after only: result-mail.worker.poll-delay-ms")
    parser.add_argument("--heap", default="512m", help="app JVM -Xmx")
    parser.add_argument("--pass-count", type=int, default=1000)
    parser.add_argument("--fail-count", type=int, default=1000)
    parser.add_argument("--pg-container", default="ttokttok-426-repro-pg")
    parser.add_argument("--pg-port", type=int, help="host port of the container's 5432 (default: docker port)")
    parser.add_argument("--database", default="ttokttok460")
    parser.add_argument("--redis-port", type=int, default=6460)
    parser.add_argument("--app-port", type=int, default=18460)
    parser.add_argument("--config-dir", type=Path, default=REPO / "src/main/resources",
                        help="--spring.config.additional-location; the gitignored yml files are read from here, never copied")
    parser.add_argument("--firebase-file", default="ttokttok-push-firebase-adminsdk-fbsvc-9af12cbea9.json")
    parser.add_argument("--log-dir", type=Path, default=Path("build/crash-measure-logs"))
    args = parser.parse_args()
    if not args.prepare_schema and args.output is None:
        parser.error("--output is required")
    if args.pg_port is None:
        out = subprocess.run(["docker", "port", args.pg_container, "5432"], capture_output=True, text=True,
                             check=True).stdout.splitlines()[0]
        args.pg_port = int(out.rsplit(":", 1)[1])
    args.log_dir.mkdir(parents=True, exist_ok=True)
    if args.prepare_schema:
        prepare_schema(args)
        return 0
    rows = []
    for repetition in range(1, args.repetitions + 1):
        row = run_once(args, repetition)
        rows.append(row)
        print(f"{args.variant} r{repetition}: expected={row['expected_recipients']} total={row['received_total']} "
              f"distinct={row['received_distinct']} missing={row['missing']} duplicates={row['duplicates']} "
              f"at_kill={row['received_at_kill_detected']} stop={row['stop_reason']}", flush=True)
        report = {
            "variant": args.variant, "code_ref": args.code_ref, "kill_after_mails": args.kill_after, "app_heap": args.heap,
            "stall_seconds": args.stall_seconds, "max_wait_seconds": args.max_wait_seconds,
            "poll_delay_ms": args.poll_delay_ms if args.variant == "after" else None,
            "mail_host": f"{MAIL_HOST}:{SMTP_PORT}", "runs": rows,
            "summary": {key: {"median": statistics.median(r[key] for r in rows),
                              "min": min(r[key] for r in rows), "max": max(r[key] for r in rows)}
                        for key in ("received_total", "received_distinct", "missing", "duplicates")},
        }
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
