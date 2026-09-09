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

### 6. Work-log management
- Record work done by date in `IMPLEMENTATION.md`, updating it each time.
