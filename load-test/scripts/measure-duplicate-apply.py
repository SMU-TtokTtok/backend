"""Measure real multipart HTTP requests against the dedicated #426 fixture server.

Requires the existing k6 executable; Python uses only its standard library.
Disposable JWT fixtures live in a temporary directory, never in result files.
"""
import argparse
import itertools
import json
import os
from pathlib import Path
import statistics
import subprocess
import tempfile
import threading
import time
import urllib.parse
import urllib.request


def fetch(control, route, **parameters):
    query = urllib.parse.urlencode(parameters)
    with urllib.request.urlopen(f"{control.rstrip('/')}/{route}?{query}", timeout=120) as response:
        return json.load(response)


def sample_metrics(control, stopped, observations, started):
    while not stopped.is_set():
        tick = time.monotonic()
        try:
            values = fetch(control, "metrics")
            observations.append({"elapsed_seconds": tick - started, **values})
        except (OSError, ValueError) as error:
            observations.append({"elapsed_seconds": tick - started,
                                 "error": type(error).__name__})
        stopped.wait(max(0, .1 - (time.monotonic() - tick)))


def correct(variant, scenario, vus, rounds, warmup, summary, verification):
    total_groups = (rounds + warmup) * (1 if scenario == "hot" else vus)
    measured_groups = rounds * (1 if scenario == "hot" else vus)
    measured_requests = rounds * vus
    result = {
        "complete_measured_requests": sum(summary[key] for key in
            ("accepted", "conflicts", "unexpected")) == measured_requests,
        "no_unexpected_http": summary["unexpected"] == 0,
        "expected_group_count": verification["expectedGroups"] == total_groups,
        "no_missing_groups": verification["missingGroups"] == 0,
        "documents_match_applications": verification["documents"] == verification["applications"],
    }
    if variant != "V0":
        result.update({
            "one_application_per_group": verification["applications"] == total_groups,
            "no_duplicate_groups": verification["duplicateGroups"] == 0,
            "expected_successes": summary["accepted"] == measured_groups,
            "expected_conflicts": summary["conflicts"] == measured_requests - measured_groups,
        })
    return result


def measure(args, scenario, vus, repetition):
    total_rounds = args.warmup + args.rounds
    fixture = fetch(args.control, "prepare", scenario=scenario, vus=vus, rounds=total_rounds)
    if len(fixture["batches"]) != total_rounds or any(len(batch) != vus for batch in fixture["batches"]):
        raise ValueError("Fixture batch count does not match the requested workload")
    script = Path(__file__).resolve().parents[1] / "k6/scenarios/issue-b-duplicate-apply.js"
    observations, stopped = [], threading.Event()
    started = time.monotonic()
    sampler = threading.Thread(target=sample_metrics,
        args=(args.control, stopped, observations, started), daemon=True)
    with tempfile.TemporaryDirectory(prefix="ttokttok426-") as temporary:
        fixture_path = Path(temporary) / "fixture.json"
        summary_path = Path(temporary) / "summary.json"
        fixture_path.write_text(json.dumps(fixture), encoding="utf-8")
        environment = {**os.environ, "DATA_FILE": str(fixture_path),
            "SUMMARY_FILE": str(summary_path), "SERVERS": args.server,
            "WARMUP": str(args.warmup)}
        sampler.start()
        try:
            completed = subprocess.run([args.k6, "run", "--quiet", str(script)],
                env=environment, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                text=True, encoding="utf-8", errors="replace", timeout=1900)
            if completed.returncode != 0 or not summary_path.exists():
                # Do not persist raw k6 diagnostics: request context may include credentials.
                raise RuntimeError(f"k6 did not complete: exit code {completed.returncode}")
            summary = json.loads(summary_path.read_text(encoding="utf-8"))
        finally:
            stopped.set()
            sampler.join(timeout=125)
    verification = fetch(args.control, "verify")
    checks = correct(args.variant, scenario, vus, args.rounds, args.warmup, summary, verification)
    return {"variant": args.variant, "scenario": scenario, "vus": vus,
        "repetition": repetition, "servers": len(args.server.split(',')),
        "summary": summary, "verification": verification, "checks": checks,
        "passed": all(checks.values()), "db_samples": observations,
        "db_sample_window": "whole k6 invocation including startup, warmup and measured rounds"}


def comparison(results):
    grouped = {}
    for row in results:
        key = f"{row['scenario']}-{row['vus']}"
        grouped.setdefault(key, []).append(row)
    report = {}
    for key, rows in grouped.items():
        measures = {}
        for metric in ("request_rps", "new_application_rps", "measured_seconds"):
            values = [row["summary"][metric] for row in rows]
            measures[metric] = {"median": statistics.median(values), "min": min(values), "max": max(values)}
        for outcome in ("accepted", "conflict"):
            for quantile in ("p(50)", "p(95)", "p(99)"):
                values = [row["summary"][f"{outcome}_latency_ms"][quantile] for row in rows
                          if row["summary"][f"{outcome}_latency_ms"] is not None]
                if values:
                    measures[f"{outcome}_{quantile}_ms"] = {
                        "median": statistics.median(values), "min": min(values), "max": max(values)}
        report[key] = {"repetitions": len(rows), "all_checks_passed": all(row["passed"] for row in rows),
                       "metrics": measures}
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", default="http://127.0.0.1:18080")
    parser.add_argument("--control", default="http://127.0.0.1:18081")
    parser.add_argument("--variant", required=True, choices=("V0", "V1", "V2", "V3"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--vus", default="20,50")
    parser.add_argument("--scenarios", default="hot,users,forms")
    parser.add_argument("--repetitions", type=int, default=3)
    parser.add_argument("--rounds", type=int, default=50)
    parser.add_argument("--warmup", type=int, default=10)
    parser.add_argument("--k6", default="k6")
    args = parser.parse_args()
    vus_values = [int(value) for value in args.vus.split(',')]
    scenarios = args.scenarios.split(',')
    if min(vus_values + [args.repetitions, args.rounds]) < 1 or args.warmup < 0:
        parser.error("vus, repetitions and rounds must be positive; warmup must be nonnegative")
    if any(scenario not in ("hot", "users", "forms") for scenario in scenarios):
        parser.error("scenarios must be hot,users,forms")
    for url in args.server.split(',') + [args.control]:
        parsed = urllib.parse.urlparse(url)
        if parsed.scheme != "http" or parsed.hostname != "127.0.0.1" or parsed.username:
            parser.error("Only dedicated loopback HTTP servers are accepted")
    args.output.mkdir(parents=True, exist_ok=True)
    results = []
    for scenario, vus, repetition in itertools.product(scenarios, vus_values, range(1, args.repetitions + 1)):
        row = measure(args, scenario, vus, repetition)
        name = f"{args.variant}-{scenario}-{vus}-r{repetition}.json"
        (args.output / name).write_text(json.dumps(row, indent=2), encoding="utf-8")
        results.append(row)
        print(f"{name}: accepted={row['summary']['accepted']} conflicts={row['summary']['conflicts']} "
              f"unexpected={row['summary']['unexpected']} applications={row['verification']['applications']} "
              f"checks={'PASS' if row['passed'] else 'FAIL'}", flush=True)
    (args.output / f"{args.variant}-comparison.json").write_text(
        json.dumps(comparison(results), indent=2), encoding="utf-8")
    return 1 if args.variant != "V0" and any(not row["passed"] for row in results) else 0


if __name__ == "__main__":
    raise SystemExit(main())
