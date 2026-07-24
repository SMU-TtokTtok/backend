# Maintenance Task Intake

> Copy this file to define one task (or paste it into a GitHub issue body).
> For the procedure, see [`HARNESS.md`](./HARNESS.md).

---

## Overview

- **Title**:
- **Issue number**: #
- **Task type** (one+): [ ] Bug fix  [ ] Feature add & improve  [ ] Refactor & perf  [ ] Dependency & config

## Scope

- **Domain**: `domain/____`
- **Layers**: [ ] controller  [ ] service  [ ] domain  [ ] repository  [ ] global  [ ] infrastructure
- **DB change**: [ ] none  [ ] yes → needs a Flyway `Vn__name.sql`

## Acceptance Criteria

> In a verifiable form. e.g. "GET /clubs returns popularity-sorted response", "ClubServiceTest green".

- [ ]
- [ ]

---

## Per-Type Mini Checklist

### 🐛 Bug fix
- [ ] Confirm the reproduction path (which input/situation triggers it)
- [ ] **Add a regression test first** (reproduce the failure → red)
- [ ] After the fix, that test is green
- [ ] No impact on adjacent cases

### ✨ Feature add & improve
- [ ] Unit tests written (Given-When-Then)
- [ ] Swagger docs written/updated (`@Tag`, `@Operation`)
- [ ] Response unified with `ApiResponse`
- [ ] Custom exceptions + `GlobalExceptionHandler`

### ♻️ Refactor & perf
- [ ] **Behavior preserved**: existing tests stay green (no behavior change)
- [ ] For perf work, measure evidence (As-Is / To-Be, see `IMPLEMENTATION.md` format)
- [ ] On public API/signature change, update all call sites

### 📦 Dependency & config
- [ ] Assess `build.gradle` change impact (transitive dependency conflicts)
- [ ] Check impact per profile (`local`/`dev`/`prod`)
- [ ] On migration/config change, review rollback feasibility
- [ ] Confirm secrets (`application*.yml`, Firebase `*.json`) are not committed

---

## Verify & Record (HARNESS steps 4-5)

- [ ] `bash maintenance/verify.sh` green
- [ ] Work appended to `IMPLEMENTATION.md`
- [ ] Commit `[#issue] - message` / PR template checklist satisfied
