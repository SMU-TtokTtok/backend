#!/usr/bin/env bash
#
# One-time setup: point git at the versioned hooks directory.
# Run once after cloning:  bash maintenance/hooks/install.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

git config core.hooksPath maintenance/hooks
chmod +x maintenance/hooks/pre-commit maintenance/hooks/commit-msg \
         maintenance/hooks/pre-push maintenance/hooks/install.sh 2>/dev/null || true

echo "✅ git hooks installed (core.hooksPath = maintenance/hooks)"
echo ""
echo "Active guards:"
echo "  • pre-commit : no direct commit on main/develop; block secret files"
echo "  • commit-msg : enforce '[#issue] - message'"
echo "  • pre-push   : block force-push (all branches); no direct push to main/develop; branch-name convention"
