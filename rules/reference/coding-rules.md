# Reference: Naming, DTO, Testing & Logging Rules

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Coding Conventions.

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

Choose the level by who must act and how often the line appears. Production runs at INFO.

| Level | Use for | Stack trace |
|---|---|---|
| `ERROR` | A server fault someone must act on | Yes, once per failure or batch |
| `WARN` | The service continues, but someone should look: suspected abuse, a failed external call | No |
| `INFO` | State changes and batch results worth keeping as operating history | No |
| `DEBUG` | Diagnostics, and expected outcomes such as an expired token | No |

- **No per-request INFO.** Normal request flow stays at DEBUG; a busy endpoint must not scale log volume.
- **Expected client failures are not errors.** An expired token or a 4xx is normal flow.
- **Log a failure once.** Do not log and rethrow, or log the same failure in two layers.
- **Never log secrets, at any level:** tokens, `Authorization` values, passwords, verification codes.
- **Mask personal data:** use `LogMasker.email(...)`, or log an ID.
- **Do not log client-controlled exception messages.** JWT parsing messages can carry request content; log `e.getClass().getSimpleName()`.

### 6. Work-log management
- Record work done by date in `IMPLEMENTATION.md`, updating it each time.
