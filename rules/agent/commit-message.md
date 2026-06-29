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
