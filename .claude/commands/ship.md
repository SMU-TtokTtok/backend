---
description: Run the full ttokttok task pipeline — issue, branch, worktree, implement, verify, PR
argument-hint: <task description> [--type feature|fix|refactor|chore|test|arch|hotfix]
---

Follow the `ship` project skill end-to-end for this task: $ARGUMENTS

Read `.omc/skills/ship/SKILL.md` at the project root and execute its full protocol in order: issue creation, branch + worktree creation, implementation, the verify gate (cap at 3 retries), the commit-split plan, one commit per slice, the per-commit compile gate, `IMPLEMENTATION.md` recording, push, and PR creation against `develop`. Follow the type-mapping table in that file for issue template/label/branch-prefix selection.

Commits must follow `rules/agent/commit-granularity.md` (one concern per commit) and carry no AI signatures — no `Co-Authored-By: <model>`, no `🤖 Generated with…` — in either commit messages or the PR body.

Run fully autonomously — do not pause for confirmation between steps, per the skill's protocol. If `release` is requested, refuse and point at the release-PR section of `.github/PULL_REQUEST_TEMPLATE.md` instead.
