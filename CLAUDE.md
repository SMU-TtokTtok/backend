# CLAUDE.md

Claude Code entry point for **ttokttok**. This file only routes — the actual content lives in the shared docs below (kept single-source to avoid duplication).

## Start here

- **Maintenance work** → follow the 5-step loop in [`maintenance/HARNESS.md`](maintenance/HARNESS.md) (Intake → Orient → Change → Verify → Record).
- **Coding rules & conventions** → [`AGENTS.md`](AGENTS.md).
- **Project facts, stack, build/run, commit convention** → [`GEMINI.md`](GEMINI.md).
- **Work log** → append to [`IMPLEMENTATION.md`](IMPLEMENTATION.md).

## Claude Code operating hints

- **Setup (once per clone):** `bash maintenance/hooks/install.sh` installs git safety guards (no force-push, no direct commit/push to main/develop, commit-message & branch-name conventions, secret guard).
- **OS:** Windows. Prefer `./gradlew.bat clean build`; the harness gate `bash maintenance/verify.sh` works in Git Bash.
- **Verification gate:** a task is not done until `verify.sh` is green (build + tests). CI skips tests, so this is enforced locally.
- **Secrets:** never commit `application*.yml` or Firebase `*.json`. Run `git status` before committing.
- **Commit/PR:** `[#issue] - message` (Korean), fill `.github/PULL_REQUEST_TEMPLATE.md`.
- This repo also runs under oh-my-claudecode (OMC); the user's global `~/.claude/CLAUDE.md` merges with this file.
