# Rule: Branch Naming & Protection

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Git Conventions. Enforced by [`maintenance/hooks/pre-push`](../../maintenance/hooks/pre-push).

- **Naming:** `<type>/#<issue-number>` (e.g. `feat/#312`, `fix/#205`)
- **Types:** `feat | feature | fix | hotfix | refactor | chore | test | arch | release`
- **Protected branches:** `main`, `develop` — no direct commit or push; merge via PR only.
- **No force-push** on any branch (non-fast-forward pushes are rejected).
