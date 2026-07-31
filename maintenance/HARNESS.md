# Maintenance Harness v1

This document defines a **repeatable 5-step loop** for maintaining `ttokttok`.
Whenever an agent (Claude Code / Gemini CLI / any runtime) starts a maintenance task, it **reads this document first** and follows the steps below.

- Coding rules & conventions live in [`AGENTS.md`](../AGENTS.md) (not duplicated here).
- Project facts & build instructions live in [`GEMINI.md`](../GEMINI.md).
- Task intake form: [`task.md`](./task.md). Verification gate: [`verify.sh`](./verify.sh).

> Applies to all task types: **bug fix / feature add & improve / refactor & performance / dependency & config**. All four run the same loop.

## Setup (once per clone)

```bash
bash maintenance/hooks/install.sh
```

Installs git safety guards via `core.hooksPath`: no direct commit/push on `main`/`develop`, no force-push (any branch), `[#issue] - message` commit format, branch-name convention, and a secret-file commit guard. These then apply automatically to **every** git operation (agent or human) — see [`hooks/`](./hooks/).

---

## Loop Overview

```
1. Intake  →  2. Orient  →  3. Change  →  4. Verify  →  5. Record
                  ↑___________________________|
                   (on Verify failure, return to step 2-3)
```

Fill in each step's **DoD (Definition of Done)** checkbox before moving on.

---

## 1. Intake — Define the task

- Copy [`task.md`](./task.md) and define **one** task (or fill it into the GitHub issue body).
- Specify:
  - **Task type** tag: bug / feature / refactor & perf / dependency & config (multiple allowed)
  - **Affected domain**: which `src/main/java/org/project/ttokttok/domain/{name}`
  - **Acceptance criteria**: in a verifiable form ("request X returns Y", "test Z green")
  - **Issue number** (e.g. `#312`) — reused in commit/PR

**DoD**: [ ] task.md is filled in; task type, scope, and acceptance criteria are clear.

## 2. Orient — Understand the code (no edits)

- Read the **4 layers** of the affected domain and narrow the change surface:
  - `controller/` (presentation) → `service/` (application) → `domain/` (Entity·VO) → `repository/` (JPA/QueryDSL)
- Check rules in [`AGENTS.md`](../AGENTS.md) and stack/structure in [`GEMINI.md`](../GEMINI.md).
- Look for existing functions, utils, and patterns first (`global/`, `infrastructure/`). Prefer reuse over new code.

> **Do not edit code in this step.** Understand and scope only.

**DoD**: [ ] You can describe the files/layers to change and the impact scope in one line.

## 3. Change — Implement

Follow [`AGENTS.md`](../AGENTS.md) conventions. Key points:
- Constructor injection (`@RequiredArgsConstructor`); business logic **inside the Entity**; functions under 20 lines.
- Entity ↔ DTO conversion via static factories (`from()` / `to()`).
- Unify responses with `ApiResponse`; use custom exceptions + `GlobalExceptionHandler`.
- **On DB schema change**: add a Flyway `Vn__name.sql` under `src/main/resources/db/migration`.
- See the per-type mini checklist in [`task.md`](./task.md) for details.

> ⚠️ **Never commit secrets**: `application*.yml`, Firebase `*.json`, etc. must not be committed.

**DoD**: [ ] Change complete; code-related items in the per-type checklist are satisfied.

## 4. Verify — Verification gate

```bash
bash maintenance/verify.sh
```

- On a pure PowerShell environment: `./gradlew.bat clean build`
- This gate runs `./gradlew clean build` **with tests included**. (CI is a deployment build and skips tests with `-x test`, so enforcing tests is this step's responsibility.)
- **A task is not done until this is green.** On failure, return to step 2-3.

**DoD**: [ ] `verify.sh` is green (build + tests pass).

## 5. Record — Log the work

- Append the work to [`../IMPLEMENTATION.md`](../IMPLEMENTATION.md) as a **dated section** (keep the existing format: key work / changed files / verification notes).
- Commit message: `[#issue] - message` (Korean header), with each item as a `- item` line in the body. (See the commit convention in [`GEMINI.md`](../GEMINI.md).) No AI signatures — see [`rules/agent/commit-message.md`](../rules/agent/commit-message.md).
- **Split the work into one commit per concern** rather than a single large commit — see [`rules/agent/commit-granularity.md`](../rules/agent/commit-granularity.md).
- PR: fill in the [`.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md) checklist.

> ⚠️ Before committing, re-run `git status` to confirm no secrets are staged.

**DoD**: [ ] IMPLEMENTATION.md recorded; commit/PR conventions followed.

---

## Quick Reference

| What you need | Where |
|---------------|-------|
| Coding rules & conventions | [`AGENTS.md`](../AGENTS.md) |
| Stack, build, run | [`GEMINI.md`](../GEMINI.md) |
| Task intake form | [`task.md`](./task.md) |
| Verification gate | `bash maintenance/verify.sh` |
| Work log | [`../IMPLEMENTATION.md`](../IMPLEMENTATION.md) |
