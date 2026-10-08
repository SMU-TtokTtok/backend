# Reference: Clean Code Principles

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Coding Conventions.

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
- **New code must throw `CustomException` subclasses, not `IllegalArgumentException`.** Add an `ErrorMessage`
  entry (message + HTTP status) and a matching exception class, so the status is declared at the throw site
  instead of relying on the catch-all `IllegalArgumentException` handler in `GlobalExceptionHandler`.
  That handler still exists only for pre-existing call sites; do not add new dependencies on it.
