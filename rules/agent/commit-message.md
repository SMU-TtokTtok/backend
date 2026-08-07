# Rule: Commit Message Convention

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Git Conventions. Enforced by [`maintenance/hooks/commit-msg`](../../maintenance/hooks/commit-msg).

- **Format:** `[#issue-number] - message` (header)
- **Language:** Korean
- **Body:** one `- item` line per piece of work
- **Example:**
  ```text
  [#123] - 새로운 알림 기능 추가

  - FCM 연동 로직 구현
  - 알림 설정 API 엔드포인트 추가
  - 관련 단위 테스트 작성
  ```
- Auto-generated messages (`Merge`, `Revert`, `fixup!`, `squash!`) are exempt from the format check.

## No AI signatures

Commit messages must not carry AI attribution — no model names, no bot email addresses. Blocked by the same `commit-msg` hook:

- `Co-Authored-By: <model name> <noreply@...>`
- `🤖 Generated with [Claude Code]` and similar trailers
- Any model name or bot address in the header or body

**Co-Authored-By for human collaborators stays allowed** — only AI/bot signatures are blocked.

The same applies to **PR bodies**, which the `/ship` pipeline generates from
[`.github/PULL_REQUEST_TEMPLATE.md`](../../.github/PULL_REQUEST_TEMPLATE.md).

> Scope note: the hook only runs on local commits that go through `core.hooksPath`. Squash-merging from
> the GitHub web UI bypasses it. That gap is accepted; no CI check is added for it.

