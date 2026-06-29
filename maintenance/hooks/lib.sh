#!/usr/bin/env bash
#
# Shared library for the maintenance git hooks (sourced by each hook).
# See maintenance/HARNESS.md for the overall harness.

# Protected integration branches — never allow direct commit/push.
PROTECTED_BRANCHES="main develop"

# Allowed branch-name convention: <type>/#<issue-number>
# Types derived from the repo's actual branches.
BRANCH_CONVENTION='^(feat|feature|fix|hotfix|refactor|chore|test|arch|release|Release)/#[0-9]+$'

# Git's "null" object id (all zeros) — length differs for sha1(40)/sha256(64).
# Detected by "contains only zeros" rather than a fixed string.
is_zero_sha() {
  case "$1" in
    *[!0]*) return 1 ;;  # contains a non-zero char -> not the null sha
    "")     return 1 ;;
    *)      return 0 ;;
  esac
}

is_protected() {
  local branch="$1"
  for p in $PROTECTED_BRANCHES; do
    [ "$branch" = "$p" ] && return 0
  done
  return 1
}

# A staged/relative path points at a secret if its basename or location matches.
# Mirrors .gitignore. Profile-split configs (application-dev/prod/local/test.yml)
# are all caught by the application(-<profile>)?.yml pattern.
is_secret_path() {
  local path="$1"
  local base
  base="$(basename "$path")"

  case "$path" in
    src/main/resources/application*.yml|src/test/resources/application*.yml) return 0 ;;
    */db/seed/*|src/main/resources/db/seed/*|src/test/resources/db/seed/*)   return 0 ;;
  esac
  case "$base" in
    application.yml)        return 0 ;;
    ttokttok-push*.json)    return 0 ;;
    *.csv)                  return 0 ;;
  esac
  # application-<profile>.yml regardless of directory
  if printf '%s' "$base" | grep -Eq '^application(-[a-z]+)?\.yml$'; then
    return 0
  fi
  return 1
}

# Red error block + bypass note, then fail the hook.
block() {
  printf '\n\033[31m❌ [maintenance hook] %s\033[0m\n' "$1" >&2
  shift
  for line in "$@"; do
    printf '   %s\n' "$line" >&2
  done
  printf '   (의도적 우회가 꼭 필요하면 --no-verify, 단 책임은 본인에게.)\n\n' >&2
  exit 1
}
