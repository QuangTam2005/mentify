# Mentify Backend

Spring Boot modular monolith backend for Mentify.

## Requirements

- Java 21
- Maven 3.9+ or the included Maven Wrapper

## Local commands

Run from `be/`:

```bash
./mvnw test
./mvnw package
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

The application runs on `http://localhost:8080`. The minimal health check is available at `GET /actuator/health` and returns `UP` when the application and database are available. Only the health actuator endpoint is exposed.

Audit timestamps use Java `Instant` and PostgreSQL `TIMESTAMPTZ`; persistence is normalized to UTC. Convert timestamps to `Asia/Ho_Chi_Minh` only for display or Vietnam-local business rules, using an explicit ISO 8601 offset.

## PostgreSQL local development

Start PostgreSQL from `be/`:

```bash
docker compose up -d postgres
```

The default local connection uses:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=mentify
DB_USERNAME=mentify
DB_PASSWORD=mentify_local
```

These values are development defaults only. Copy `.env.example` to `.env` and change the values when needed. `.env` is ignored and must not be committed.

Flyway runs automatically on application startup. The first migration creates `users`. Hibernate uses `ddl-auto=validate`, so it validates the schema but never creates or updates it.

Integration tests use Testcontainers with PostgreSQL. Start Docker Desktop before running `./mvnw test`; when Docker is unavailable, the PostgreSQL integration test is skipped and must be rerun with Docker to verify the database path.

When running in CI, set `CI=true` (or use the provider's equivalent CI flag).
The test suite then fails if Docker is unavailable, preventing a green build
with all PostgreSQL integration tests skipped. Shared integration fixtures and
the fixed-clock test configuration live under `src/test/java/vn/mentify/be/support`.

To add a migration, create the next sequential file under `src/main/resources/db/migration`, for example `V2__add_user_consents.sql`. Never edit a migration that has already been applied; add a new version instead.

## Package structure

```text
vn.mentify.be
├── infra       # Framework configuration and technical integrations
├── modules     # Business features, one package per module
└── shared      # Small components genuinely shared by multiple modules
```

All modules run in the same Spring Boot process. A business module owns its use cases, entities, and persistence details. A module must not access another module's internal repository or entity directly; cross-module collaboration must go through an explicit public application boundary owned by the providing module.

`shared` must remain technical and minimal. It is not a place to put business logic from every module.

The shared API success/error contracts and pagination metadata are documented in `docs/fe-guide/backend-foundation.md`. Business modules should use the shared response types and must not define their own response shapes.

## Security baseline

`GET /actuator/health` is public. Business routes under `/api/v1/**` require an authenticated request with a stateless JWT Bearer token (`Authorization: Bearer <token>`), except public authentication endpoints under `/api/v1/auth/**` (such as login, password forgot/reset, and OAuth2 code exchange). `/api/v1/admin/**` additionally requires the `ADMIN` role. Google OAuth is enabled only when `GOOGLE_OAUTH_ENABLED=true` and its credentials are configured; dev-login is available only on local/dev/test profiles.

The application uses stateless JWT authentication. State-changing requests do not require CSRF cookies or `X-XSRF-TOKEN` headers. Configure comma-separated trusted frontend origins with `CORS_ALLOWED_ORIGINS`; leaving it empty does not open CORS broadly.
