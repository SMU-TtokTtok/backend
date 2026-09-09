# Reference: SOLID Principles

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Coding Conventions.

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
