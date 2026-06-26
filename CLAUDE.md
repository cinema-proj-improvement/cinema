# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
./gradlew clean build -x test

# Run tests (all)
./gradlew test

# Run a single test class
./gradlew test --tests "com.elice.cinema.domain.reservation.service.ReservationServiceTest"

# Run a single test method
./gradlew test --tests "com.elice.cinema.domain.reservation.service.ReservationServiceTest.methodName"

# Run the application (local profile — H2 in-memory DB)
./gradlew bootRun

# Compile only (generates QueryDSL Q-classes to build/generated/querydsl/)
./gradlew compileJava
```

## Local Development Requirements

- **Java 21** (Gradle toolchain is enforced)
- **Redis** on `localhost:6379` — required even for local profile (session store + seat-hold locks)
- Default profile is `local` (set in `application.yml`), which uses H2 in-memory database
- File uploads default to `/tmp/cinema/uploads`; override via `FILE_UPLOAD_BASE_PATH` env var
- Toss Payments uses hardcoded test keys in `application-local.yml` — no env vars needed locally

## Architecture Overview

**Spring Boot 3.5 / Java 21 MVC application** with server-side rendering via Thymeleaf.

### Package Layout

```
com.elice.cinema
├── domain/          # Feature domains (each has controller/service/repository/entity/dto/mapper)
│   ├── movie/       # Movie catalog with status lifecycle (UPCOMING → SHOWING → ENDED)
│   ├── screening/   # Screening schedules (SCHEDULED → SHOWING → ENDED / CANCELED)
│   ├── screen/      # Physical screens and seats
│   ├── reservation/ # Reservation + seat selection flow (HOLD → CONFIRMED / CANCELED / EXPIRED)
│   ├── payment/     # Toss Payments integration
│   ├── refund/      # Refund processing
│   ├── policy/      # EnvironmentPolicy (prices, limits) + RefundPolicy rules
│   ├── member/      # User accounts (ROLE_USER / ROLE_ADMIN)
│   ├── review/      # Movie reviews
│   ├── mypage/      # Member self-service
│   ├── auth/        # Login / signup controllers (form auth)
│   ├── movieImage/  # MovieImage entity + storage event listener
│   ├── common/      # Shared enums (e.g. ScreeningType)
│   └── admin/       # Admin UI entry point
└── global/
    ├── config/      # Spring beans: Security, Redis, QueryDSL, RestTemplate, file storage
    ├── security/    # CustomUserDetails, CustomAuthSuccessHandler
    ├── error/       # ErrorCode enum, GlobalExceptionHandler, BusinessException
    ├── common/      # BaseEntity (audit), FileService interface + local/S3 impls
    ├── batch/       # Schedulers + batch services (scheduler → batch service separation)
    └── home/        # Home page controller/service
```

### Key Architectural Patterns

**Domain model**: Entities are never directly mutated from outside; they expose intent-named methods (e.g., `reservation.confirm()`, `movie.createUpcoming(...)`). Constructors are `protected`; use static factory methods or builders.

**Error handling**: All business failures throw `BusinessException(ErrorCode)`. `GlobalExceptionHandler` (scoped to `@Controller`) renders `error/custom-error.html`. Admin API controllers (`/admin/api/**`) return JSON and have CSRF disabled.

**MapStruct**: All entity↔DTO conversions go through `@Mapper(componentModel = "spring")` interfaces. Because Lombok and MapStruct share annotation processing, the binding order is enforced via `lombok-mapstruct-binding` in `build.gradle`. Generated mapper beans are Spring components — inject with `@RequiredArgsConstructor`.

**QueryDSL**: Complex queries use a `RepositoryCustom` interface + `RepositoryImpl` class pattern (e.g., `ScreeningRepositoryCustom` / `ScreeningRepositoryImpl`). Q-classes are generated to `build/generated/querydsl/` and are on the compile classpath automatically.

**Seat hold via Redis**: When a user selects seats, `ReservationLockRepository` uses `SETNX` (via `setIfAbsent`) to atomically claim a hold key `hold:screening:{id}:seat:{id}` for 5 minutes (configurable in `seat-hold.minutes`). Expired holds are cleaned by `ExpireHoldScheduler` (runs every 60 s with `fixedDelay`).

**Session**: Spring Session backed by Redis — all HTTP sessions are stored in Redis, not in-memory.

**File storage**: `FileService` is a `local` or `s3` implementation selected by `file.storage.type`. The `local` profile uses `LocalFileService`; switching to S3 requires no code changes. Movie image files are written via `MovieImagesStorageEventListener`, which fires `@TransactionalEventListener(AFTER_COMMIT)` — file I/O only happens after the DB transaction commits.

**Facade pattern**: `SeatSelectionFacade` is a read-only service that aggregates data from `ScreeningService`, `SeatService`, `ReservedSeatService`, and `EnvironmentPolicyService` into a single `SeatSelectionResponse`. Use this pattern when a page needs data from multiple domains without write side effects.

**Scheduled batch jobs** (in `global/batch/`): each scheduler delegates to a sibling batch service class (e.g., `MovieStatusScheduler` → `MovieStatusBatchService`). The scheduler holds only the `@Scheduled` trigger; business logic lives in the service.
- `MovieStatusScheduler` — promotes movies UPCOMING → SHOWING → ENDED
- `ScreeningStatusScheduler` — transitions screening statuses
- `ExpireHoldScheduler` — expires stale HOLD reservations (runs every 60 s)
- `MovieReservationRateScheduler` — refreshes advance reservation rates

### Security

- Form-login with session-based auth (`/login` POST)
- `ROLE_ADMIN` required for all `/admin/**` routes
- Static assets (`/static/**`, `/css/**`, `/js/**`, `/images/**`, `/uploads/**`) bypass security
- CSRF enabled globally; disabled only for `/h2-console/**` and `/admin/api/**`

### Profiles

| Profile | DB | Notes |
|---------|-----|-------|
| `local` | H2 in-memory (MySQL mode) | `ddl-auto: create-drop`, H2 console at `/h2-console` |
| `test`  | H2 in-memory | Same as local; used by test suite |
| `dev`   | MySQL via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) | `ddl-auto: none`, deployed via GitLab CI |

### Default Seed Accounts

`DataInitialize` (runs on startup) creates these accounts if they don't exist:

| Email | Password | Role |
|-------|----------|------|
| `admin@test.com` | `1234` | ADMIN |
| `user@test.com` | `1234` | USER |

`EnvironmentPolicy` is also seeded with default price and reservation limits. Additional fixture data is in `src/main/resources/data.sql` (loaded only when `spring.sql.init.mode=always`, i.e. local/test profiles).

### Testing Conventions

Tests use `@ExtendWith(MockitoExtension.class)` with `@InjectMocks` / `@Mock` (pure unit tests, no Spring context). The test profile (`application-test.yml`) uses H2 + local Redis config identical to `local`.
