# Document & Task Management Platform

A REST API backend for managing projects, tasks, and documents, with JWT-based authentication and MinIO file storage. Built as a job interview assessment.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Persistence | Spring Data JPA + Hibernate -> PostgreSQL 16 |
| Migrations | Flyway (V1–V6) |
| File Storage | MinIO (S3-compatible) |
| Build | Maven |
| Infrastructure | Docker + Docker Compose |

---

## Prerequisites

- **Java 21** (`java -version` to verify)
- **Maven** (or use the included `./mvnw` wrapper)
- **Docker** and **Docker Compose** (for PostgreSQL and MinIO)

---

## Getting Started

### 1. Clone and configure environment variables

Copy the example and fill in your values:

```bash
cp .env.example .env
```

The `.env` file must define all variables listed in the table below. The application **will not start** without them.

### 2. Start everything

```bash
docker compose up --build
```

This builds the Spring Boot image and starts all three services — PostgreSQL (port 5432), MinIO (port 9000, console on 9001), and the app (port 8080). The app waits for PostgreSQL to pass its healthcheck before starting. Flyway migrations run automatically on first boot.

The API is available at `http://localhost:8080`.

### Running without Docker (local development)

Start only the infrastructure:

```bash
docker compose up -d postgres minio
```

Then run the app with Maven:

```bash
./mvnw spring-boot:run
```

---

## Environment Variables

All variables are **required**. The application fails to start if any are missing.

| Variable | Description | Example |
|---|---|---|
| `DB_USER` | PostgreSQL username | `dmsuser` |
| `DB_PASSWORD` | PostgreSQL password | `changeme` |
| `JWT_SECRET` | HS256 signing key — must be at least 32 bytes | `af3547ee...` (64-char hex) |
| `MINIO_ACCESS_KEY` | MinIO root user / access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO root password / secret key | `minioadmin` |

> The MinIO bucket (`dms-documents`) is created automatically at startup if it does not exist.

---

## API Reference

All protected endpoints require the header:
```
Authorization: Bearer <token>
```

Tokens are obtained from `/auth/login` or `/auth/register`. Token TTL is 24 hours.

### Authentication

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/auth/register` | Public | 201 | Register a new user |
| POST | `/auth/login` | Public | 200 | Login and receive a JWT |

### Users

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| GET | `/users/me` | Any user | 200 | Get own profile |
| PUT | `/users/me` | Any user | 200 | Update own first/last name |
| GET | `/users` | ADMIN | 200 | List all users |
| PUT | `/users/{id}/role?role={ADMIN\|USER}` | ADMIN | 200 | Change a user's role |
| PUT | `/users/{id}/deactivate` | ADMIN | 200 | Deactivate a user account |

> The last active admin cannot be demoted or deactivated.

### Projects

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/projects` | Any user | 201 | Create a project |
| GET | `/projects` | Any user | 200 | List projects you own or are a member of |
| GET | `/projects/{id}` | Member or owner | 200 | Get a single project |
| PUT | `/projects/{id}` | Owner | 200 | Update project name/description |
| DELETE | `/projects/{id}` | Owner | 204 | Soft-delete a project |
| POST | `/projects/{id}/members/{userId}` | Owner | 200 | Add a member to the project |
| DELETE | `/projects/{id}/members/{userId}` | Owner | 200 | Remove a member from the project |

### Tasks

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/projects/{projectId}/tasks` | Member or owner | 201 | Create a task in a project |
| GET | `/projects/{projectId}/tasks` | Member or owner | 200 | List tasks (filter by `?status=` and/or `?priority=`) |
| PUT | `/projects/{projectId}/tasks/{taskId}` | Member or owner | 200 | Update a task |
| DELETE | `/projects/{projectId}/tasks/{taskId}` | Member or owner | 204 | Soft-delete a task |

Valid `status` values: `TODO`, `IN_PROGRESS`, `DONE`  
Valid `priority` values: `LOW`, `MEDIUM`, `HIGH`

### Documents

| Method | Path | Auth | Status | Description |
|---|---|---|---|---|
| POST | `/projects/{projectId}/documents` | Member or owner | 201 | Upload a file (`multipart/form-data`, field name: `file`) |
| GET | `/projects/{projectId}/documents` | Member or owner | 200 | List documents in a project |
| GET | `/projects/{projectId}/documents/{documentId}/download` | Member or owner | 200 | Download a file |
| DELETE | `/projects/{projectId}/documents/{documentId}` | Member or owner | 204 | Hard-delete a document (also removes from MinIO) |

Upload constraints: max file size 50 MB; allowed types: PDF, JPEG, PNG, GIF, WebP, plain text, CSV, DOCX, XLSX, PPTX, DOC, XLS, PPT.

---

## Architecture

The project uses **package-by-layer** under `com.example.dms`:

```
controller/     HTTP layer — parse request, call one service method, return ResponseEntity
service/        Business logic — all rules live here, @Transactional boundaries
repository/     Spring Data JPA interfaces — no SQL in services
entity/         JPA entities — all suffixed with Entity to avoid naming conflicts
dto/            Request and response DTOs per domain (auth/, user/, project/, task/, document/)
security/       JwtService, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl
config/         MinioConfig (bean + ApplicationRunner for bucket creation)
aspect/         AuditAspect (@Around advice for audit logging)
exception/      Custom exceptions + GlobalExceptionHandler
annotation/     @Audited annotation
```

**Request flow:**

```
Client → JwtAuthFilter → Controller → Service → Repository → PostgreSQL
                                              ↘ MinioClient → MinIO (documents only)
                              AuditAspect ↗ (fires around @Audited service methods)
```

**Key design decisions:**

- Files are stored in MinIO only — never in the database. The DB stores metadata (name, type, size, MinIO object key).
- Projects and tasks use **soft delete** (`deleted = true`). Documents are **hard deleted** — this immediately frees MinIO storage.
- JWT tokens are verified on every request via `JwtAuthFilter`. Deactivating a user account is instant revocation — all their tokens stop working immediately.
- Audit logging uses `Propagation.REQUIRES_NEW` so audit entries are committed independently of the caller's transaction — they survive rollbacks.

---

## Database Schema

Flyway manages all schema changes. Migrations are in `src/main/resources/db/migration/`.

| Version | Description |
|---|---|
| V1 | `users` table |
| V2 | `projects` + `project_members` tables |
| V3 | `tasks` table |
| V4 | `documents` table |
| V5 | `audit_logs` table |
| V6 | Unique constraint on `(project_id, name)` in `documents` |

---

## Postman Collection

A full collection is provided in the `postman/` directory. Import it into Postman or any compatible tool.

Set the `token` collection variable after logging in:

```
POST /auth/login → copy the token value → set as {{token}}
```
