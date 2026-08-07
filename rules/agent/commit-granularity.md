# Rule: Commit Granularity

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Git Conventions. Applied by the `/ship` pipeline ([`.omc/skills/ship/SKILL.md`](../../.omc/skills/ship/SKILL.md)) and by [`maintenance/HARNESS.md`](../../maintenance/HARNESS.md) step 5.
>
> Unlike the other git rules here, this one is **not** hook-enforced — see [Why no hook](#why-no-hook).

## The rule

> **One commit = one concern a reviewer can understand in a single pass, together with that concern's tests.**

Production code and the tests that directly cover it belong in the **same** commit. Do not split them apart.

## Soft caps

Split the commit — or state in the commit body why it cannot be split — once it exceeds:

- **~6 code files** (`src/**`)
- **~300 lines** of hand-written diff

Exempt from the caps:

- **Purely mechanical changes** (rename, import cleanup, reformatting) may span any number of files in one commit — provided that commit contains *nothing but* the mechanical change.
- **Generated files and measurement artifacts** do not count toward the line cap. They still count toward the file cap.

## Never mix these four axes

A single commit must stay within one axis:

| Axis | Typical paths |
|---|---|
| 1. Production code + its tests | `src/main/**`, `src/test/**` |
| 2. Test harness, infrastructure, scripts | `load-test/` (`docker-compose*.yml`, `k6/**`, `scripts/*.ps1`, `sql/**`), `maintenance/**`, `.github/**`, `.claude/**`, `.omc/**` |
| 3. Measurement artifacts and reports | `load-test/results/**` (`*.log`, `summary.json`, report `*.md`) |
| 4. Standalone docs and reports | top-level `*.md` such as `migration_plan.md`, report `*.md` |

The 40-file commit in `#346` mixed axes 1, 2, and 3 — a production concurrency fix, the k6 harness that measured it, and 31 result artifacts. That is exactly what this rule prevents.

> **`IMPLEMENTATION.md` is not one of these axes — it is gitignored** (`.gitignore:68`, next to `GEMINI.md`) and is never committed at all. Append to it as the local work log, but do not stage it and do not expect a commit from it.

## Ordering

Order commits so that **each one compiles on top of the one before it**. Preferred sequence:

```
characterization tests first → behavior-preserving refactor → new behavior → cleanup (comments, dead code)
```

`#344` is the worked example in this repo's history: `e437740 ApplicantCustomRepositoryImpl characterization 테스트 선작성` lands before `f6b06ff 서류/면접 분기를 ApplicantPhaseQuery 전략으로 제거`.

## Verifying a split branch

Commits are sliced **after** the work is finished, so while staging a slice the working tree still holds every later change — you cannot compile "just this commit's state" in place. Sweep the finished branch instead:

```bash
git rebase origin/develop --exec './gradlew.bat compileJava compileTestJava'
```

- Guarantees every intermediate commit compiles, so `git revert` and `git bisect` actually work.
- On failure the rebase stops at the offending commit. Fix it, `git commit --amend`, then `git rebase --continue`.
- **Do not use `git stash` for this.** Rebase never touches untracked or gitignored files, so `application*.yml`, the Firebase key, and `db/seed/` stay safe. In particular `git stash -a` / `--all` **would** stash those gitignored local-only files — never use it here (see [`protected-local-files.md`](protected-local-files.md)).
- Afterwards confirm the final tree is unchanged: `git diff <pre-rebase HEAD> HEAD --stat` must be empty. If it is, the full `verify.sh` run from HARNESS step 4 still holds and does not need repeating. If it is not, re-run `verify.sh`.

The full test suite runs **once**, on the final tree — not per commit. `gradlew clean build` with tests takes minutes; running it N times is not worth the bisect precision it buys.

## Why no hook

The caps above are deliberately *soft* and are not enforced by `maintenance/hooks/`. They require judgment, and legitimate exceptions exist (mechanical bulk renames, artifact-only commits). Contrast [`commit-message.md`](commit-message.md), whose format and AI-signature rules **are** hook-enforced because they are mechanical.
