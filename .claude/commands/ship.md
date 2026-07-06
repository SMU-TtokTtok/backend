---
description: Run the full ttokttok task pipeline — issue, branch, worktree, implement, verify, PR
argument-hint: <task description> [--type feature|fix|refactor|chore|test|arch|hotfix]
---

Follow the `ship` project skill end-to-end for this task: $ARGUMENTS

Read `.omc/skills/ship/SKILL.md` at the project root and execute its full protocol in order: issue creation, branch + worktree creation, implementation, the verify gate (cap at 3 retries), `IMPLEMENTATION.md` recording, commit, push, and PR creation against `develop`. Follow the type-mapping table in that file for issue template/label/branch-prefix selection.

Run fully autonomously — do not pause for confirmation between steps, per the skill's protocol. If `release` is requested, refuse and point at the release-PR section of `.github/PULL_REQUEST_TEMPLATE.md` instead.
