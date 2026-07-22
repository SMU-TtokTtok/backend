# Rule: Protected Local-Only Config Files

> Indexed from [`AGENTS.md`](../../AGENTS.md). These files are `.gitignore`d and exist only on disk (and in the CI / GitHub Actions secret store). Git never versions them, so a deletion is **unrecoverable** via git history.

## Never delete, move, overwrite, or `git clean` these

- `src/main/resources/application.yml`, `application-local.yml`, `application-dev.yml`, `application-prod.yml`
- `src/test/resources/application.yml`, `application-*.yml`
- the Firebase service-account `*.json` key (e.g. `src/main/resources/*firebase*.json`)
- `src/main/resources/db/seed/**`

## Rules

- Treat these files as **read-only** unless the user explicitly asks to change them.
- When restoring or copying them (e.g. into a git worktree), only copy **in**, and **refuse to overwrite** an existing target.
- If one appears missing, **investigate first** — it may be absent from `src` by design and materialized at CI build time from a GitHub Actions secret — rather than assuming loss or recreating it blindly.
- Never edit `.gitignore` to make these trackable; they stay untracked by design (secrets / local fixtures).
