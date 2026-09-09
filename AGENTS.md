# AGENTS Development Guidelines

> **Maintenance work follows the 5-step loop in [`maintenance/HARNESS.md`](maintenance/HARNESS.md).** This document is the single entry point for any coding agent working on **ttokttok**: project facts, build/run instructions, and coding/git conventions.

## 📋 Table of Contents
- [Project Overview](#-project-overview)
- [Tech Stack](#-tech-stack)
- [Build & Run Guide](#-build--run-guide)
- [Working Language](#-working-language)
- [Coding Conventions](#-coding-conventions)
- [Database Migrations](#️-database-migrations)
- [Git Conventions](#-git-conventions)
- [Prohibitions](#️-prohibitions)
- [Code Review Checklist](#-code-review-checklist)

---

## 🚀 Project Overview

**ttokttok** is a backend service supporting club and organization recruiting at Sangmyung University. It provides application management, evaluation processes, notification services, and more.

## 🛠 Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.5.0
- **Build Tool:** Gradle
- **Database:** PostgreSQL (main), H2 (local/test)
- **Persistence:** Spring Data JPA, QueryDSL 5.0.0
- **Migration:** Flyway
- **Cache/Session:** Redis
- **Security:** Spring Security, JWT (io.jsonwebtoken)
- **Documentation:** Springdoc (Swagger UI 2.8.9)
- **Storage:** AWS S3 (SDK v2)
- **Messaging:** Firebase Admin SDK (FCM)
- **Misc:** Apache POI (Excel processing), Spring Mail

Architecture and package layout follow DDD + a layered structure — see [`rules/reference/ddd-architecture.md`](rules/reference/ddd-architecture.md).

---

## 🏗 Build & Run Guide

### Requirements
- JDK 17
- Docker (recommended for running Redis, PostgreSQL)

### Key commands
- **Build:** `./gradlew build`
- **Run:** `./gradlew bootRun`
- **Run tests:** `./gradlew test`
- **Clean build:** `./gradlew clean build`

### Configuration & profiles
- `local`: local development environment (H2/local DB)
- `dev`: development server environment
- `prod`: production server environment
- Config files live at `src/main/resources/application-{profile}.yml` and are excluded from Git to protect sensitive information — never commit them (see [`rules/agent/protected-local-files.md`](rules/agent/protected-local-files.md)).

### API documentation
- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (local)

### QueryDSL
- QClass generation runs during `./gradlew compileJava`.

---

## 🌐 Working Language

- Think in English, but answer in Korean.

---

## 🧭 Coding Conventions

Detailed conventions live under [`rules/reference/`](rules/reference/) — read the relevant file when you need the specifics, don't re-derive from first principles.

| Topic | Reference |
|---|---|
| SOLID principles | [`rules/reference/solid-principles.md`](rules/reference/solid-principles.md) |
| Clean Code principles (naming, function size, exception types, formatting) | [`rules/reference/clean-code.md`](rules/reference/clean-code.md) |
| DDD & package structure | [`rules/reference/ddd-architecture.md`](rules/reference/ddd-architecture.md) |
| Spring Boot guidelines (annotations, DI, response format) | [`rules/reference/spring-boot-guidelines.md`](rules/reference/spring-boot-guidelines.md) |
| Naming / DTO conversion / testing / logging rules | [`rules/reference/coding-rules.md`](rules/reference/coding-rules.md) |

---

## 🗄️ Database Migrations

Flyway scripts live in `src/main/resources/db/migration` and run at application startup.

### 1. Never ship a destructive migration with the code change

Deployment is blue-green (`deploy/deploy.sh`). Flyway runs while the **previous version is
still serving traffic against the same database**, and it keeps serving for `DRAIN_SECONDS`
(30s) after the switch. For at least 30 seconds, old code runs on the new schema.

`/health` does not touch the schema, so it passes and the deploy is reported as **successful**
while users get 500s.

Split anything that breaks backward compatibility into at least two releases:

| Release | Step | Example |
|---|---|---|
| N | **expand** — additive only | add column as nullable; `DROP NOT NULL`; add index |
| N+1 | **contract** — remove | `DROP COLUMN`; `DROP CONSTRAINT`; `RENAME` |

Between them, both versions must run correctly against the same schema.

Statements that require this split: `DROP COLUMN`, `DROP TABLE`, `RENAME`,
`SET NOT NULL`, narrowing a type, adding a `UNIQUE` constraint to existing data.

> `DROP NOT NULL` is safe in the expand step, and PostgreSQL treats NULLs as distinct,
> so an existing `UNIQUE` constraint can stay until the contract step.

### 2. Do not write `GRANT` in migrations

Migrations run as `MIGRATOR_DB_USER`, which owns every object in `public`. The runtime role
(`APP_DB_USER`) receives DML on new tables automatically through default privileges — see
`deploy/init-db/lib/ownership.sql`. Granting by hand drifts from that setup.

### 3. Assume a failed migration rolls back cleanly

PostgreSQL runs DDL inside transactions and Flyway commits the history row in the same
transaction, so a failed script leaves no partial schema and no `flyway_schema_history` entry.
Fix the cause and redeploy — do not reach for `flyway repair`.

The exception is statements that cannot run in a transaction (`CREATE INDEX CONCURRENTLY`,
`VACUUM`, `ALTER TYPE ... ADD VALUE`). Avoid them; if unavoidable, say so in the PR.

---

## 🔀 Git Conventions

Detailed, situational rules live under [`rules/`](rules/) — common agent rules in [`rules/agent/`](rules/agent/), coding-convention reference docs in [`rules/reference/`](rules/reference/), runtime-specific rules in `rules/<runtime>/` (e.g. `rules/claude/`, pointed to from that runtime's entry doc). Git rules are enforced by the hooks in `maintenance/hooks/` (install once via `bash maintenance/hooks/install.sh`).

- **Commit messages** → [`rules/agent/commit-message.md`](rules/agent/commit-message.md)
- **Commit granularity** → [`rules/agent/commit-granularity.md`](rules/agent/commit-granularity.md)
- **Branches** → [`rules/agent/branch-naming.md`](rules/agent/branch-naming.md)
- **PR titles** → [`rules/agent/pr-title.md`](rules/agent/pr-title.md)
- **Protected local-only files** → [`rules/agent/protected-local-files.md`](rules/agent/protected-local-files.md)

---

## ⚠️ Prohibitions

1. **No God Objects** - do not assign too many responsibilities to a single class.
2. **No magic numbers** - define and use constants instead.
3. **No Primitive Obsession** - avoid overusing primitive types; use Value Objects.
4. **No tight coupling** - keep loose coupling through interfaces.
5. **No business logic in Controllers.**
6. **Never delete gitignored local-only config** - do not delete/move/overwrite/`git clean` `application*.yml`, Firebase `*.json`, or `db/seed/**` (unrecoverable from git). See [`rules/agent/protected-local-files.md`](rules/agent/protected-local-files.md).

---

## 🔍 Code Review Checklist

- [ ] SOLID principles followed
- [ ] Clean Code principles applied
- [ ] DDD layered structure followed
- [ ] Naming conventions followed
- [ ] Exception handling appropriate
- [ ] Tests present
- [ ] Documentation (Swagger) complete

---

**Follow these guidelines to write maintainable, extensible, high-quality code.**
