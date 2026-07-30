# DMS Backend Architecture

This document describes the **current, as-implemented** state of the `backend/` Spring Boot
application, for consumption by a frontend team building a client against this API. It is
derived directly from the source under `backend/src/main/java/com/example/dms`, the Flyway
migrations, `application.yml`, `pom.xml`, and the Postman collection under `backend/postman/`.
Where the code and the top-level `README.md` disagree, or where behavior is ambiguous, it is
called out explicitly in section 10.

---

## 1. Tech Stack / Key Dependencies (from `pom.xml`)

| Dependency | Version | Notes |
|---|---|---|
| `spring-boot-starter-parent` | **4.0.6** | Parent BOM; Java 21 (`<java.version>21</java.version>`) |
| `spring-boot-starter-data-jpa` | (managed) | Hibernate ORM |
| `spring-boot-starter-flyway` | (managed) | Wires Flyway into Spring Boot autoconfig |
| `flyway-database-postgresql` | (managed) | Flyway PostgreSQL dialect support |
| `spring-boot-starter-security` | (managed) | Spring Security |
| `spring-boot-starter-validation` | (managed) | Jakarta Bean Validation |
| `spring-boot-starter-webmvc` | (managed) | Spring MVC (servlet stack) |
| `postgresql` (JDBC driver) | (managed) | runtime scope |
| `io.jsonwebtoken:jjwt-api` | **0.12.6** | JWT creation/parsing |
| `io.jsonwebtoken:jjwt-impl` | **0.12.6** | runtime scope |
| `io.jsonwebtoken:jjwt-jackson` | **0.12.6** | runtime scope, JSON (de)serialization for JWT claims |
| `io.minio:minio` | **8.5.9** | MinIO (S3-compatible) client |
| `org.apache.tika:tika-core` | **2.9.2** | Content-based MIME type sniffing for uploaded files |
| `org.aspectj:aspectjweaver` | (managed) | Backs `@Aspect`/`@Around` AOP for audit logging |
| `org.projectlombok:lombok` | (managed) | `@Getter/@Setter/@Builder/@RequiredArgsConstructor` etc., optional, excluded from the fat jar |
| `spring-boot-devtools` | (managed) | runtime + optional |
| `spring-boot-starter-test` | (managed) | test scope only |

Build: Maven, with a compiler-plugin configuration that wires the Lombok annotation processor
explicitly for both `default-compile` and `default-testCompile`.

No Swagger/OpenAPI/springdoc dependency is present — there is no generated API spec; this
document and the Postman collection are the closest things to one.

---

## 2. Package Structure (`com.example.dms`)

| Package | Purpose |
|---|---|
| `controller` | HTTP layer: `@RestController`s, one per domain, thin — validate via `@Valid`, delegate to a service, map to `ResponseEntity` |
| `service` | All business logic and authorization checks, `@Transactional` boundaries |
| `repository` | Spring Data JPA repositories, one per entity, derived queries + a couple of `@Query`/native derived methods |
| `entity` | JPA entities and enums, all entity classes suffixed `Entity` |
| `dto` | Request/response DTOs, sub-packaged per domain: `auth/`, `user/`, `project/`, `task/`, `document/` |
| `security` | `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `UserDetailsServiceImpl` |
| `config` | `MinioConfig` (MinIO client bean + startup bucket-creation `ApplicationRunner`) |
| `aspect` | `AuditAspect` — `@Around` advice around `@Audited`-annotated service methods |
| `exception` | Custom `RuntimeException` subclasses + `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| `annotation` | `@Audited` — marker annotation consumed by `AuditAspect` |

Root package `com.example.dms` also holds `DocumentManagementSystemApplication` (plain
`@SpringBootApplication`, no extra `@Enable*` annotations beyond what `SecurityConfig` declares).

---

## 3. JPA Entities

All entities use `@GeneratedValue(strategy = GenerationType.IDENTITY)` on a `Long id`.

### `UserEntity` (table `users`)
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `email` | `String` | `unique = true, nullable = false` |
| `password` | `String` | BCrypt hash, `nullable = false` |
| `firstName` | `String` | column `first_name`, `nullable = false` |
| `lastName` | `String` | column `last_name`, `nullable = false` |
| `role` | `Role` (enum: `ADMIN`, `USER`) | `@Enumerated(EnumType.STRING)`, `nullable = false` |
| `active` | `boolean` | default `true`; soft-disable flag — checked as `UserDetails.isEnabled()` |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp`, `updatable = false` |
| `updatedAt` | `LocalDateTime` | `@UpdateTimestamp` |

No `deleted` flag — users are never deleted, only deactivated (`active = false`).

### `ProjectEntity` (table `projects`)
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `name` | `String` | `nullable = false, length = 100` |
| `description` | `String` | `length = 500`, nullable |
| `status` | `ProjectStatus` (enum: `ACTIVE`, `ARCHIVED`) | `nullable = false, length = 20`, default `ACTIVE`. **No code path currently sets it to `ARCHIVED`** — see section 10 |
| `deleted` | `boolean` | default `false` — **soft delete** |
| `owner` | `UserEntity` | `@ManyToOne(FetchType.LAZY)`, column `owner_id`, `nullable = false`. Owner is tracked separately from `members` |
| `members` | `Set<UserEntity>` | `@ManyToMany(FetchType.LAZY)` via join table `project_members(project_id, user_id)`. Owner is **not** included in this set |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` |
| `updatedAt` | `LocalDateTime` | `@UpdateTimestamp` |

### `TaskEntity` (table `tasks`)
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `title` | `String` | `nullable = false, length = 200` |
| `description` | `String` | `length = 1000`, nullable |
| `priority` | `TaskPriority` (enum: `LOW`, `MEDIUM`, `HIGH`) | `nullable = false, length = 20`, default `MEDIUM` |
| `status` | `TaskStatus` (enum: `TODO`, `IN_PROGRESS`, `DONE`) | `nullable = false, length = 20`, default `TODO` |
| `deadline` | `LocalDate` | nullable |
| `project` | `ProjectEntity` | `@ManyToOne(FetchType.LAZY)`, column `project_id`, `nullable = false` |
| `creator` | `UserEntity` | `@ManyToOne(FetchType.LAZY)`, column `creator_id`, `nullable = false` |
| `assignee` | `UserEntity` | `@ManyToOne(FetchType.LAZY)`, column `assignee_id`, **nullable** — optional, and per the entity's own doc comment does **not** have to be a project member |
| `deleted` | `boolean` | default `false` — **soft delete** |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` |
| `updatedAt` | `LocalDateTime` | `@UpdateTimestamp` |

### `DocumentEntity` (table `documents`)
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `name` | `String` | `nullable = false, length = 255` — original filename, unique per project (`uq_document_name_per_project` from V6) |
| `type` | `String` | `length = 100`, nullable — the **Tika-detected MIME type**, not a file extension |
| `size` | `Long` | `nullable = false` — bytes |
| `minioKey` | `String` | `nullable = false, length = 512` — a random UUID string, the MinIO object key |
| `project` | `ProjectEntity` | `@ManyToOne(FetchType.LAZY)`, column `project_id`, `nullable = false` |
| `owner` | `UserEntity` | `@ManyToOne(FetchType.LAZY)`, column `owner_id`, `nullable = false` — the uploader |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` |

No `updatedAt`, no `deleted` flag — documents are **hard-deleted** (row + MinIO object both
removed immediately).

### `AuditLogEntity` (table `audit_logs`)
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | PK |
| `actor` | `String` | `nullable = false` — email of the acting user, or the literal string `"anonymous"` |
| `action` | `String` | `nullable = false, length = 100` — event name, e.g. `DOCUMENT_UPLOAD`, `LOGIN_FAILURE` |
| `entityType` | `String` | column `entity_type`, `length = 100`, nullable |
| `entityId` | `String` | column `entity_id`, nullable |
| `projectId` | `Long` | column `project_id`, nullable. **Intentionally has no FK constraint** (per entity doc comment) so audit rows survive project deletion |
| `details` | `String` | `columnDefinition = "TEXT"`, nullable — free text |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` |

No entity/repository/controller exposes audit logs for reading — they are write-only from the
application's perspective (see section 9 and 10).

### Enums
- `Role`: `ADMIN`, `USER`
- `ProjectStatus`: `ACTIVE`, `ARCHIVED`
- `TaskStatus`: `TODO`, `IN_PROGRESS`, `DONE`
- `TaskPriority`: `LOW`, `MEDIUM`, `HIGH`

---

## 4. DTOs by Domain

All request DTOs are plain `@Getter/@Setter` classes (mutable, framework-friendly for
`@RequestBody` binding); all response DTOs use `@Getter` (+ `@Builder` or `@AllArgsConstructor`).
Field names below are exact Java field names — Jackson serializes them as-is (camelCase) unless
noted.

### `auth/`
**`RegisterRequest`** (request body of `POST /auth/register`)
```
email      String   @NotBlank @Email @Size(max=255)
password   String   @NotBlank @Size(min=8, max=72)
firstName  String   @NotBlank @Size(max=100)
lastName   String   @NotBlank @Size(max=100)
```

**`LoginRequest`** (request body of `POST /auth/login`)
```
email      String   @NotBlank @Email @Size(max=255)
password   String   @NotBlank @Size(min=8, max=72)
```

**`AuthResponse`** (response body of both `register` and `login`)
```
token      String   // signed JWT
email      String
firstName  String
lastName   String
role       String   // enum name, e.g. "USER" or "ADMIN" — a plain String, not the Role enum type
```

### `user/`
**`UpdateProfileRequest`** (request body of `PUT /users/me`)
```
firstName  String   @Size(min=1, max=100)   // optional; nulls are left unchanged server-side
lastName   String   @Size(min=1, max=100)   // optional; nulls are left unchanged server-side
```
Note: fields are **not** `@NotBlank` — omitting a field in the JSON (or sending `null`) leaves
that field unchanged on the server; sending an empty string `""` would fail `@Size(min=1)`.

**`UserResponse`** (embedded in many responses, and the body of `/users/me`, `/users`, `/users/{id}/role`, `/users/{id}/deactivate`)
```
id         Long
email      String
firstName  String
lastName   String
role       Role     // serialized as the enum name string, e.g. "ADMIN"
active     boolean
```
Note the type inconsistency: `AuthResponse.role` is `String`, but `UserResponse.role` is the
`Role` enum (serializes identically to JSON, but is a different Java type — matters if the
frontend generates types from Java reflection rather than from the JSON shape).

**`UserSearchResponse`** (body of `GET /users/search`) — added for the frontend's
add-project-member flow, since `GET /users` is admin-only
```
id         Long
email      String
firstName  String
lastName   String
```
Deliberately narrower than `UserResponse` — no `role`/`active` — since this endpoint is
reachable by any authenticated user, not just admins.

### `project/`
**`CreateProjectRequest`** (request body of `POST /projects`)
```
name          String  @NotBlank @Size(max=100)
description   String  @Size(max=500)          // optional
```

**`UpdateProjectRequest`** (request body of `PUT /projects/{id}`)
```
name          String  @Size(min=1, max=100)   // optional; null = unchanged
description   String  @Size(max=500)          // optional; null = unchanged
```

**`ProjectResponse`** (response body for all project endpoints)
```
id            Long
name          String
description   String
status        ProjectStatus     // "ACTIVE" | "ARCHIVED"
owner         UserResponse
members       Set<UserResponse> // does NOT include the owner
createdAt     LocalDateTime
```
Note: no `updatedAt` in the response even though the entity has one.

### `task/`
**`CreateTaskRequest`** (request body of `POST /projects/{projectId}/tasks`)
```
title         String        @NotBlank @Size(max=200)
description   String        @Size(max=1000)     // optional
priority      TaskPriority  // optional; server defaults to MEDIUM if omitted/null
deadline      LocalDate     // optional, ISO-8601 date string, e.g. "2026-08-01"
assigneeId    Long          // optional
```

**`UpdateTaskRequest`** (request body of `PUT /projects/{projectId}/tasks/{taskId}`)
```
title         String        @Size(min=1, max=200)   // optional; null = unchanged
description   String        @Size(max=1000)         // optional; null = unchanged
priority      TaskPriority  // optional; null = unchanged
status        TaskStatus    // optional; null = unchanged
deadline      LocalDate     // optional; null = unchanged
assigneeId    Long          // optional; null = unchanged (there is no way to *clear* an assignee — see section 10)
```

**`TaskResponse`** (response body for all task endpoints)
```
id            Long
title         String
description   String
priority      TaskPriority     // "LOW" | "MEDIUM" | "HIGH"
status        TaskStatus       // "TODO" | "IN_PROGRESS" | "DONE"
deadline      LocalDate
creator       UserResponse
assignee      UserResponse     // null if unassigned
projectId     Long
createdAt     LocalDateTime
```

### `document/`
No dedicated request DTO — upload is a raw `multipart/form-data` part named `file`
(`MultipartFile`), not a JSON body.

**`DocumentResponse`** (response body for upload/list)
```
id            Long
name          String    // original filename as uploaded
type          String    // Tika-detected MIME type, e.g. "application/pdf"
size          Long      // bytes
projectId     Long
ownerId       Long      // just the ID, not a nested UserResponse — inconsistent with Project/Task responses (see section 10)
createdAt     LocalDateTime
```

**`DocumentDownload`** — internal-only `record(InputStream stream, String name, String contentType, long size)`,
never serialized to JSON; it's the vehicle the service uses to hand the file stream + metadata to
the controller, which turns it into headers + a streamed body (see section 8).

---

## 5. Auth Mechanism

### `JwtService` (`security/JwtService.java`)
- Algorithm: **HS256** (`Keys.hmacShaKeyFor(secretKey.getBytes(UTF_8))`, signed via `Jwts.builder().signWith(...)`, using JJWT 0.12.6's builder API).
- Secret key: injected from `${jwt.secret}` → environment variable `JWT_SECRET` (no default; app fails to start if unset). README states it must be at least 32 bytes.
- Claims: `subject` = user's **email**; custom claim `role` = the role name as a plain string (e.g. `"USER"`); standard `iat` (`issuedAt`) and `exp` (`expiration`) claims.
- Expiry: `${jwt.expiration}` = **86400000 ms (24 hours)**, hardcoded in `application.yml` (not overridden by an env var).
- `generateToken(email, role)` — builds and signs the token.
- `extractEmail(token)` — parses claims and returns `subject`. Throws `io.jsonwebtoken.JwtException` (or subclasses, e.g. `ExpiredJwtException`, `SignatureException`) if the token is malformed/invalid/expired.
- `isTokenValid(token, email)` — `true` iff the token's subject equals the given email **and** it is not expired. (Signature validity is implicitly checked earlier when the claims are parsed — an invalid signature throws before this method is reached.)

### `JwtAuthFilter` (`security/JwtAuthFilter.java`)
A `OncePerRequestFilter` registered via `SecurityConfig` **before** `UsernamePasswordAuthenticationFilter`. Per request:
1. Reads the `Authorization` header. If missing or not prefixed `Bearer `, the filter no-ops and calls `filterChain.doFilter(...)` — the request proceeds **unauthenticated** (later rejected by `authorizeHttpRequests` if the path isn't `/auth/**`).
2. Strips the `Bearer ` prefix, calls `jwtService.extractEmail(token)`. Any exception here (invalid/expired/malformed token) is swallowed and the request proceeds unauthenticated (not rejected by the filter itself — again, downstream authorization rules decide).
3. If an email was extracted and there's no existing `Authentication` in the `SecurityContextHolder`:
   - Loads `UserDetails` via `UserDetailsServiceImpl.loadUserByUsername(email)`.
   - If `jwtService.isTokenValid(token, email)` **and** `userDetails.isEnabled()` → builds a `UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())`, attaches `WebAuthenticationDetailsSource` details, and sets it on the `SecurityContextHolder`. This is what makes the request "authenticated" for the rest of the chain.
   - If the token is well-formed for a known user but `!userDetails.isEnabled()` (i.e., the account was deactivated after the token was issued): does **not** authenticate, and explicitly writes an audit log entry `AUTH_DENIED_DEACTIVATED` via `AuditService` (actor = the token's email, entityType `USER`, details `"JWT rejected: account disabled"`).
   - If the user in the token no longer exists (`UsernameNotFoundException`), it's caught and just logged at debug level — request proceeds unauthenticated.
4. Always calls `filterChain.doFilter(...)` at the end.

This means **deactivating a user instantly invalidates all their outstanding JWTs**, without needing a token blacklist — every request re-checks `active` in the DB.

### `UserDetailsServiceImpl` (`security/UserDetailsServiceImpl.java`)
Loads a `UserEntity` by email via `UserRepository.findByEmail`, throws `UsernameNotFoundException` if absent, and returns a Spring Security `User` with:
- username = email, password = the stored BCrypt hash
- `enabled` = `user.isActive()`
- `accountNonExpired`, `credentialsNonExpired`, `accountNonLocked` = all hardcoded `true`
- authorities = single `SimpleGrantedAuthority("ROLE_" + role.name())` — i.e. `ROLE_ADMIN` or `ROLE_USER`

### `SecurityConfig` (`security/SecurityConfig.java`)
- CSRF disabled, session policy `STATELESS`.
- `authorizeHttpRequests`: `/auth/**` → `permitAll()`; everything else → `authenticated()`.
- `@EnableMethodSecurity` is active, enabling `@PreAuthorize` on controller methods.
- Beans: `BCryptPasswordEncoder` (default strength) as `PasswordEncoder`, and an `AuthenticationManager` pulled from Spring's `AuthenticationConfiguration` (used by `AuthService.login` to actually verify credentials against `UserDetailsServiceImpl` + the password encoder).
- **No CORS configuration anywhere in the codebase** (no `CorsConfigurationSource` bean, no `@CrossOrigin`, no `WebMvcConfigurer#addCorsMappings`) — see section 10, this will block cross-origin requests from a frontend dev server by default.
- No custom `AuthenticationEntryPoint`/`AccessDeniedHandler` is configured, so unauthenticated/forbidden responses at the security-filter level use Spring Security's defaults rather than `GlobalExceptionHandler`'s JSON shape (see sections 7 and 10).

### Role model
- Two roles only: `ADMIN`, `USER`. New registrations are always `USER` (`AuthService.register` hardcodes `Role.USER`); there is no self-service way to become `ADMIN` — an existing admin must call `PUT /users/{id}/role`.
- Role checks are **method-security annotations** (`@PreAuthorize("hasRole('ADMIN')")`) only on the three admin `UserController` endpoints (`GET /users`, `PUT /users/{id}/role`, `PUT /users/{id}/deactivate`).
- All other authorization (project ownership, project membership) is **manual**, done in the service layer via `checkOwner(...)` / `checkMemberOrOwner(...)` helper methods that throw `org.springframework.security.access.AccessDeniedException` — not method-security annotations. See section 10 for a significant consequence of this distinction.
- Business-rule guard: a user with `role == ADMIN` cannot be demoted (`updateRole`) or deactivated (`deactivateUser`) if they are the **last active admin** (`userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1`) — throws `InvalidOperationException` → HTTP 409.

---

## 6. Full REST API Surface

All paths are relative to the app root (`server.port: 8080`, no `context-path` configured).
"Auth" column: **Public** = no token required; **Authenticated** = any valid, non-disabled
JWT; **ADMIN** = authenticated + `ROLE_ADMIN`; **Owner** / **Member or owner** = manual
service-layer check (not a Spring Security role).

### `AuthController` — base path `/auth` (all public)
| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| POST | `/auth/register` | `RegisterRequest` | `AuthResponse` | 201 |
| POST | `/auth/login` | `LoginRequest` | `AuthResponse` | 200 |

`register` throws `EmailAlreadyExistsException` (409) if the email is taken. `login` delegates
credential checking to `AuthenticationManager`; on failure the original Spring Security exception
propagates (`BadCredentialsException` → 401 "Invalid email or password", or `DisabledException`
→ 401 "Account is disabled" if the account was deactivated). Both success and failure are
audit-logged (`LOGIN_SUCCESS` / `LOGIN_FAILURE`) directly in `AuthService`, not via `@Audited`.

### `UserController` — base path `/users` (all require authentication)
| Method | Path | Params | Body | Response | Status | Auth |
|---|---|---|---|---|---|---|
| GET | `/users/me` | — | — | `UserResponse` | 200 | Authenticated |
| PUT | `/users/me` | — | `UpdateProfileRequest` | `UserResponse` | 200 | Authenticated |
| GET | `/users/search` | query `email` (required, partial/case-insensitive) | — | `List<UserSearchResponse>` | 200 | Authenticated (any role) |
| GET | `/users` | — | — | `List<UserResponse>` | 200 | ADMIN (`@PreAuthorize`) |
| PUT | `/users/{id}/role` | query `role` (`ADMIN`\|`USER`, required) | — | `UserResponse` | 200 | ADMIN (`@PreAuthorize`) |
| PUT | `/users/{id}/deactivate` | — | — | `UserResponse` | 200 | ADMIN (`@PreAuthorize`) |

`GET /users/search` was added specifically to unblock the frontend's add-project-member flow
(project owners aren't admins and had no way to look up a user to invite). Queries shorter
than 2 characters return an empty list rather than hitting the DB; matches are capped at 10
and restricted to `active = true` users.

Notes:
- `role` is bound via `@RequestParam Role role` — an invalid enum string produces a
  `MethodArgumentTypeMismatchException` → 400 with message `Invalid value '<value>' for
  parameter 'role'`.
- `updateRole`/`deactivateUser` throw `ResourceNotFoundException` (404) for an unknown `id`, and
  `InvalidOperationException` (409) for the last-active-admin guard.
- There is no "activate" / re-enable endpoint — deactivation appears to be one-way through the
  API (see section 10).
- No DELETE for a user account exists anywhere.

### `ProjectController` — base path `/projects` (all require authentication)
| Method | Path | Body | Response | Status | Auth nuance |
|---|---|---|---|---|---|
| POST | `/projects` | `CreateProjectRequest` | `ProjectResponse` | 201 | Any authenticated user; caller becomes owner |
| GET | `/projects` | — | `List<ProjectResponse>` | 200 | Returns only projects the caller owns or is a member of (non-deleted) |
| GET | `/projects/{id}` | — | `ProjectResponse` | 200 | Owner or member |
| PUT | `/projects/{id}` | `UpdateProjectRequest` | `ProjectResponse` | 200 | Owner only |
| POST | `/projects/{id}/members/{userId}` | — | `ProjectResponse` | 200 | Owner only |
| DELETE | `/projects/{id}/members/{userId}` | — | `ProjectResponse` | 200 | Owner only |
| DELETE | `/projects/{id}` | — | (empty) | 204 | Owner only — soft delete |

Notes:
- `addMember` rejects (409 `InvalidOperationException`) adding the owner as a member, and
  rejects adding an already-existing member.
- `removeMember` throws `ResourceNotFoundException` (404) — not a 409/400 — if the target user
  isn't currently a member.
- `getProject`/`updateProject`/member endpoints all throw `ResourceNotFoundException` (404) if
  the project doesn't exist **or is soft-deleted** — deleted projects are indistinguishable from
  never-existing ones through this API.
- "Owner only" and "Member or owner" throw `org.springframework.security.access.AccessDeniedException`
  from the service layer, mapped to HTTP 403 (see section 7 — this previously fell through to
  the generic 500 handler, fixed since this doc was first written).
- There is no endpoint to change `status` (`ACTIVE`/`ARCHIVED`) — see section 10.

### `TaskController` — base path `/projects/{projectId}/tasks` (all require authentication + member-or-owner of `projectId`)
| Method | Path | Query params | Body | Response | Status |
|---|---|---|---|---|---|
| POST | `/projects/{projectId}/tasks` | — | `CreateTaskRequest` | `TaskResponse` | 201 |
| GET | `/projects/{projectId}/tasks` | `status` (optional, `TaskStatus`), `priority` (optional, `TaskPriority`) | — | `List<TaskResponse>` | 200 |
| PUT | `/projects/{projectId}/tasks/{taskId}` | — | `UpdateTaskRequest` | `TaskResponse` | 200 |
| DELETE | `/projects/{projectId}/tasks/{taskId}` | — | — | (empty) | 204 |

Notes:
- Filtering supports any combination of `status`/`priority`/neither/both (four distinct
  repository queries dispatched in `TaskService.getTasksForProject`); no pagination parameters.
- `assigneeId` on create/update, if provided, must reference an existing user
  (`ResourceNotFoundException` "Assignee not found" otherwise) but that user does **not** have
  to be a project member or the project owner.
- There is **no `GET /projects/{projectId}/tasks/{taskId}`** single-task endpoint — a client
  needing one task's current data must use the list endpoint (see section 10).
- Soft delete only (`deleted = true`); deleted tasks are excluded from every `TaskRepository`
  query used by the service, and update/delete on an already-deleted task returns
  `ResourceNotFoundException` (404).

### `DocumentController` — base path `/projects/{projectId}/documents` (all require authentication + member-or-owner of `projectId`)
| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| POST | `/projects/{projectId}/documents` | `multipart/form-data`, field `file` | `DocumentResponse` | 201 |
| GET | `/projects/{projectId}/documents` | — | `List<DocumentResponse>` | 200 |
| GET | `/projects/{projectId}/documents/{documentId}/download` | — | binary stream (see section 8) | 200 |
| DELETE | `/projects/{projectId}/documents/{documentId}` | — | (empty) | 204 |

Notes:
- Upload validation order: empty check → size check (50MB) → Tika MIME sniff → allow-list check
  → filename-present check → duplicate-name-in-project check → MinIO `putObject`. See section 8
  for exact constants.
- No single-document metadata `GET` (only list + download) — see section 10.
- Delete is a genuine hard delete: MinIO object removed first, then the DB row; if the MinIO
  removal fails, a `FileStorageException` (500) is thrown and the DB row is **not** deleted
  (transaction not yet committed at that point) — i.e., failure leaves the document with its
  file intact and visible via list/download, no partial state.

### Cross-check against `backend/postman/`
The Postman collection (`DMS API`) has folders `Auth`, `Users`, `Projects`, `Tasks`, `Documents`,
`Setup`, matching the controllers above; all requests found there use the same paths, methods,
and `Authorization: bearer {{token}}` pattern described here. The collection stores only
requests (no saved example responses), so it doesn't independently confirm response bodies, but
two things are worth noting:
- The request named **"Get My Profile - 401 No Token"** (`GET /users/me`, no auth header)
  corroborates that unauthenticated access to a protected endpoint is expected/observed to
  return **401** (not 403) in this deployment — consistent with Spring Security's default
  `AuthenticationEntryPoint` behavior when no auth mechanism-specific entry point is configured.
- The request file for deleting a project is named `Delete Project - 200 Success-.request.yaml`
  on disk but its internal `name:` field says `"Delete Project - 204 No Content."` — a stale/
  inconsistent filename in the Postman collection itself (the code definitively returns 204, per
  `ProjectController.deleteProject`).

---

## 7. `GlobalExceptionHandler` — Error Response Shape

`@RestControllerAdvice`, single JSON shape produced by a private `buildResponse(status, message,
details)` helper:

```json
{
  "timestamp": "2026-07-28T10:15:30.123456",
  "status": 404,
  "message": "Project not found",
  "details": { }
}
```

- `timestamp`: `LocalDateTime.now().toString()` — **no timezone offset**, microsecond precision,
  not wrapped in quotes-as-ISO-instant (it's just Java's default `LocalDateTime` string form).
- `status`: the numeric HTTP status code (also duplicated in the actual HTTP status line).
- `message`: always present, human-readable.
- `details`: **only present** when non-null — currently only populated for validation errors
  (see below). Frontend code should treat `details` as optional/absent, not `null`.

| Exception | HTTP Status | `message` | `details` |
|---|---|---|---|
| `MethodArgumentNotValidException` (`@Valid` failures) | 400 | `"Validation failed"` | `Map<String,String>` of `fieldName → violation message` |
| `EmailAlreadyExistsException` | 409 | `"Email already in use: <email>"` | — |
| `ResourceAlreadyExistsException` | 409 | exception message (e.g. document name collision) | — |
| `InvalidOperationException` | 409 | exception message (e.g. last-admin guard, duplicate member) | — |
| `InvalidDocumentException` | 400 | exception message (empty file / too large / disallowed type / no filename) | — |
| `MethodArgumentTypeMismatchException` | 400 | `"Invalid value '<value>' for parameter '<name>'"` | — |
| `BadCredentialsException` | 401 | `"Invalid email or password"` (deliberately generic, doesn't reveal which field was wrong) | — |
| `DisabledException` | 401 | `"Account is disabled"` | — |
| `FileStorageException` | 500 | exception message (MinIO failure detail) | — |
| `ResourceNotFoundException` | 404 | exception message | — |
| `AuthorizationDeniedException` (from `@PreAuthorize` denial) | 403 | `"Access denied"` | — |
| `org.springframework.security.access.AccessDeniedException` (manual `checkOwner`/`checkMemberOrOwner` denial) | 403 | `"Access denied"` | — |
| any other `Exception` (catch-all) | 500 | `"An unexpected error occurred"` (original exception message is logged server-side only, never returned) | — |

**Fixed** (was previously a gap): `AccessDeniedException` — the type actually thrown by
`checkOwner`/`checkMemberOrOwner` in `ProjectService`, `TaskService`, and `DocumentService` for
"not owner"/"not a member" failures — now has its own `@ExceptionHandler`, added alongside the
existing `AuthorizationDeniedException` one (which `AccessDeniedException` is the superclass of,
so Spring's handler-resolution still picks the more specific `AuthorizationDeniedException`
handler for `@PreAuthorize` denials; this new handler only catches the manual-check case). Both
now return 403 "Access denied" with no `details`. Frontend error handling can now treat 403
uniformly for these endpoints.

---

## 8. File Upload / Storage Flow

- **Client bean**: `MinioConfig.minioClient()` builds a singleton `MinioClient` from
  `${minio.url}` / `${minio.access-key}` / `${minio.secret-key}` (all required env vars except
  `MINIO_URL`, which defaults to `http://localhost:9000`).
- **Bucket**: name fixed at `${minio.bucket}` = `dms-documents` (hardcoded default in
  `application.yml`, not overridable via an env var name). An `ApplicationRunner` bean checks
  `bucketExists` and calls `makeBucket` at startup if needed; failure wraps into
  `FileStorageException` and would fail app startup.
- **Object key**: `UUID.randomUUID().toString()` — a fresh random UUID per upload, unrelated to
  the filename or the DB id. Stored in `DocumentEntity.minioKey`. The original filename is
  **never used as the MinIO object key**, only stored as `DocumentEntity.name` for display and
  for the `Content-Disposition` header on download.
- **Upload flow** (`DocumentService.uploadDocument`, all in one `@Transactional` method):
  1. Authorization: caller must be project owner or member (`AccessDeniedException` → 403 if
     not, see section 7).
  2. `file.isEmpty()` → `InvalidDocumentException` ("File must not be empty.").
  3. `file.getSize() > MAX_FILE_SIZE` → `InvalidDocumentException` ("File exceeds the maximum
     allowed size of 50 MB."). **`MAX_FILE_SIZE = 50L * 1024 * 1024` bytes**, defined as a
     `private static final long` constant in `DocumentService`. Separately, Spring's own
     multipart limits are set in `application.yml`: `spring.servlet.multipart.max-file-size:
     50MB` and `max-request-size: 52MB` — a request whose multipart body exceeds those would be
     rejected by Spring/Tomcat *before* reaching `DocumentService` at all (likely as a
     `MultipartException`, which is **not** one of the explicitly handled exception types —
     falls to the generic 500 handler; see section 10).
  4. MIME type is **detected from file content** via `new Tika().detect(inputStream)` — the
     client-supplied `Content-Type` on the multipart part is ignored entirely for validation
     purposes.
  5. Detected MIME checked against a fixed allow-list, `ALLOWED_MIME_TYPES`
     (`private static final Set<String>` in `DocumentService`):
     ```
     application/pdf
     image/jpeg
     image/png
     image/gif
     image/webp
     text/plain
     text/csv
     application/vnd.openxmlformats-officedocument.wordprocessingml.document   (.docx)
     application/vnd.openxmlformats-officedocument.spreadsheetml.sheet          (.xlsx)
     application/vnd.openxmlformats-officedocument.presentationml.presentation  (.pptx)
     application/msword        (.doc)
     application/vnd.ms-excel  (.xls)
     application/vnd.ms-powerpoint (.ppt)
     ```
     Anything else → `InvalidDocumentException` ("File type '<mime>' is not allowed.").
  6. `file.getOriginalFilename()` null/blank → `InvalidDocumentException`.
  7. Duplicate name within the same project (`documentRepository.existsByProjectIdAndName`) →
     `ResourceAlreadyExistsException` (409) — also enforced at the DB level by the V6 unique
     constraint `uq_document_name_per_project (project_id, name)`.
  8. `minioClient.putObject(...)` with the detected content type; any exception →
     `FileStorageException` (500).
  9. `DocumentEntity` persisted with `name`, `type` (detected MIME), `size`, `minioKey`,
     `project`, `owner`.
- **Download flow** (`DocumentController.downloadDocument` + `DocumentService.downloadDocument`):
  - Service fetches the DB row (`findByIdAndProjectId`), opens a MinIO `getObject` stream, and
    returns a `DocumentDownload(stream, name, contentType, size)` record.
  - Controller builds the HTTP response:
    - `Content-Disposition: attachment; filename="<name>"` — the raw stored filename is
      interpolated directly into the header with only surrounding quotes, **no escaping/encoding**
      of quotes or non-ASCII characters (a filename containing a `"` would produce a malformed
      header) — see section 10.
    - `Content-Length: <size>` from the stored DB `size` value (not re-measured from the stream).
    - `Content-Type` parsed from the stored `type` (the MIME detected at upload time).
    - Body: `InputStreamResource` wrapping the MinIO stream directly — streamed, not buffered
      into memory.
  - Any MinIO failure (including "object not found" if the DB row and MinIO object have somehow
    diverged) is wrapped into `FileStorageException` (500), not a 404.
- **Delete flow**: `minioClient.removeObject` first; on success, `documentRepository.delete`.
  Failure at the MinIO step throws `FileStorageException` (500) and the DB row survives (see
  section 6 notes above).

---

## 9. `AuditAspect`

- Trigger: any service method annotated `@Audited(...)` (`annotation/Audited.java`). The
  aspect's pointcut is `@Around("@annotation(com.example.dms.annotation.Audited)")` — it applies
  to any Spring-managed bean method carrying the annotation, not just services, but in practice
  it is currently only used on methods in `UserService`, `ProjectService`, `TaskService`, and
  `DocumentService`.
- Annotated methods today:
  - `UserService.updateRole` → action `USER_ROLE_CHANGED`, entityType `USER`, entityId = `#id`,
    details = the new role name.
  - `UserService.deactivateUser` → action `USER_DEACTIVATED`, entityType `USER`, entityId = `#id`.
  - `ProjectService.addMember` / `removeMember` → `PROJECT_MEMBER_ADDED` /
    `PROJECT_MEMBER_REMOVED`, entityType `PROJECT`, entityId = `#projectId`, projectId =
    `#projectId`, details = `"userId=<userId>"`.
  - `ProjectService.deleteProject` → `PROJECT_DELETE`, entityType `PROJECT`, entityId = `#id`.
  - `TaskService.deleteTask` → `TASK_DELETE`, entityType `TASK`, entityId = `#taskId`, projectId
    = `#projectId`.
  - `DocumentService.uploadDocument` → `DOCUMENT_UPLOAD`, entityType `DOCUMENT`, entityId =
    `#result.id.toString()` (the newly created document's ID, available because the SpEL context
    binds `#result` to the method's **return value**, evaluated in the `finally` block after
    `proceed()`), projectId = `#projectId`, details = `#file.originalFilename`.
  - `DocumentService.downloadDocument` → `DOCUMENT_DOWNLOAD`.
  - `DocumentService.deleteDocument` → `DOCUMENT_DELETE`.
  - Notably, `createTask`, `createProject`, `updateProject`, `updateTask`, and
    `UserController`'s `updateMe`/`getMe` are **not** `@Audited` — only a subset of mutating
    operations are audited (see section 10).
  - Login events (`LOGIN_SUCCESS`, `LOGIN_FAILURE`) and the JWT-filter's
    `AUTH_DENIED_DEACTIVATED` event are logged by **direct calls to `AuditService.log(...)`**,
    not via `@Audited` — they bypass the aspect entirely (there's no method to annotate, since
    they originate from `AuthService`/`JwtAuthFilter` control flow, not a single clean service
    call boundary).
- Mechanics: on invocation, builds a SpEL `StandardEvaluationContext` binding every method
  parameter name (via `MethodSignature.getParameterNames()`, requires `-parameters` compiler
  info or debug symbols) to its runtime value. Calls `joinPoint.proceed()` inside
  try/finally: on `Throwable`, records `outcome = "FAILURE: <ExceptionSimpleName>"` and
  **rethrows** (does not swallow); the `finally` block always runs, binds `#result` (`null` on
  failure), evaluates the annotation's three optional SpEL expressions
  (`entityIdExpression`, `projectIdExpression`, `detailsExpression` — each defaults to `""`
  meaning "not evaluated" → `null`), and calls `auditService.log(actor, action, entityType,
  entityId, projectId, details + " | " + outcome)`. So **both successes and failures are
  logged**, with the outcome suffixed onto `details` (or standing alone as `details` if the
  annotation didn't specify a `detailsExpression`).
- Actor resolution: `SecurityContextHolder`'s current `Authentication.getName()` (the email), or
  the literal string `"anonymous"` if there's no authentication / it's the anonymous principal.
- Propagation: the aspect itself doesn't manage transactions, but `AuditService.log` is
  `@Transactional(propagation = Propagation.REQUIRES_NEW)` — audit rows are committed in an
  **independent transaction** from the business operation, so they persist even if the calling
  method's transaction later rolls back (e.g., an audited method that fails after `proceed()`
  still gets its `FAILURE` audit row committed, even though the business-side changes are rolled
  back).
- Failure isolation: if evaluating the SpEL expressions or the `auditService.log` call itself
  throws, that exception is caught and only logged (`log.error(...)`) — it never propagates and
  never masks the original business result/exception.

---

## 10. Flags for the Frontend Team

- ~~Manual authorization failures currently return HTTP 500, not 403~~ — **fixed.**
  `ProjectService`, `TaskService`, and `DocumentService` all throw
  `org.springframework.security.access.AccessDeniedException` from their
  `checkOwner`/`checkMemberOrOwner` helpers for "not owner"/"not a member" cases.
  `GlobalExceptionHandler` originally only had a handler for
  `org.springframework.security.authorization.AuthorizationDeniedException` (the type thrown by
  `@PreAuthorize`, used only on the three admin `UserController` endpoints), so these fell
  through to the generic `@ExceptionHandler(Exception.class)` handler and returned 500. A
  dedicated `@ExceptionHandler(AccessDeniedException.class)` was added (see section 7); every
  "not owner"/"not a member" rejection across Projects, Tasks, and Documents now correctly
  returns 403.
- **No CORS configuration exists anywhere** (no `CorsConfigurationSource` bean, no
  `WebMvcConfigurer`, no `@CrossOrigin`). A frontend served from a different origin (any typical
  dev server port) will be blocked by browser CORS unless something is added.
- **No single-resource GET for tasks or documents.** There is no `GET
  /projects/{projectId}/tasks/{taskId}` and no `GET
  /projects/{projectId}/documents/{documentId}` (metadata only, not a download). A frontend
  needing current data for one task/document must fetch it out of the list endpoint's response.
- **No pagination anywhere** — `GET /projects`, `GET /projects/{id}/tasks`, `GET
  /projects/{id}/documents`, and `GET /users` all return complete, unpaginated lists.
- **`ProjectStatus.ARCHIVED` is unreachable via the API.** The enum and DB column exist and
  default to `ACTIVE`, but no controller/service code path ever sets a project's status to
  `ARCHIVED` — there's no endpoint to archive/unarchive a project.
- **No way to un-assign a task or clear other optional fields.** `UpdateTaskRequest` treats
  `null` as "leave unchanged" for every field, so there is no way to explicitly clear
  `assigneeId` (unassign) or `deadline` via the update endpoint — only to change it to another
  value. The same applies to `UpdateProjectRequest.description` (can't be cleared, only
  replaced) and `UpdateProfileRequest`.
- **No account reactivation endpoint.** `PUT /users/{id}/deactivate` sets `active = false`; there
  is no corresponding "activate"/"reactivate" endpoint in `UserController`, so once deactivated
  through the API, a user cannot be re-enabled through the API (would require direct DB access).
- **Deleted projects and non-existent projects are indistinguishable.** `findActiveProject`
  throws the same `ResourceNotFoundException("Project not found")` for both a truly-unknown ID
  and a soft-deleted one.
- **`DocumentResponse.ownerId` is a bare `Long`**, unlike `ProjectResponse.owner` and
  `TaskResponse.creator`/`assignee`, which are nested `UserResponse` objects. A frontend wanting
  to show the uploader's name for a document needs a separate lookup (e.g., against the project's
  member/owner list) — the document endpoints don't embed it.
- **`Content-Disposition` filename is not escaped/encoded** on download — a stored filename
  containing a double-quote or non-ASCII characters (both possible: filenames are stored exactly
  as `MultipartFile.getOriginalFilename()`, and MIME/type validation, not filename validation,
  is the only upload gate) could produce a malformed header or a mis-decoded filename in some
  browsers/HTTP clients.
- **Two different multipart size limits can both apply to uploads.** Spring's
  `spring.servlet.multipart.max-file-size: 50MB` / `max-request-size: 52MB` (in
  `application.yml`) act at the servlet/container level before a request body is even fully
  read into a `MultipartFile`; `DocumentService.MAX_FILE_SIZE` (also 50 MB, in bytes) is a second,
  application-level check. A request that trips the Spring-level limit likely surfaces as a
  `MultipartException`/`MaxUploadSizeExceededException`, which has **no dedicated handler** in
  `GlobalExceptionHandler` and would fall to the generic 500 handler rather than the more
  specific `InvalidDocumentException` 400 path used for the application-level check.
- **`AuthResponse.role` is typed `String`, `UserResponse.role` is typed the `Role` enum** — they
  serialize identically as JSON strings, but this is worth knowing if the frontend's type
  generation is sensitive to it.
- **Audit logs are write-only from the application's perspective** — there is no controller or
  repository query exposed for reading `audit_logs`; only `AuditService.log(...)` writes to it,
  and only a subset of mutating operations are actually annotated `@Audited` (see section 9) —
  e.g. project/task creation and project/task updates are not audited, only deletion, membership
  changes, document operations, and admin user-management actions are.
- The top-level `README.md` (currently at the repository root, mid-move into `backend/` per the
  pending file relocation visible in `git status`) is broadly accurate against the code, but its
  API table omits the `assigneeId`-need-not-be-a-member nuance and the auth failure/500 issue
  above.
