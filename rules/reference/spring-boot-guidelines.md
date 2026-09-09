# Reference: Spring Boot Guidelines

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Coding Conventions.

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
