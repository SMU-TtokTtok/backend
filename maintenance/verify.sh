#!/usr/bin/env bash
#
# Maintenance harness verification gate (HARNESS.md step 4)
# Forces build + tests to run together.
#
# CI (ci-cd.yml) is a deployment build and skips tests with `clean build -x test`.
# This gate intentionally omits `-x test` to enforce tests — the harness fills the CI gap here.
#
# Usage:  bash maintenance/verify.sh
# (On a pure PowerShell environment, use instead:  ./gradlew.bat clean build)

set -euo pipefail

# Move to the repo root (works even though this script lives under maintenance/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Use gradlew.bat on Windows (Git Bash), ./gradlew otherwise
if [ -f "./gradlew" ] && command -v sh >/dev/null 2>&1 && [ -z "${WINDIR:-}" ]; then
  GRADLE="./gradlew"
elif [ -f "./gradlew.bat" ]; then
  GRADLE="./gradlew.bat"
else
  GRADLE="./gradlew"
fi

echo "==> Running verification gate: $GRADLE clean build (tests included)"
echo "==> Working directory: $REPO_ROOT"

if "$GRADLE" clean build; then
  echo ""
  echo "✅ VERIFY PASS — build + tests passed (green)"
  exit 0
else
  status=$?
  echo ""
  echo "❌ VERIFY FAIL — build or tests failed (exit=$status)"
  echo "   Test report: build/reports/tests/test/index.html"
  echo "   Return to HARNESS.md step 2-3 and fix the cause."
  exit "$status"
fi
