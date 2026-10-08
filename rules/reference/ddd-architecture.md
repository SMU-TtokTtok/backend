# Reference: DDD & Architecture

> Indexed from [`AGENTS.md`](../../AGENTS.md) → Coding Conventions.

Follows Domain-Driven Design (DDD) and a layered architecture.

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
- Business logic lives inside domain objects; keep the domain model rich rather than anemic.
- Services compose domain objects to implement use cases.
- Controllers handle only request/response conversion.

### 3. Package structure

```
org.project.ttokttok
├── domain/                     # per-domain business logic
│   ├── club/
│   │   ├── controller/         # presentation layer
│   │   ├── service/            # application layer
│   │   ├── domain/             # domain layer (Entity, VO)
│   │   └── repository/         # infrastructure layer (JPA/QueryDSL)
│   └── user/
├── global/                     # global configuration and shared functionality
│   ├── auth/                   # JWT and Security configuration
│   ├── config/                 # various configs (Async, Swagger, QueryDSL, etc.)
│   ├── exception/               # global exception handling (GlobalExceptionHandler)
│   └── util/                   # shared utilities
└── infrastructure/             # external service integration (Email, S3, Firebase, etc.)
```
