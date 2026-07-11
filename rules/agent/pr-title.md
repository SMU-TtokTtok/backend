# Rule: PR Title Convention

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Git Conventions. Applied when opening PRs (manually or via the `ship` skill). Not git-hook enforced — PR titles live on the remote, so this is a convention plus automation, not a local hook.

- **Format:** `<작업 내용> (<작업 브랜치> -> <머지받을 브랜치>)`
- **Language:** Korean (the `작업 내용` part)
- **Issue number:** omit it from the title. The branch name already carries `#<issue>` inside the parentheses, and the issue link belongs in the PR body (`관련 이슈`).
- **Example:**
  ```text
  파일 업로드 로직 개선 (refactor/#326 -> develop)
  ```
