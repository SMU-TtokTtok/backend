---
name: ship
description: End-to-end automation for ttokttok tasks — GitHub issue, branch, worktree, implementation, verify, commit, push, PR
aliases: [ship-task]
argument-hint: "<task description> [--type feature|fix|refactor|chore|test|arch|hotfix]"
level: 2
triggers:
  - "새 작업"
  - "새 이슈"
  - "이슈 생성"
  - "이슈 만들고"
  - "이슈부터 pr"
  - "브랜치 만들고"
  - "worktree 만들고"
  - "pr 생성"
  - "pr까지"
  - "ship"
---

# Ship Skill

Automates the full `ttokttok` task pipeline described in [`maintenance/HARNESS.md`](../../../maintenance/HARNESS.md):

```
issue → branch/worktree → implement (Change) → verify → record → commit → push → PR
```

Runs fully autonomously once invoked — no confirmation checkpoints between steps. It stops and reports (never fakes success) if the verification gate keeps failing.

Out of scope: `release` branches/PRs (the `develop → main` release flow documented in the lower half of [`.github/PULL_REQUEST_TEMPLATE.md`](../../../.github/PULL_REQUEST_TEMPLATE.md)). If asked for a release, say so and point at that template instead of running this pipeline.

## Type mapping

| Input type | Issue template (section skeleton only) | Label (`gh label list`) | Branch prefix |
|---|---|---|---|
| feature / feat | `.github/ISSUE_TEMPLATE/feature-template.md` | `Feature - 새 기능` | `feat` |
| fix / bug | `.github/ISSUE_TEMPLATE/bug-template.md` | `Fix - 수정 사항` | `fix` |
| hotfix | same as fix | `Fix - 수정 사항` | `hotfix` |
| refactor | `.github/ISSUE_TEMPLATE/refactor-template.md` | `Refactor - 리펙토링` | `refactor` |
| chore | generic skeleton (below) | `Chore - 세팅` | `chore` |
| test | generic skeleton (below) | `Test - 테스트` | `test` |
| arch | generic skeleton (below) | `Arch - 아키텍처 설계 관련` | `arch` |

The three dedicated templates in `.github/ISSUE_TEMPLATE/` currently contain boilerplate unrelated to this project (leftover from a different app). **Reuse only their section headers** — `📝 설명`, `✅ 할 일 목록`, `💬 기타 참고 사항` — and always write fresh body content. For types without a dedicated template, use the same three-section skeleton.

Every issue body must also cover the fields from [`maintenance/task.md`](../../../maintenance/task.md): task type, affected domain (`domain/<name>`, or "N/A" for non-domain/tooling work), affected layers, DB-change flag, and acceptance criteria — this is what makes the issue double as the HARNESS Intake artifact.

## Protocol

1. **Intake** — parse title, type, domain, acceptance criteria from the task description. If something can't be inferred, note it as "TBD" in the issue body rather than asking the user (this skill is fully autonomous).

2. **Create the issue**:
   ```bash
   gh issue create --title "<title>" --body-file <generated body> --label "<mapped label>"
   ```
   Parse the returned issue number/URL from the output.

3. **Create branch + worktree**. `EnterWorktree`'s `name` parameter cannot contain `#` (allowed chars: letters/digits/dots/underscores/dashes), but the repo's branch convention (`rules/agent/branch-naming.md`) requires a literal `#`. So create the worktree manually and then switch the session into it:
   ```bash
   git fetch origin develop
   git worktree add ".claude/worktrees/<prefix>-<issue-number>" -b "<prefix>/#<issue-number>" origin/develop
   ```
   Immediately run `git branch --unset-upstream` inside the new worktree — `git worktree add -b <branch> origin/develop` auto-sets upstream tracking to `origin/develop`, and a bare `git push` would then target the protected `develop` branch instead of creating a new remote branch.
   Then call `EnterWorktree({ path: ".claude/worktrees/<prefix>-<issue-number>" })` to switch the session's cwd. If the tool reports the path is already the current working directory (this can happen if a prior Bash `cd` already moved the shell there), that's fine — no further action needed, just confirm with `pwd` / `git status --short --branch`.

4. **Implement (Change)** — follow `AGENTS.md` conventions. Use the per-type mini checklist in `maintenance/task.md` as the working checklist.

5. **Verify**:
   ```bash
   bash maintenance/verify.sh
   ```
   On failure, go back to step 4 and retry. **Cap at 3 verify attempts.** If still failing after 3, stop — do not commit/push/open a PR. Report the failure with the test report path (`build/reports/tests/test/index.html`) and leave the worktree in place for manual inspection.

6. **Record** — append a dated section to `IMPLEMENTATION.md`, following its existing format (see prior entries: dated header, 주요 작업 내용, 기술적 세부 사항, 업데이트된 파일, 최종 확인 사항).

7. **Commit** — re-run `git status` to confirm no secret files (`application*.yml`, Firebase `*.json`) are staged, then commit with:
   ```
   [#<issue-number>] - <message>

   - <item>
   - <item>
   ```
   (Korean header, per `rules/agent/commit-message.md`. The `commit-msg` hook enforces the format.)

8. **Push**:
   ```bash
   git push -u origin "<prefix>/#<issue-number>"
   ```
   The `pre-push` hook enforces branch naming, blocks force-push, and blocks direct pushes to `main`/`develop` — do not bypass it.

9. **Create the PR** against `develop`, using only the upper half of `.github/PULL_REQUEST_TEMPLATE.md` (everything above the "아래 부터 `develop -> main` PR 템플릿입니다" divider — the release-PR section below it does not apply here):
   ```bash
   gh pr create --base develop --head "<branch>" --title "<title>" --body-file <generated PR body> --label "<mapped label>"
   ```
   Auto-check the checklist items you can actually verify (로컬 테스트 완료 — since verify.sh passed; 라벨을 붙혔나요 — yes; 팀 코드 컨벤션 준수 — yes), fill in 관련 이슈 with `#<issue-number>`, and summarize the change in 기타 참고 사항.

10. **Cleanup** — leave the worktree and branch on disk (for review follow-up commits); do not remove them automatically. If you used `EnterWorktree`, you may call `ExitWorktree({action: "keep"})` to return the parent session to its original directory without deleting anything.

11. **Report** — output the issue URL, PR URL, branch name, worktree path, and a one-line verify-status summary.

## Safety

- Never bypass the git hooks in `maintenance/hooks/` (secret guard, protected-branch guard, branch-naming/commit-message checks, no-force-push). They already enforce the conventions this skill relies on.
- Never claim the task is done if `verify.sh` hasn't gone green in this run.
- If asked for a `release`, refuse this pipeline and point at the release-PR section of `.github/PULL_REQUEST_TEMPLATE.md` instead.
