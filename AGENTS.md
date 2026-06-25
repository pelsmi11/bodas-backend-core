# bodas-backend-core

Spring Boot backend for a wedding photo-sharing platform with real-time SSE feed, AI moderation webhook, and an admin panel for event/photo/user management.

## Build & Run

```bash
./gradlew build                            # Compile + test + JaCoCo coverage
./gradlew test                             # Run tests only
./gradlew bootRun                          # Run locally (requires env vars + .env)
./gradlew test jacocoTestReport            # Run tests + generate coverage report
./gradlew jacocoTestCoverageVerification   # Verify coverage >= 80%
```

**Java toolchain**: Java 21 (configured in `build.gradle`). Set `JAVA_HOME` accordingly.

## Swagger UI (dev profile only)

Swagger UI and OpenAPI 3 docs are available only when `SPRING_PROFILES_ACTIVE=dev`:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

Usage:
1. Start the app with `SPRING_PROFILES_ACTIVE=dev` in `.env`
2. Open `http://localhost:8080/swagger-ui.html`
3. Click **Authorize** and paste a Cognito JWT (IdToken or AccessToken)
4. Test any admin endpoint directly from the browser

Endpoints are grouped by `@Tag` annotations: Guest Photos, Event Feed, Health, Webhooks, Admin Events, Admin Photos, Admin Users & Devices.

In `prod` profile, Swagger endpoints are not registered (not just blocked — they don't exist), keeping the API structure private.

## Local Development with Docker Redis

Start a local Redis container (replaces the SSM tunnel to ElastiCache for dev):

```bash
docker compose up -d redis   # Start local Redis on 127.0.0.1:6379
./gradlew bootRun             # Run app with SPRING_PROFILES_ACTIVE=dev in .env
docker compose down           # Stop Redis (volume persists)
docker compose down -v        # Stop Redis AND delete volume data
```

**Note**: Use `127.0.0.1` (not `localhost`) in `.env` to force IPv4 and avoid DNS/IPv6 resolution issues on Windows.

Verify Redis is up: `GET /api/v1/health/feed` should return `{"redis":"UP"}`.

## Spring Profiles (Redis switching)

Redis host is determined by the active Spring profile (`SPRING_PROFILES_ACTIVE` env var):

| Profile | Redis source | Config file |
|---------|-------------|-------------|
| `dev` (default) | `127.0.0.1:6379` — works with both Docker Redis and SSM tunnel | `application-dev.yml` |
| `prod` | ElastiCache via SSM tunnel (`REDIS_HOST` env var required) | `application-prod.yml` |

The `dev` profile always connects to `127.0.0.1:6379`. What serves that port is transparent — you choose:

**Option A: Local Docker Redis (no AWS needed)**
```bash
docker compose up -d redis
./gradlew bootRun
```

**Option B: SSM tunnel to ElastiCache (no Docker needed)**
```bash
docker compose down    # free port 6379 if Docker Redis is running
aws ssm start-session --target <ec2-instance-id> \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "host=<elastichache-endpoint>,portNumber=6379,localPortNumber=6379"
# In another terminal:
./gradlew bootRun
```

**Option C: Full prod (inside AWS VPC)**
1. In `.env`: change `SPRING_PROFILES_ACTIVE=dev` → `SPRING_PROFILES_ACTIVE=prod`
2. In `.env`: uncomment `REDIS_HOST=bodas-pubsub-redis...`
3. Start the SSM tunnel (same as Option B)
4. `./gradlew bootRun`

**To switch back to local Redis:**

1. In `.env`: change `SPRING_PROFILES_ACTIVE=prod` → `SPRING_PROFILES_ACTIVE=dev`
2. `docker compose up -d redis`
3. `./gradlew bootRun`

The `docker-compose.yml` at project root defines a single `redis:7-alpine` service with AOF persistence and a healthcheck. No other infrastructure is dockerized — S3, Cognito, and PostgreSQL (Neon) are accessed via AWS/internet directly.

## Building Docker Image

The project includes a multi-stage `Dockerfile` optimized for AWS ECS Fargate (Well-Architected: small image, non-root user, minimal attack surface):

```bash
docker build -t bodas-backend-core .                                    # Build image
docker run -p 8080:8080 --env-file .env bodas-backend-core               # Run container
docker images bodas-backend-core                                         # Check image size (~180MB)
```

**For AWS ECR (staging/prod):**
```bash
docker tag bodas-backend-core:latest <account>.dkr.ecr.us-east-1.amazonaws.com/bodas-backend-core:latest
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/bodas-backend-core:latest
```

**Dockerfile details:**
- **Stage 1 (build)**: `eclipse-temurin:21-jdk-alpine` — compiles with JDK 21
- **Stage 2 (runtime)**: `eclipse-temurin:21-jre-alpine` — runs with JRE 21 only (~180MB)
- **Non-root user**: `spring` (Well-Architected security best practice)
- **Layered jar**: `bootJar.layered.enabled = true` in `build.gradle` — separates dependencies from application code for optimal Docker layer caching
- **Healthcheck**: `wget` to `/actuator/health` every 30s

The `.dockerignore` excludes `build/`, `.gradle/`, `.env`, `.idea/`, and other non-essential files from the build context.

**Note**: Docker image is for staging/prod/CI. For local dev iterativo, use `./gradlew bootRun` (hot reload with devtools).

## Environment Variables Required

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` (local Redis) or `prod` (ElastiCache via SSM tunnel) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection (Neon) |
| `AWS_REGION`, `AWS_PROFILE` | AWS credentials |
| `S3_BUCKET_NAME` | S3 bucket for photo uploads |
| `COGNITO_ISSUER_URI` | Cognito JWT validation |
| `REDIS_HOST`, `REDIS_PORT` | Redis host (required for `prod` profile, optional for `dev`) |
| `WEBHOOK_SECRET` | Moderation webhook validation |
| `MODERATION_SECRET_NAME` | AWS Secrets Manager for moderation |
| `APP_REDIS_LISTENER_ENABLED` | Set to `false` to disable Redis pub/sub listener (default: `true`) |

See `.env.example` for template values.

## Project Structure

```
website.pelsmi11.bodasbackendcore
├── BodasBackendCoreApplication.java
├── domain/
│   ├── dto/                        # Transport objects (records + @Data)
│   │   ├── admin/                  # Admin panel DTOs (Event, Photo, User, Device)
│   │   ├── ApiResponse.java        # Generic wrapper {success, data}
│   │   ├── ApiError.java           # Error wrapper {success, message}
│   │   ├── PhotoFeedDto.java       # SSE feed payload
│   │   ├── UploadUrlRequest/Response.java
│   │   ├── PhotoConfirmRequest.java
│   │   └── ModerationWebhookRequest.java
│   ├── exception/
│   │   └── CustomErrorException.java   # Business exception with HttpStatus
│   └── service/
│       ├── PhotoService.java           # Guest upload orchestrator
│       ├── ModerationService.java      # Webhook-driven moderation
│       ├── feed/
│       │   ├── EventFeedService.java       # Approved photo feed snapshot
│       │   ├── EventFeedStreamService.java  # SSE emitter management
│       │   └── PhotoFeedPublisher.java      # Redis pub/sub publisher (shared)
│       ├── photo/
│       │   ├── EventResolver.java          # Active event lookup by token
│       │   ├── GuestIdentityService.java   # Anonymous/auth identity resolution
│       │   ├── PhotoKeyFactory.java        # S3 key builder + ownership check
│       │   └── S3PhotoValidator.java       # MIME + size validation via S3 HEAD
│       └── admin/
│           ├── AdminEventService.java      # Event CRUD + stats + token regen
│           ├── AdminPhotoService.java      # Photo moderation + soft-delete
│           ├── AdminUserService.java       # User/device management
│           └── EventTokenGenerator.java    # Unique 8-char token generator
├── persistence/
│   ├── model/                      # JPA entities (Lombok @Getter/@Setter)
│   │   ├── Event.java              # + EventStatus enum, eventDate, description
│   │   ├── Photo.java              # + moderatedAt, moderatedBy, deletedAt
│   │   ├── User.java               # + role (UserRole), lastLoginAt
│   │   ├── UserDevice.java         # + blocked, blockedAt
│   │   ├── ModerationStatus.java   # enum: PENDING, APPROVED, REJECTED
│   │   ├── EventStatus.java        # enum: DRAFT, ACTIVE, ENDED, ARCHIVED
│   │   └── UserRole.java           # enum: GUEST, ADMIN
│   └── repository/                 # Spring Data JPA repositories
│       ├── EventRepository.java
│       ├── PhotoRepository.java
│       ├── UserRepository.java
│       └── UserDeviceRepository.java
└── web/
    ├── auth/
    │   └── JwtSubjectExtractor.java    # Shared JWT sub extraction (throws 401)
    ├── config/
    │   ├── SecurityConfig.java         # Public vs authenticated endpoint rules
    │   └── RedisConfig.java            # Pub/sub listener (conditional bean)
    ├── controller/
    │   ├── PhotoController.java            # Guest upload endpoints (public)
    │   ├── EventFeedController.java        # SSE feed + stream (public)
    │   ├── FeedHealthController.java       # Redis health check (public)
    │   ├── WebhookController.java          # Moderation webhook (public, secret)
    │   ├── AdminEventController.java       # Event management (authenticated)
    │   ├── AdminPhotoController.java       # Photo moderation (authenticated)
    │   └── AdminUserController.java        # User/device management (authenticated)
    └── exception/
        └── GlobalExceptionHandler.java    # @RestControllerAdvice
```

## API Endpoints

### Public Endpoints (6) — no JWT required

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/photos/presigned-url` | Generate S3 signed PUT URL for guest upload |
| POST | `/api/v1/photos/confirm` | Confirm upload + persist pending photo record |
| GET | `/api/v1/events/{eventToken}/feed` | Initial snapshot of approved photos |
| GET | `/api/v1/events/{eventToken}/stream` | SSE live stream of new approved photos |
| GET | `/api/v1/health/feed` | Redis + listener health check |
| POST | `/api/v1/webhooks/moderation` | Lambda moderation callback (X-Webhook-Secret validated) |

### Admin Endpoints (20) — `/api/v1/admin/**`, JWT required (no role enforcement yet)

**Events** (`AdminEventController` — `/api/v1/admin/events`):

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/admin/events` | Create event (auto-generates 8-char token) |
| GET | `/api/v1/admin/events` | List events (paginated, filters: `search`, `status`) |
| GET | `/api/v1/admin/events/{id}` | Event detail (includes `photoCount`) |
| PATCH | `/api/v1/admin/events/{id}` | Partial update (name, eventDate, description, status) |
| POST | `/api/v1/admin/events/{id}/regenerate-token` | Regenerate access token (invalidates old QR) |
| POST | `/api/v1/admin/events/{id}/activate` | Activate event |
| POST | `/api/v1/admin/events/{id}/deactivate` | Deactivate event (soft-delete) |
| GET | `/api/v1/admin/events/{id}/stats` | Photo counts by moderation status |
| GET | `/api/v1/admin/events/{id}/photos` | Photos of event (paginated, filter: `status`, `includeDeleted`) |
| DELETE | `/api/v1/admin/events/{id}` | Soft-delete event (isActive=false, status=ARCHIVED) |

**Photos** (`AdminPhotoController` — `/api/v1/admin/photos`):

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/photos/pending` | Global pending moderation queue (paginated) |
| GET | `/api/v1/admin/photos/{id}` | Photo detail |
| PATCH | `/api/v1/admin/photos/{id}/status` | Change photo status (approve/reject/remoderate) |
| POST | `/api/v1/admin/photos/moderate` | Batch moderation (APPROVE/REJECT) |
| DELETE | `/api/v1/admin/photos?ids=uuid1,uuid2` | Batch soft-delete |

**Users/Devices** (`AdminUserController` — `/api/v1/admin`):

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/users` | List users (paginated, filters: `search`, `role`) |
| GET | `/api/v1/admin/users/{id}` | User detail |
| GET | `/api/v1/admin/users/{id}/devices` | Devices of user (paginated) |
| PATCH | `/api/v1/admin/users/{id}/role` | Set user role (GUEST/ADMIN — prepared, not enforced) |
| POST | `/api/v1/admin/devices/block` | Block devices (batch) |
| POST | `/api/v1/admin/devices/unblock` | Unblock devices (batch) |

## Architecture Notes

- **Single module** Gradle project (`settings.gradle`: `rootProject.name = 'bodas-backend-core'`)
- **Package**: `website.pelsmi11.bodasbackendcore`
- **Java**: 21 (toolchain configured in `build.gradle`)
- **Spring Boot**: 3.5.14 with Spring Cloud AWS 3.2.1
- **JPA**: `ddl-auto: validate` — Hibernate validates entities against schema only; Flyway manages schema evolution (`src/main/resources/db/migration/`)
- **Auth**: OAuth2 Resource Server with Cognito JWT. `sub` claim used as user identity.
  - Public endpoints: `permitAll()` in `SecurityConfig`
  - Admin endpoints: `authenticated()` — any valid Cognito JWT accepted (no role/ownership enforcement yet)
  - `JwtSubjectExtractor` (`web/auth/`) centralizes JWT `sub` extraction with 401 fallback
- **Real-time feed**: SSE via `EventFeedStreamService` (in-memory emitters) + Redis Pub/Sub (`event-feed:*` channel) for multi-node fan-out. `PhotoFeedPublisher` is the shared publish point used by both `ModerationService` (webhook) and `AdminPhotoService` (manual approval).
- **Identity model**: Hybrid anonymous/authenticated. Guests identified by client-generated UUID (localStorage). If Cognito JWT present, `GuestIdentityService` merges guest device with Cognito user. Blocked devices get 403.
- **Soft-delete**: Events use `isActive=false` + `status=ARCHIVED`. Photos use `deletedAt` timestamp.
- **Moderation**: AI (Lambda/Rekognition) via webhook → `PENDING`/`APPROVED`/`REJECTED`. Manual admin moderation sets `moderatedBy` (JWT sub) and `moderationDetails.source = "admin-manual"`. Approved photos (auto or manual) publish to Redis feed channel.
- **Event tokens**: 8-char alphanumeric (`EventTokenGenerator`), unique with retry on collision. Used in QR codes for guest access.
- **Response format**: All endpoints return `ApiResponse<T>` (`{success: true, data: T}`) on success. Errors return `ApiError` (`{success: false, message: "..."}`) via `GlobalExceptionHandler`.

## Database Migrations (Flyway)

Schema evolution is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/`.

**How it works:**
- `ddl-auto: validate` — Hibernate only validates entities match the schema; it never modifies it
- Flyway runs migrations automatically on app startup
- `baseline-on-migrate: true` — V1 is marked as baseline (not executed) on the existing Neon DB; fresh installs execute V1 fully
- Future migrations: create `V2__description.sql`, `V3__description.sql`, etc.

**Adding a new migration:**
```bash
# Example: add a column to events
src/main/resources/db/migration/V2__add_events_location.sql
```
```sql
ALTER TABLE events ADD COLUMN location VARCHAR(255);
```
Flyway executes it automatically on the next `./gradlew bootRun`.

**Verification:**
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## Testing

- **Framework**: JUnit Platform (`useJUnitPlatform`), Mockito, AssertJ, MockMvc, spring-security-test
- **Layers**:
  - Unit tests (Mockito `@ExtendWith`): services, components, repositories — no Spring context
  - Web layer tests (`@WebMvcTest` + `MockMvc`): all 7 controllers, security filters exercised
  - Smoke test: `BodasBackendCoreApplicationTests` (context load with mocked beans)
- **Helpers**:
  - `TestDataFactory`: central builders for test entities (Event, Photo, User, UserDevice, JWT mock)
  - `TestSecurityConfig`: stub `JwtDecoder` for admin controller tests (accepts any token, returns fixed `sub`)
- **Coverage**: JaCoCo plugin enforces minimum 80% instruction coverage (currently ~95%). Models and DTOs excluded from report (Lombok-generated code).
  - Run: `./gradlew test jacocoTestReport` (report at `build/reports/jacoco/test/html/index.html`)
  - Verify: `./gradlew jacocoTestCoverageVerification`
- **Test count**: 177 tests across 26 test files

## Code Conventions

- **Language**: All code, comments, Javadoc, commit messages, and documentation MUST be in English (enforced — no Spanish in codebase)
- **Communication**: Conversational AI responses in Spanish (per Language Directives below)
- **Entities**: `@Getter @Setter @Entity` (not `@Data` — avoids Lombok equals/hashCode issues with JPA)
- **DTOs**: `record` for immutable responses, `@Data` for validated request bodies
- **Dependency injection**: Constructor injection (no `@Autowired` on fields)
- **Transactions**: `@Transactional` on service methods that write; `@Transactional(readOnly = true)` for reads
- **Validation**: `@Valid` + Jakarta constraints (`@NotBlank`, `@NotNull`, `@Size`) on request DTOs
- **Errors**: `CustomErrorException.handlerCustomError(message, HttpStatus)` → handled by `GlobalExceptionHandler`
- **SOLID compliance**: Audited. Key design decisions:
  - `PhotoFeedPublisher` extracted as shared component (DRY — used by webhook + admin moderation)
  - `JwtSubjectExtractor` extracted as shared component (DRY — used by all admin controllers)
  - `parseIds` logic lives in `AdminPhotoService` not controller (SRP)
  - `GuestIdentityService` handles all identity resolution branches (cohesive despite complexity)
  - No interface abstractions on services (pragmatic for current scale; revisit if mocking without Spring becomes needed)

## Key Infrastructure Files

| File | Purpose |
|------|---------|
| `build.gradle` | Gradle config, dependencies, JaCoCo plugin, bootJar layered |
| `Dockerfile` | Multi-stage Docker image build (JDK 21 build → JRE 21 alpine runtime) |
| `.dockerignore` | Excludes build artifacts, IDE files, .env from Docker context |
| `docker-compose.yml` | Local Redis for development |
| `.env.example` | Template for required env vars |
| `src/main/resources/application.yml` | Spring config (all values via env vars) |
| `src/main/resources/db/migration/` | Flyway SQL migrations (V1, V2, ...) |
| `src/main/resources/application-dev.yml` | Dev profile: local Redis + Swagger UI enabled |
| `src/main/resources/application-prod.yml` | Prod profile: ElastiCache Redis |
| `.gitignore` | Excludes `.env`, `build/`, `.idea/`, etc. |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5.14 (Java 21) |
| Security | Spring Security OAuth2 Resource Server (Cognito JWT) |
| Persistence | Spring Data JPA + PostgreSQL (Neon) |
| Cache/PubSub | Spring Data Redis (ElastiCache in prod, Docker in dev) |
| AWS | Spring Cloud AWS 3.2.1 (S3, Secrets Manager) |
| Build | Gradle (Kotlin DSL) |
| Test | JUnit 5 + Mockito + AssertJ + MockMvc + JaCoCo |
| Containerization | Docker (multi-stage Dockerfile) + Docker Compose (Redis only) |
| API Docs | springdoc-openapi 2.6.0 (Swagger UI, dev profile only) |

## Known Debt / Future Work

- **Role enforcement**: `UserRole` enum exists (`GUEST`, `ADMIN`) but is not enforced in `SecurityConfig`. Current admin endpoints accept any valid Cognito JWT. Future: add `JwtAuthenticationConverter` mapping `cognito:groups` or custom claim to `ROLE_ADMIN`, then `hasRole("ADMIN")` on `/api/v1/admin/**`.
- **Event ownership**: `Event.adminId` stores Cognito `sub` but is not validated against caller JWT. Future: filter events by `adminId == jwt.sub` for organizer-scoped access.
- **Database migrations**: ✅ Resolved — Flyway manages schema evolution (`db/migration/`). `ddl-auto: validate` (Hibernate validates only).
- **SSE distribution**: Emitters are in-memory per node. Redis pub/sub handles fan-out across nodes, but `dispatchNewPhoto` is only called from the node processing the webhook. Acceptable for current scale.

## Language Directives
* Artifacts: All code, variable names, inline comments, commit messages, and documentation artifacts MUST be written in English.
* Communication: All conversational responses, explanations, analysis, and interface interactions provided by the AI assistant MUST be in Spanish.
