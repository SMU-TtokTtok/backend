# AGENTS Development Guidelines

> **Maintenance work follows the 5-step loop in [`maintenance/HARNESS.md`](maintenance/HARNESS.md).** This document covers the "coding rules" part of that loop.

This document defines the coding principles and guidelines that AI agents must follow during development.

## 📋 Table of Contents
- [SOLID Principles](#solid-principles)
- [Clean Code Principles](#clean-code-principles)
- [Domain-Driven Design (DDD)](#domain-driven-design-ddd)
- [Spring Boot Guidelines](#spring-boot-guidelines)
- [Coding Rules](#coding-rules)
- [Git Conventions](#git-conventions)

---

## 🎯 SOLID Principles

### S - Single Responsibility Principle
- **A class should have only one responsibility.**
- Each class and method should have a clear, single purpose.
- Controllers handle only HTTP request/response, Services only business logic, Repositories only data access.

```java
// ❌ Bad - a class with multiple responsibilities
public class UserController {
    public void saveUser() { /* save logic */ }
    public void sendEmail() { /* email logic */ }
    public void validateUser() { /* validation logic */ }
}

// ✅ Good - separated responsibilities
public class UserController { /* HTTP handling only */ }
public class UserService { /* business logic only */ }
public class EmailService { /* email sending only */ }
```

### O - Open/Closed Principle
- **Open for extension, closed for modification.**
- Use interfaces and abstract classes to extend functionality.
- Add new features without modifying existing code.

### L - Liskov Substitution Principle
- **Subtypes must be substitutable for their base types.**
- Interface implementations must honor the same contract.

### I - Interface Segregation Principle
- **Clients must not depend on interfaces they do not use.**
- Prefer small, specific interfaces.

### D - Dependency Inversion Principle
- **High-level modules must not depend on low-level modules.**
- Use dependency injection via the Spring DI container.

---

## 🧹 Clean Code Principles

### 1. Use meaningful names
```java
// ❌ Bad
public List<Club> getData() { return clubs; }

// ✅ Good
public List<Club> getActiveClubs() { return activeClubs; }
```

### 2. Functions should be small and single-purpose
- A function should do one thing.
- Recommended function length: under 20 lines.
- Recommended arguments: 3 or fewer.

### 3. Explain with code, not comments
```java
// ❌ Bad
// check if the user is active
if (user.getStatus() == 1) { }

// ✅ Good
if (user.isActive()) { }
```

### 4. Consistent formatting
- 4-space indentation.
- K&R brace style.
- Max 120 characters per line.

### 5. Exception handling
- Prefer unchecked over checked exceptions.
- Define specific exception types.
- Handle exceptions at the top level.

---

## 🏗️ Domain-Driven Design (DDD)

### 1. Layered structure
```
Controller (Presentation Layer)
    ↓
Service (Application Layer)
    ↓
Domain (Domain Layer)
    ↓
Repository (Infrastructure Layer)
```

### 2. Domain-model-centric design
- **Entity**: a domain object with a unique identifier.
- **Value Object**: an immutable object distinguished only by its value.
- **Aggregate**: the unit of data change.
- **Repository**: an abstraction over domain object storage.

### 3. Package structure
```
src/main/java/org/project/ttokttok/
├── domain/
│   ├── club/
│   │   ├── controller/     # presentation layer
│   │   ├── service/        # application layer
│   │   ├── domain/         # domain layer
│   │   └── repository/     # infrastructure layer
│   └── user/
└── global/                 # shared functionality
```

### 4. Domain rules
- Business logic lives inside domain objects.
- Services compose domain objects to implement use cases.
- Controllers handle only request/response conversion.

---

## 🌱 Spring Boot Guidelines

### 1. Annotation usage
```java
@RestController
@RequiredArgsConstructor  // constructor injection
@Slf4j                    // logging
@Tag(name = "API name")   // Swagger documentation
public class ClubController {
    private final ClubService clubService; // use the final keyword
}
```

### 2. Dependency injection
- Use constructor injection (Lombok `@RequiredArgsConstructor`).
- Avoid field injection and setter injection.

### 3. Exception handling
- Global exception handling via `@ControllerAdvice`.
- Define custom exception classes.
- Return appropriate HTTP status codes.

### 4. Unified response format
```java
@GetMapping
public ResponseEntity<ApiResponse<ClubListResponse>> getClubs() {
    // use a consistent response format
    return ResponseEntity.ok(ApiResponse.success(data));
}
```

---

## 📝 Coding Rules

### 1. Naming conventions
- **Class**: PascalCase (e.g., ClubService)
- **Method/variable**: camelCase (e.g., getActiveClubs)
- **Constant**: UPPER_SNAKE_CASE (e.g., MAX_MEMBER_COUNT)
- **Package**: lowercase (e.g., domain.club.service)

### 2. Method-writing rules
```java
// ✅ Good - clear method name with a single responsibility
public ClubDetailResponse getClubIntroduction(String userEmail, String clubId) {
    validateUser(userEmail);
    Club club = findClubById(clubId);
    return ClubDetailResponse.from(club);
}
```

### 3. DTO conversion rules
- Use static factory methods for Entity ↔ DTO conversion.
- Use method names `from()` and `to()`.

### 4. Writing tests
- Unit tests are mandatory.
- Use the Given-When-Then pattern.
- Korean test method names are allowed.

### 5. Logging
```java
@Slf4j
public class ClubService {
    public void processClub(String clubId) {
        log.info("Club processing started: clubId={}", clubId);
        // business logic
        log.info("Club processing finished: clubId={}", clubId);
    }
}
```

### 6. Work-log management
- Record work done by date in `IMPLEMENTATION.md`, updating it each time.

---

## 🔀 Git Conventions

This is the **single source of truth** for commit/branch rules. They are enforced automatically by the git hooks in `maintenance/hooks/` (install once via `bash maintenance/hooks/install.sh`).

### Commit messages
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

### Branches
- **Naming:** `<type>/#<issue-number>` (e.g. `feat/#312`, `fix/#205`)
- **Types:** `feat | feature | fix | hotfix | refactor | chore | test | arch | release`
- **Protected branches:** `main`, `develop` — no direct commit or push; merge via PR only.
- **No force-push** on any branch (non-fast-forward pushes are rejected).

---

## ⚠️ Prohibitions

1. **No God Objects** - do not assign too many responsibilities to a single class.
2. **No magic numbers** - define and use constants instead.
3. **No Primitive Obsession** - avoid overusing primitive types; use Value Objects.
4. **No tight coupling** - keep loose coupling through interfaces.
5. **No business logic in Controllers.**

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
