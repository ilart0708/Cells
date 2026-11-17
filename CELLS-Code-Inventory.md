# CELLS Code Inventory

Inventory of the current working tree, including untracked source files, read on 2026-11-17. Secret values are masked. “UNCLEAR” and “NOT FOUND” are used where the repository does not establish an answer.

## 1. Project overview

### Folder tree (three directory levels; excludes `frontend/node_modules` and `backend/target`)

```text
backend/
├── .idea/
├── .mvn/wrapper/
├── config/
└── src/
    ├── main/
    │   ├── java/com/cells/
    │   └── resources/db/
    └── test/
        ├── java/com/cells/
        └── resources/ [directory not present]

frontend/
├── .vscode/
├── dist/ [generated build output]
├── public/
└── src/
    ├── api/ (__tests__)
    ├── assets/
    ├── components/
    ├── composables/ (__tests__)
    ├── constants/
    ├── layouts/
    ├── router/ (__tests__)
    ├── stores/ (__tests__)
    ├── utils/
    └── views/ (__tests__, admin/, pastor/)
```

### Versions and key dependencies

| Area | Declared version / evidence |
|---|---|
| Spring Boot | Parent `spring-boot-starter-parent` 4.1.2 (`backend/pom.xml`) |
| Java | 21 (`backend/pom.xml`) |
| Maven | Maven wrapper present (`backend/mvnw`, `backend/.mvn/wrapper/`); wrapper distribution version not summarized here. |
| PostgreSQL | 18 (`docker-compose.yml`; test container is `postgres:18`) |
| Frontend package / Node | `frontend@0.0.0`; engines `^22.18.0 || >=24.12.0` |
| Vue / Router / Pinia | Declared `vue ^3.5.42`, `vue-router ^5.3.1`, `pinia ^4.0.3`; lockfile resolves Vue 3.5.45, Router 5.5.1, Pinia 4.1.0. |
| Vite / Tailwind | Declared Vite `^8.2.2`, Tailwind `^4.3.5`, `@tailwindcss/vite ^4.3.5`; lockfile resolves Vite 8.3.8. |
| Frontend tests/lint | Vitest `^4.1.11` (locked 4.1.12), Oxlint `~1.82.0`, ESLint `^10.10.0`. |
| Backend dependencies | Spring Boot Actuator, Data JPA, Flyway, Security, OAuth2 Resource Server, Validation, WebMVC; Flyway PostgreSQL module; PostgreSQL JDBC; Lombok and DevTools. Versions are parent-managed, not individually pinned in `pom.xml`. Test starters and Testcontainers PostgreSQL/JUnit dependencies are declared. |

### Configuration keys

Configuration values that may be credentials, signing keys, or passwords are shown as `****`. The packaged `backend/src/main/resources/application.yaml` contains only `spring.application.name: backend`. The external `backend/config/application.yml` activates the local profile; loading that external directory depends on the Spring Boot config search path/current working directory.

| File | Keys and non-secret values |
|---|---|
| `backend/config/application.yml` | `spring.profiles.active=local`; `spring.jpa.open-in-view=false`; `spring.jpa.hibernate.ddl-auto=validate`; `app.bootstrap.admin.email=${BOOTSTRAP_ADMIN_EMAIL:}`; `app.bootstrap.admin.password=****`; `app.bootstrap.admin.full-name=${BOOTSTRAP_ADMIN_NAME:Platform Administrator}`; `app.jwt.secret=****`; `app.jwt.issuer=cells`; `app.jwt.access-token-ttl=15m`; `app.refresh.ttl=7d`; `app.refresh.cookie-secure=${REFRESH_COOKIE_SECURE:true}`; `app.dev-seed.password=****`; `app.invitation.ttl=7d`; `app.invitation.base-url=${APP_FRONTEND_URL:http://localhost:5173}`. |
| `backend/config/application-local.yml` | `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password=****`; `spring.flyway.enabled=true`; `app.jwt.secret=****`; `app.refresh.cookie-secure=false`. |
| `backend/src/main/resources/application.yaml` | `spring.application.name=backend`; no profile activation. |

## 2. Database migrations

### Flyway migrations

| Version | File | Tables/columns/constraints/indexes created or changed |
|---:|---|---|
| 1 | `V1__init.sql` | Empty/no-op. |
| 2 | `V2__create_users.sql` | Creates `users`, role/status checks, and unique index on lowercase email. |
| 3 | `V3__create_refresh_tokens.sql` | Creates `refresh_tokens`, token-hash unique index, family and user indexes. |
| 4 | `V4__create_churches.sql` | Empty/no-op. |
| 5 | `V5__create_churches.sql` | Creates `churches`, status check, unique index on lowercase name. |
| 6 | `V6__create_invitations.sql` | Empty/no-op. |
| 7 | `V7__create_invitations.sql` | Creates `invitations`, role check, token-hash unique index, email/church indexes. |
| 8 | `V8__create_pastors.sql` | Creates `pastors`, church index; backfills pastors from accepted pastor invitations, ignoring conflicts. |
| 9 | `V9__create_leaders.sql` | Empty/no-op. |
| 10 | `V10__create_leaders.sql` | Creates `cell_group_leaders` and `pastor_leaders`; adds church/pastor indexes. |
| 11 | `V11__create_rooms.sql` | Creates `rooms`, status/archive consistency checks, active-room partial unique index, leader/church indexes. |
| 12 | `V12__create_members_and_room_members.sql` | Creates `members` and `room_members`, positive participation-number check, partial unique indexes for active room-number and room-member pairs, member index. |
| 13 | `V13__add_room_to_invitations.sql` | Adds nullable `invitations.room_id`, FK/index, and check requiring a room exactly for `MEMBER` invitations. |

The migration names/descriptions for V4/V5 and V6/V7 are duplicate pairs; V4, V1, V6, and V9 currently contain no SQL.

### Final business schema

`NOT NULL` is stated per column; nullable columns are explicitly marked. Identity columns are `BIGINT GENERATED ALWAYS AS IDENTITY`.

| Table | Columns, keys, constraints, indexes |
|---|---|
| `users` | `id BIGINT NOT NULL` PK; `email VARCHAR(255) NOT NULL`; `password_hash VARCHAR(255) NOT NULL`; `full_name VARCHAR(150) NOT NULL`; `role VARCHAR(20) NOT NULL`; `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Checks: role `ADMIN/PASTOR/LEADER/MEMBER`; status `ACTIVE/PENDING/DISABLED`. Unique index on `lower(email)`. |
| `refresh_tokens` | `id BIGINT NOT NULL` PK; `user_id BIGINT NOT NULL` FK `users(id) ON DELETE CASCADE`; `token_hash VARCHAR(64) NOT NULL`; `family_id UUID NOT NULL`; `expires_at TIMESTAMPTZ NOT NULL`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`; `revoked_at TIMESTAMPTZ NULL`. Unique token-hash index; family and user indexes. |
| `churches` | `id BIGINT NOT NULL` PK; `name VARCHAR(150) NOT NULL`; `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Status check `ACTIVE/DISABLED`; unique index on `lower(name)`. |
| `invitations` | `id BIGINT NOT NULL` PK; `token_hash VARCHAR(64) NOT NULL`; `email VARCHAR(255) NOT NULL`; `full_name VARCHAR(150) NOT NULL`; `role VARCHAR(20) NOT NULL`; `church_id BIGINT NOT NULL` FK to `churches`; `invited_by BIGINT NOT NULL` FK to `users`; `expires_at TIMESTAMPTZ NOT NULL`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`; `room_id BIGINT NULL` FK to `rooms`; `accepted_at TIMESTAMPTZ NULL`; `revoked_at TIMESTAMPTZ NULL`. Role check `PASTOR/LEADER/MEMBER`; V13 room/role check; unique token-hash index; email/church/room indexes. |
| `pastors` | `user_id BIGINT NOT NULL` PK/FK `users(id) ON DELETE CASCADE`; `church_id BIGINT NOT NULL` FK `churches`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Church index. |
| `cell_group_leaders` | `user_id BIGINT NOT NULL` PK/FK `users(id) ON DELETE CASCADE`; `church_id BIGINT NOT NULL` FK `churches`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Church index. |
| `pastor_leaders` | `leader_id BIGINT NOT NULL` PK/FK `cell_group_leaders(user_id) ON DELETE CASCADE`; `pastor_id BIGINT NOT NULL` FK `pastors(user_id)`; `assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Pastor index. |
| `rooms` | `id BIGINT NOT NULL` PK; `church_id BIGINT NOT NULL` FK `churches`; `leader_id BIGINT NOT NULL` FK `cell_group_leaders(user_id)`; `name VARCHAR(100) NOT NULL`; `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`; `archived_at TIMESTAMPTZ NULL`. Status check `ACTIVE/ARCHIVED`; check keeps `archived_at` consistent with status. Partial unique `(leader_id, lower(name)) WHERE status='ACTIVE'`; leader/church indexes. |
| `members` | `user_id BIGINT NOT NULL` PK/FK `users(id) ON DELETE CASCADE`; `church_id BIGINT NOT NULL` FK `churches`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Church index. |
| `room_members` | `id BIGINT NOT NULL` PK; `room_id BIGINT NOT NULL` FK `rooms`; `member_id BIGINT NOT NULL` FK `members(user_id)`; `participation_number INTEGER NOT NULL`; `joined_at TIMESTAMPTZ NOT NULL DEFAULT now()`; `removed_at TIMESTAMPTZ NULL`. Check `participation_number >= 1`; partial unique `(room_id, participation_number)` and `(room_id, member_id)` where `removed_at IS NULL`; member index. |

Flyway also creates and maintains `flyway_schema_history`; its schema is Flyway-managed, not defined by the business migrations above.

### Seed data

`DevSeedRunner` runs under profile `local`, is transactional/idempotent, and skips when the configured seed password is shorter than 12 characters. It creates Demo Church; `pastor@example.com` (PASTOR, Test Pastor); `leader@example.com` (LEADER, Test Leader); Demo Room; and `member@example.com` (MEMBER, Test Member), `member2@example.com` (MEMBER, Second Member), `member3@example.com` (MEMBER, Third Member). No passwords are listed. `BootstrapAdminRunner` separately creates an admin only when both configured email and password are set and the password has at least 12 characters; password values are not recorded here.

## 3. Backend packages

Type/purpose inventory below is grouped by declared Java package. `DTO` means request/response record; generated record accessors and inherited repository methods are not enumerated.

### `com.cells.backend`

| Class | Type | Purpose |
|---|---|---|
| `BackendApplication` | Application | Boot entry point; scans configuration-property records. |

### `com.cells.backend.auth`

| Class | Type | Purpose |
|---|---|---|
| `AuthController` | Controller | Login, refresh, logout, current-user routes. |
| `AuthResult` | Internal record | Login response plus refresh-token value for controller cookie handling. |
| `AuthService` | Service | Authenticates users and manages sessions. |
| `LoginRequest` | DTO record | Validated login email/password input. |
| `LoginResponse` | DTO record | Access token, token type, expiry, user response. |
| `RefreshCookieFactory` | Helper | Creates and clears refresh cookies. |
| `RefreshToken` | Entity | Persisted hashed refresh-token/family/revocation state. |
| `RefreshTokenRepository` | Repository | Looks up refresh tokens and revokes families. |
| `RefreshTokenService` | Service | Issues, rotates, and revokes refresh-token families. |
| `TokenService` | Service/helper | Creates signed access JWTs; contains `AccessToken`. |

### `com.cells.backend.church`

| Class | Type | Purpose |
|---|---|---|
| `Church` | Entity | Church name/status state. |
| `ChurchController` | Controller | Admin church create/list/get/rename routes. |
| `ChurchRepository` | Repository | Church lookup, uniqueness, and search. |
| `ChurchRequest` | DTO record | Validated church-name input. |
| `ChurchResponse` | DTO record | Church response; maps from `Church`. |
| `ChurchService` | Service | Church creation, uniqueness, search, lookup, rename. |
| `ChurchStatus` | Enum | `ACTIVE`, `DISABLED`. |

### `com.cells.backend.common`

| Class | Type | Purpose |
|---|---|---|
| `AccountNotActiveException` | Exception | Inactive account rejection. |
| `BusinessRuleException` | Exception | Product-rule rejection. |
| `DuplicateResourceException` | Exception | Duplicate/conflicting resource. |
| `GlobalExceptionHandler` | Advice/controller | Maps domain/framework exceptions to problem responses. |
| `InvalidCredentialsException` | Exception | Invalid login credentials. |
| `InvalidInvitationException` | Exception | Invalid/expired/revoked invitation. |
| `PageResponse<T>` | DTO record | Stable page response with `from(Page<T>)`. |
| `ResourceNotFoundException` | Exception | Missing or intentionally concealed resource. |
| `TokenHasher` | Utility | Random token generation and SHA-256 hashing. |

### `com.cells.backend.config`

| Class | Type | Purpose |
|---|---|---|
| `BootstrapAdminProperties` | Config record | Bootstrap admin settings. |
| `BootstrapAdminRunner` | Application runner | Optionally creates initial admin. |
| `DevSeedRunner` | Application runner | Local demo seed. |
| `InvitationProperties` | Config record | Invitation TTL and frontend URL. |
| `JwtConfig` | Configuration | HMAC key, JWT encoder/decoder. |
| `JwtProperties` | Config record | JWT secret, issuer, access TTL. |
| `PasswordConfig` | Configuration | Password-encoder bean. |
| `RefreshProperties` | Config record | Refresh TTL and cookie Secure flag. |

### `com.cells.backend.invitation`

| Class | Type | Purpose |
|---|---|---|
| `AcceptInvitationRequest` | DTO record | Token, full name, password acceptance input. |
| `AdminInvitationController` | Controller | Admin pastor-invitation create/list/revoke. |
| `Invitation` | Entity | Invitation lifecycle, scope, and timestamps. |
| `InvitationController` | Controller | Public invitation preview/accept. |
| `InvitationPreviewResponse` | DTO record | Public invitation preview. |
| `InvitationRepository` | Repository | Invitation persistence, locking, scoped queries/revocation. |
| `InvitationResponse` | DTO record | Invitation response; response mapping factory. |
| `InvitationService` | Service | Creates/revokes/previews/accepts pastor, leader, member invitations. |
| `InvitationStatus` | Enum | `PENDING`, `ACCEPTED`, `EXPIRED`, `REVOKED`. |
| `MemberInvitationRequest` | DTO record | Validated room-member invite input. |
| `PastorInvitationRequest` | DTO record | Validated email/name/church invite input. |
| `TokenRequest` | DTO record | Validated invitation token input. |
| `LeaderInvitationRequest` | DTO record | Validated leader-invite input; physically under `leader/`. |

### `com.cells.backend.leader`

| Class | Type | Purpose |
|---|---|---|
| `AdminLeaderController` | Controller | Admin leader list/status routes. |
| `CellGroupLeader` | Entity | Leader user-to-church row. |
| `CellGroupLeaderRepository` | Repository | Leader searches/projections, including pastor scope. |
| `LeaderMemberInvitationController` | Controller | Room-scoped leader member-invitation routes. |
| `LeaderResponse` | DTO record | Leader projection. |
| `LeaderService` | Service | Admin/pastor leader list, scope checks, status changes. |
| `PastorLeader` | Entity | Pastor-to-leader assignment. |
| `PastorLeaderController` | Controller | Pastor leader management and leader invitation routes. |
| `PastorLeaderRepository` | Repository | Checks pastor/leader assignment. |
| `UserStatusRequest` | DTO record | Account-status input. |

### `com.cells.backend.member`

| Class | Type | Purpose |
|---|---|---|
| `AvailableMemberResponse` | DTO record | Addable member ID/name/email projection. |
| `Member` | Entity | Member user-to-church row. |
| `MemberRepository` | Repository | Searches active church members not enrolled in a room. |
| `RoomMember` | Entity | Room-membership history and participation number; declared in package `room`. |
| `RoomMemberRepository` | Repository | Active membership lookup, numbering, roster projection; declared in package `room`. |
| `RoomMemberResponse` | DTO record | Room roster/member status/participation response; declared in package `room`. |

### `com.cells.backend.pastor`

| Class | Type | Purpose |
|---|---|---|
| `AdminPastorController` | Controller | Admin pastor list/get/status routes. |
| `Pastor` | Entity | Pastor user-to-church row. |
| `PastorRepository` | Repository | Pastor search and response projection queries. |
| `PastorResponse` | DTO record | Pastor projection. |
| `PastorService` | Service | Pastor list/search/get/status changes. |
| `PastorStatusRequest` | DTO record | Pastor status input. |

### `com.cells.backend.room`

| Class | Type | Purpose |
|---|---|---|
| `AddMemberRequest` | DTO record | Existing member ID input. |
| `LeaderRoomController` | Controller | Owner-scoped room CRUD/status. |
| `LeaderRoomMemberController` | Controller | Roster, available-member, enrollment routes. |
| `Room` | Entity | Room ownership/status/archive state. |
| `RoomFilter` | Enum | `ACTIVE`, `ARCHIVED`, `ALL` list filter. |
| `RoomMemberService` | Service | Room roster, member search, enrollment/number assignment. |
| `RoomRepository` | Repository | Room search/projections, owner lookup, row lock. |
| `RoomRequest` | DTO record | Validated room-name input. |
| `RoomResponse` | DTO record | Room projection with active member count. |
| `RoomService` | Service | Owner-scoped room create/list/rename/archive/restore. |
| `RoomStatus` | Enum | `ACTIVE`, `ARCHIVED`. |
| `RoomStatusRequest` | DTO record | Room status input. |
| `RoomMember`, `RoomMemberRepository`, `RoomMemberResponse` | Entity/repository/DTO | Declared in `room` but physically under `member/`. |

### `com.cells.backend.security`

| Class | Type | Purpose |
|---|---|---|
| `ProblemResponseWriter` | Helper | Writes fixed security problem JSON. |
| `RestAccessDeniedHandler` | Handler | Writes JSON 403. |
| `RestAuthenticationEntryPoint` | Handler | Writes JSON 401 and Bearer challenge. |
| `SecurityConfig` | Configuration | Stateless request security, public paths, JWT role conversion, method security. |

### `com.cells.backend.user`

| Class | Type | Purpose |
|---|---|---|
| `Role` | Enum | `ADMIN`, `PASTOR`, `LEADER`, `MEMBER`. |
| `User` | Entity | Account credentials, role/status, activation/deactivation. |
| `UserRepository` | Repository | User lookup by email/existence. |
| `UserResponse` | DTO record | Public user projection excluding password hash. |
| `UserStatus` | Enum | `ACTIVE`, `PENDING`, `DISABLED`. |

### Entities: fields, relations, constructors, key methods

JPA entities have a protected no-argument constructor. Declared object relations below are `@ManyToOne(fetch=LAZY)`; other FK IDs are scalar Java fields.

| Entity | Fields / relations | Public constructor | Key methods |
|---|---|---|---|
| `User` | `Long id`, `String email`, `String passwordHash`, `String fullName`, `Role role`, `UserStatus status`, `Instant createdAt`; no entity relations. | `User(String email, String passwordHash, String fullName, Role role)` | `disable()`, `activate()`; getters, `equals`, `hashCode`. |
| `Church` | `Long id`, `String name`, `ChurchStatus status`, `Instant createdAt`; none. | `Church(String name)` | `rename(String)`; getters, `equals`, `hashCode`. |
| `RefreshToken` | `Long id`, `Long userId`, `String tokenHash`, `UUID familyId`, `Instant expiresAt`, `Instant createdAt`, `Instant revokedAt`; `userId` scalar. | `RefreshToken(Long userId, String tokenHash, UUID familyId, Instant expiresAt)` | `isRevoked()`, `isExpired(Instant)`, `revoke(Instant)`; getters. |
| `Invitation` | `Long id`, `String tokenHash`, `String email`, `String fullName`, `Role role`, `Church church`, `Room room`, `Long invitedBy`, `Instant expiresAt`, `Instant acceptedAt`, `Instant revokedAt`, `Instant createdAt`; church required LAZY many-to-one, room optional LAZY many-to-one. | `Invitation(String tokenHash, String email, String fullName, Role role, Church church, Long invitedBy, Instant expiresAt)`; overload includes `Room room`. | `getStatus(Instant)`, `markAccepted(Instant)`, `markRevoked(Instant)`; getters. |
| `Pastor` | `Long userId`, `Church church`, `Instant createdAt`; required LAZY many-to-one church. | `Pastor(Long userId, Church church)` | Getters. |
| `CellGroupLeader` | `Long userId`, `Church church`, `Instant createdAt`; required LAZY many-to-one church. | `CellGroupLeader(Long userId, Church church)` | Getters. |
| `PastorLeader` | `Long leaderId`, `Long pastorId`, `Instant assignedAt`; scalar IDs. | `PastorLeader(Long leaderId, Long pastorId)` | Getters. |
| `Member` | `Long userId`, `Church church`, `Instant createdAt`; required LAZY many-to-one church. | `Member(Long userId, Church church)` | Getters. |
| `Room` | `Long id`, `Church church`, `Long leaderId`, `String name`, `RoomStatus status`, `Instant createdAt`, `Instant archivedAt`; required LAZY many-to-one church; scalar leader ID. | `Room(Church church, Long leaderId, String name)` | `rename(String)`, `archive(Instant)`, `restore()`; getters. |
| `RoomMember` | `Long id`, `Long roomId`, `Long memberId`, `Integer participationNumber`, `Instant joinedAt`, `Instant removedAt`; scalar IDs. | `RoomMember(Long roomId, Long memberId, Integer participationNumber)` | `markRemoved(Instant)`; getters. |

### Repositories

All extend `JpaRepository<Entity, Long>` unless noted; inherited CRUD is not repeated.

| Repository | Declared methods and query behavior |
|---|---|
| `UserRepository` | `Optional<User> findByEmail(String email)`; `boolean existsByEmail(String email)`. |
| `RefreshTokenRepository` | `Optional<RefreshToken> findByTokenHash(String tokenHash)`; `int revokeFamily(UUID familyId, Instant now)` (`@Modifying`, `@Query`). |
| `ChurchRepository` | `boolean existsByNameIgnoreCase(String name)`; `boolean existsByNameIgnoreCaseAndIdNot(String name, Long id)`; `Page<Church> findByNameContainingIgnoreCase(String name, Pageable pageable)`; `Optional<Church> findByNameIgnoreCase(String name)`. |
| `InvitationRepository` | `Optional<Invitation> findByTokenHash(String tokenHash)`; `Optional<Invitation> findByTokenHashForUpdate(String hash)` (`@Lock(PESSIMISTIC_WRITE)`, `@Query`); `Page<Invitation> findAll(Pageable pageable)` (`@Override`, `@EntityGraph`); `Page<Invitation> findByInvitedBy(Long invitedBy, Pageable pageable)`; `Page<Invitation> findByRoom_IdAndInvitedBy(Long roomId, Long invitedBy, Pageable pageable)`; `Optional<Invitation> findByIdAndInvitedBy(Long id, Long invitedBy)`; `Optional<Invitation> findByIdAndInvitedByAndRoom_Id(Long id, Long invitedBy, Long roomId)`; `int revokePendingForEmail(String email, Long invitedBy, Instant now)`; `int revokePendingForEmailInRoom(String email, Long invitedBy, Long roomId, Instant now)`. The three list methods use `@EntityGraph(attributePaths={"church","room"})`; revoke methods are `@Modifying @Query`. |
| `PastorRepository` | `Page<PastorResponse> search(String pattern, Pageable pageable)`; `Optional<PastorResponse> findResponseById(Long id)` (`@Query`). |
| `CellGroupLeaderRepository` | `Page<LeaderResponse> search(String pattern, Pageable pageable)`; `Page<LeaderResponse> searchForPastor(String pattern, Long pastorId, Pageable pageable)`; `Optional<LeaderResponse> findResponseById(Long id)`; `Optional<LeaderResponse> findResponseByIdForPastor(Long id, Long pastorId)` (`@Query`). |
| `PastorLeaderRepository` | `boolean existsByLeaderIdAndPastorId(Long leaderId, Long pastorId)`. |
| `MemberRepository` | `Page<AvailableMemberResponse> searchAvailable(Long churchId, Long roomId, UserStatus status, String pattern, Pageable pageable)` (`@Query`). |
| `RoomMemberRepository` | `boolean existsByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId)`; `Optional<RoomMember> findByRoomIdAndMemberIdAndRemovedAtIsNull(Long roomId, Long memberId)`; `int maxActiveNumber(Long roomId)` (`@Query`); `List<RoomMemberResponse> roster(Long roomId)` (`@Query`). |
| `RoomRepository` | `Page<RoomResponse> searchOwn(Long leaderId, Collection<RoomStatus> statuses, String pattern, Pageable pageable)`; `Optional<RoomResponse> findOwnResponse(Long id, Long leaderId)`; `Optional<Room> findByIdAndLeaderId(Long id, Long leaderId)`; `Optional<Room> findOwnForUpdate(Long id, Long leaderId)` (`@Lock(PESSIMISTIC_WRITE)`, `@Query`); `Optional<Room> findByLeaderIdAndStatusAndNameIgnoreCase(Long leaderId, RoomStatus status, String name)`; `boolean existsByLeaderIdAndStatusAndNameIgnoreCase(Long leaderId, RoomStatus status, String name)`; `boolean existsByLeaderIdAndStatusAndNameIgnoreCaseAndIdNot(Long leaderId, RoomStatus status, String name, Long id)`. |

### Services

Modes: “normal” means `@Transactional` default; “read-only” means `@Transactional(readOnly=true)`; “none” means no declared transaction annotation. Spring/JPA infrastructure exceptions may also propagate.

| Service | Public methods (exact signatures; mode) | Rules and domain exceptions evidenced |
|---|---|---|
| `AuthService` | `AuthResult login(LoginRequest request)` (normal); `AuthResult refresh(String rawToken)` (`noRollbackFor={InvalidCredentialsException.class, AccountNotActiveException.class}`); `void logout(String rawToken)` (normal); `UserResponse currentUser(Long userId)` (read-only); `AuthResult startSession(User user)` (normal). | Normalizes login email; uses dummy hash for unknown users; rejects credentials/inactive accounts. `InvalidCredentialsException`, `AccountNotActiveException`. |
| `TokenService` | `AccessToken createAccessToken(User user)` (none). | Creates access JWT. |
| `RefreshTokenService` | `String issue(Long userId)`; `Optional<Rotation> rotate(String rawToken)`; `void revoke(String rawToken)`; `void revokeFamily(UUID familyId)` (all normal). | Unknown/expired/revoked token returns empty rotation; reuse revokes family. |
| `ChurchService` | `ChurchResponse create(ChurchRequest request)` (normal); `PageResponse<ChurchResponse> list(String query, int page, int size)` (read-only); `ChurchResponse get(Long id)` (read-only); `ChurchResponse rename(Long id, ChurchRequest request)` (normal). | Trims names, bounds page size; `DuplicateResourceException`, `ResourceNotFoundException`. |
| `InvitationService` | `InvitationResponse invitePastor(Long adminId, PastorInvitationRequest request)` (normal); `InvitationResponse inviteLeader(Long pastorId, LeaderInvitationRequest request)` (normal); `InvitationResponse inviteMember(Long leaderId, Long roomId, MemberInvitationRequest request)` (normal); `PageResponse<InvitationResponse> list(int page, int size)` (read-only); `PageResponse<InvitationResponse> listForInviter(Long inviterId, int page, int size)` (read-only); `PageResponse<InvitationResponse> listForRoom(Long leaderId, Long roomId, int page, int size)` (read-only); `void revoke(Long id)`, `void revokeOwn(Long id, Long inviterId)`, `void revokeForRoom(Long leaderId, Long roomId, Long id)` (normal); `InvitationPreviewResponse preview(String token)` (read-only); `AuthResult accept(AcceptInvitationRequest request)` (normal). | Inviter/scope checks; accepted invitations cannot be revoked; acceptance row-locks invitation; member acceptance enrolls via room logic. Uses `ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`, `InvalidInvitationException`. |
| `LeaderService` | `PageResponse<LeaderResponse> listAll(String query, int page, int size)` (read-only); `LeaderResponse setStatusAsAdmin(Long leaderId, UserStatus status)` (normal); `PageResponse<LeaderResponse> listForPastor(Long pastorId, String query, int page, int size)` (read-only); `LeaderResponse getForPastor(Long pastorId, Long leaderId)` (read-only); `LeaderResponse setStatusForPastor(Long pastorId, Long leaderId, UserStatus status)` (normal). | Enforces pastor assignment and disallows manual `PENDING`; `ResourceNotFoundException`, `BusinessRuleException`. |
| `PastorService` | `PageResponse<PastorResponse> list(String query, int page, int size)` (read-only); `PastorResponse get(Long id)` (read-only); `PastorResponse setStatus(Long id, UserStatus status)` (normal). | Disallows manual `PENDING`; `ResourceNotFoundException`, `BusinessRuleException`. |
| `RoomService` | `RoomResponse create(Long leaderId, RoomRequest request)` (normal); `PageResponse<RoomResponse> list(Long leaderId, String query, RoomFilter filter, int page, int size)` (read-only); `RoomResponse get(Long leaderId, Long roomId)` (read-only); `RoomResponse rename(Long leaderId, Long roomId, RoomRequest request)` (normal); `RoomResponse setStatus(Long leaderId, Long roomId, RoomStatus status)` (normal). | Owner/own-church scoping, active-name uniqueness, archive/restore rules; `ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`. |
| `RoomMemberService` | `List<RoomMemberResponse> roster(Long leaderId, Long roomId)` (read-only); `PageResponse<AvailableMemberResponse> availableMembers(Long leaderId, Long roomId, String query, int page, int size)` (read-only); `RoomMemberResponse enroll(Long leaderId, Long roomId, Long memberId)` (normal). | Enforces owner/church/account/room status, active duplicate checks; locks room and assigns `max active number + 1`; `ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`. |

### DTO and configuration records

| Record | Components |
|---|---|
| `LoginRequest` | `String email`, `String password` |
| `LoginResponse` | `String accessToken`, `String tokenType`, `long expiresIn`, `UserResponse user` |
| `AuthResult` | `LoginResponse body`, `String refreshToken` |
| `AccessToken` | `String value`, `long expiresInSeconds` |
| `Rotation` | `Long userId`, `UUID familyId`, `String newToken` |
| `ChurchRequest` | `String name` |
| `ChurchResponse` | `Long id`, `String name`, `ChurchStatus status`, `Instant createdAt` |
| `AcceptInvitationRequest` | `String token`, `String fullName`, `String password` |
| `PastorInvitationRequest` | `String email`, `String fullName`, `Long churchId` |
| `LeaderInvitationRequest` | `String email`, `String fullName` |
| `MemberInvitationRequest` | `String email`, `String fullName` |
| `TokenRequest` | `String token` |
| `InvitationPreviewResponse` | `String email`, `String fullName`, `Role role`, `String churchName`, `String roomName`, `String leaderName`, `Instant expiresAt` |
| `InvitationResponse` | `Long id`, `String email`, `String fullName`, `Role role`, `Long churchId`, `String churchName`, `Long roomId`, `String roomName`, `InvitationStatus status`, `Instant expiresAt`, `Instant createdAt`, `String inviteUrl` |
| `LeaderResponse` | `Long id`, `String fullName`, `String email`, `UserStatus status`, `Long churchId`, `String churchName`, `Long pastorId`, `String pastorName`, `Instant createdAt` |
| `UserStatusRequest`, `PastorStatusRequest` | `UserStatus status` |
| `PastorResponse` | `Long id`, `String fullName`, `String email`, `UserStatus status`, `Long churchId`, `String churchName`, `Instant createdAt` |
| `AvailableMemberResponse` | `Long id`, `String fullName`, `String email` |
| `AddMemberRequest` | `Long memberId` |
| `RoomRequest` | `String name` |
| `RoomStatusRequest` | `RoomStatus status` |
| `RoomResponse` | `Long id`, `String name`, `RoomStatus status`, `Long churchId`, `String churchName`, `Long leaderId`, `String leaderName`, `Long memberCount`, `Instant createdAt`, `Instant archivedAt` |
| `RoomMemberResponse` | `Long memberId`, `String fullName`, `String email`, `UserStatus accountStatus`, `Integer participationNumber`, `Instant joinedAt` |
| `UserResponse` | `Long id`, `String email`, `String fullName`, `Role role` |
| `PageResponse<T>` | `List<T> content`, `int page`, `int size`, `long totalElements`, `int totalPages` |
| `BootstrapAdminProperties` | `String email`, `String password`, `String fullName` |
| `JwtProperties` | `String secret`, `String issuer`, `Duration accessTokenTtl` |
| `RefreshProperties` | `Duration ttl`, `boolean cookieSecure` |
| `InvitationProperties` | `Duration ttl`, `String baseUrl` |

Other public helpers: `TokenHasher.newToken()`, `TokenHasher.sha256(String raw)`, `RefreshCookieFactory.create(String value)`, `RefreshCookieFactory.clear()`, `PasswordConfig.passwordEncoder()`, `JwtConfig.jwtSecretKey()`, `JwtConfig.jwtDecoder(SecretKey jwtSecretKey)`, `JwtConfig.jwtEncoder(SecretKey jwtSecretKey)`, `SecurityConfig.securityFilterChain(HttpSecurity http, RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler accessDeniedHandler)`, and runner `run(ApplicationArguments args)` methods. DTO factories include `InvitationResponse.from(Invitation, Instant, String)`, `ChurchResponse.from(Church)`, `UserResponse.from(User)`, and `PageResponse.from(Page<T>)`.

### Controllers and endpoints

Ordinary body-returning methods respond 200; explicit creates respond 201 and deletes/logout respond 204. Controller class-level roles apply unless marked public/authenticated. Request query/path arguments are shown in the request column.

Class-level controller annotations (literal source values):

| Controller | `@RequestMapping` | `@PreAuthorize` |
|---|---|---|
| `AuthController` | `@RequestMapping("/api/v1/auth")` | NONE |
| `ChurchController` | `@RequestMapping("/api/v1/admin/churches")` | `@PreAuthorize("hasRole('ADMIN')")` |
| `AdminInvitationController` | `@RequestMapping("/api/v1/admin/invitations")` | `@PreAuthorize("hasRole('ADMIN')")` |
| `InvitationController` | `@RequestMapping("/api/v1/invitations")` | NONE |
| `AdminLeaderController` | `@RequestMapping("/api/v1/admin/leaders")` | `@PreAuthorize("hasRole('ADMIN')")` |
| `LeaderMemberInvitationController` | `@RequestMapping("/api/v1/leader/rooms/{roomId}/invitations")` | `@PreAuthorize("hasRole('LEADER')")` |
| `PastorLeaderController` | `@RequestMapping("/api/v1/pastor")` | `@PreAuthorize("hasRole('PASTOR')")` |
| `AdminPastorController` | `@RequestMapping("/api/v1/admin/pastors")` | `@PreAuthorize("hasRole('ADMIN')")` |
| `LeaderRoomController` | `@RequestMapping("/api/v1/leader/rooms")` | `@PreAuthorize("hasRole('LEADER')")` |
| `LeaderRoomMemberController` | `@RequestMapping("/api/v1/leader/rooms/{roomId}/members")` | `@PreAuthorize("hasRole('LEADER')")` |

| Class-level access | Method and full path | Request | Response / success |
|---|---|---|---|
| Public auth routes; `/api/v1/auth` | `POST /login` | `LoginRequest` | `ResponseEntity<LoginResponse>` / 200 |
|  | `POST /refresh` | Refresh cookie; no body | `ResponseEntity<LoginResponse>` / 200 |
|  | `POST /logout` | Refresh cookie; no body | `ResponseEntity<Void>` / 204 |
| Authenticated, any role | `GET /api/v1/auth/me` | Bearer JWT | `UserResponse` / 200 |
| Public; `/api/v1/invitations` | `POST /preview` | `TokenRequest` | `InvitationPreviewResponse` / 200 |
|  | `POST /accept` | `AcceptInvitationRequest` | `ResponseEntity<LoginResponse>` / 200 |
| ADMIN; `/api/v1/admin/churches` | `POST` | `ChurchRequest` | `ResponseEntity<ChurchResponse>` / 201 |
|  | `GET` | `q`, `page`, `size` | `PageResponse<ChurchResponse>` / 200 |
|  | `GET /{id}` | `id` path | `ChurchResponse` / 200 |
|  | `PUT /{id}` | `id`, `ChurchRequest` | `ChurchResponse` / 200 |
| ADMIN; `/api/v1/admin/invitations` | `POST` | `PastorInvitationRequest` | `ResponseEntity<InvitationResponse>` / 201 |
|  | `GET` | `page`, `size` | `PageResponse<InvitationResponse>` / 200 |
|  | `DELETE /{id}` | `id` path | `ResponseEntity<Void>` / 204 |
| ADMIN; `/api/v1/admin/pastors` | `GET` | `q`, `page`, `size` | `PageResponse<PastorResponse>` / 200 |
|  | `GET /{id}` | `id` path | `PastorResponse` / 200 |
|  | `PATCH /{id}/status` | `id`, `PastorStatusRequest` | `PastorResponse` / 200 |
| ADMIN; `/api/v1/admin/leaders` | `GET` | `q`, `page`, `size` | `PageResponse<LeaderResponse>` / 200 |
|  | `PATCH /{id}/status` | `id`, `UserStatusRequest` | `LeaderResponse` / 200 |
| PASTOR; `/api/v1/pastor` | `POST /invitations` | `LeaderInvitationRequest` | `ResponseEntity<InvitationResponse>` / 201 |
|  | `GET /invitations` | `page`, `size` | `PageResponse<InvitationResponse>` / 200 |
|  | `DELETE /invitations/{id}` | `id` path | `ResponseEntity<Void>` / 204 |
|  | `GET /leaders` | `q`, `page`, `size` | `PageResponse<LeaderResponse>` / 200 |
|  | `GET /leaders/{id}` | `id` path | `LeaderResponse` / 200 |
|  | `PATCH /leaders/{id}/status` | `id`, `UserStatusRequest` | `LeaderResponse` / 200 |
| LEADER; `/api/v1/leader/rooms` | `POST` | `RoomRequest` | `ResponseEntity<RoomResponse>` / 201 |
|  | `GET` | `q`, `status`, `page`, `size` | `PageResponse<RoomResponse>` / 200 |
|  | `GET /{id}` | `id` path | `RoomResponse` / 200 |
|  | `PUT /{id}` | `id`, `RoomRequest` | `RoomResponse` / 200 |
|  | `PATCH /{id}/status` | `id`, `RoomStatusRequest` | `RoomResponse` / 200 |
| LEADER; `/api/v1/leader/rooms/{roomId}/members` | `GET` | `roomId` path | `List<RoomMemberResponse>` / 200 |
|  | `GET /available` | `roomId`, `q`, `page`, `size` | `PageResponse<AvailableMemberResponse>` / 200 |
|  | `POST` | `roomId`, `AddMemberRequest` | `ResponseEntity<RoomMemberResponse>` / 201 |
| LEADER; `/api/v1/leader/rooms/{roomId}/invitations` | `POST` | `roomId`, `MemberInvitationRequest` | `ResponseEntity<InvitationResponse>` / 201 |
|  | `GET` | `roomId`, `page`, `size` | `PageResponse<InvitationResponse>` / 200 |
|  | `DELETE /{id}` | `roomId`, `id` path | `ResponseEntity<Void>` / 204 |

`GET /actuator/health` is configured public by the security filter. No other Actuator endpoint is listed as public. There is no production `/api/v1/probe/{area}` controller; the probe exists in test source only.

## 4. API endpoint catalog

The catalog below uses the route contracts above. The global error mapper applies where the corresponding condition can occur; it does not mean every endpoint emits every status.

| Method | Path | Role required | Request body | Success | Mapped error statuses (as applicable) |
|---|---|---|---|---:|---|
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | 200 | 400, 401, 500 |
| POST | `/api/v1/auth/refresh` | Public | Refresh cookie | 200 | 401, 403, 500 |
| POST | `/api/v1/auth/logout` | Public | Refresh cookie | 204 | 204 on logout |
| GET | `/api/v1/auth/me` | Authenticated, any role | None | 200 | 401 |
| POST | `/api/v1/invitations/preview` | Public | `TokenRequest` | 200 | 400, 410, 500 |
| POST | `/api/v1/invitations/accept` | Public | `AcceptInvitationRequest` | 200 | 400, 409, 410, 422, 500 |
| POST | `/api/v1/admin/churches` | ADMIN | `ChurchRequest` | 201 | 400, 401, 403, 409, 500 |
| GET | `/api/v1/admin/churches` | ADMIN | Query `q,page,size` | 200 | 400, 401, 403, 500 |
| GET | `/api/v1/admin/churches/{id}` | ADMIN | Path `id` | 200 | 400, 401, 403, 404, 500 |
| PUT | `/api/v1/admin/churches/{id}` | ADMIN | `ChurchRequest` | 200 | 400, 401, 403, 404, 409, 500 |
| POST | `/api/v1/admin/invitations` | ADMIN | `PastorInvitationRequest` | 201 | 400, 401, 403, 404, 409, 500 |
| GET | `/api/v1/admin/invitations` | ADMIN | Query `page,size` | 200 | 400, 401, 403, 500 |
| DELETE | `/api/v1/admin/invitations/{id}` | ADMIN | Path `id` | 204 | 401, 403, 404, 422, 500 |
| GET | `/api/v1/admin/pastors` | ADMIN | Query `q,page,size` | 200 | 400, 401, 403, 500 |
| GET | `/api/v1/admin/pastors/{id}` | ADMIN | Path `id` | 200 | 401, 403, 404, 500 |
| PATCH | `/api/v1/admin/pastors/{id}/status` | ADMIN | `PastorStatusRequest` | 200 | 400, 401, 403, 404, 422, 500 |
| GET | `/api/v1/admin/leaders` | ADMIN | Query `q,page,size` | 200 | 400, 401, 403, 500 |
| PATCH | `/api/v1/admin/leaders/{id}/status` | ADMIN | `UserStatusRequest` | 200 | 400, 401, 403, 404, 422, 500 |
| POST | `/api/v1/pastor/invitations` | PASTOR | `LeaderInvitationRequest` | 201 | 400, 401, 403, 404, 409, 422, 500 |
| GET | `/api/v1/pastor/invitations` | PASTOR | Query `page,size` | 200 | 400, 401, 403, 500 |
| DELETE | `/api/v1/pastor/invitations/{id}` | PASTOR | Path `id` | 204 | 401, 403, 404, 422, 500 |
| GET | `/api/v1/pastor/leaders` | PASTOR | Query `q,page,size` | 200 | 400, 401, 403, 500 |
| GET | `/api/v1/pastor/leaders/{id}` | PASTOR | Path `id` | 200 | 401, 403, 404, 500 |
| PATCH | `/api/v1/pastor/leaders/{id}/status` | PASTOR | `UserStatusRequest` | 200 | 400, 401, 403, 404, 422, 500 |
| POST | `/api/v1/leader/rooms` | LEADER | `RoomRequest` | 201 | 400, 401, 403, 409, 422, 500 |
| GET | `/api/v1/leader/rooms` | LEADER | Query `q,status,page,size` | 200 | 400, 401, 403, 500 |
| GET | `/api/v1/leader/rooms/{id}` | LEADER | Path `id` | 200 | 401, 403, 404, 500 |
| PUT | `/api/v1/leader/rooms/{id}` | LEADER | `RoomRequest` | 200 | 400, 401, 403, 404, 409, 422, 500 |
| PATCH | `/api/v1/leader/rooms/{id}/status` | LEADER | `RoomStatusRequest` | 200 | 400, 401, 403, 404, 409, 422, 500 |
| GET | `/api/v1/leader/rooms/{roomId}/members` | LEADER | Path `roomId` | 200 | 401, 403, 404 |
| GET | `/api/v1/leader/rooms/{roomId}/members/available` | LEADER | Query `q,page,size` | 200 | 400, 401, 403, 404 |
| POST | `/api/v1/leader/rooms/{roomId}/members` | LEADER | `AddMemberRequest` | 201 | 400, 401, 403, 404, 409, 422 |
| POST | `/api/v1/leader/rooms/{roomId}/invitations` | LEADER | `MemberInvitationRequest` | 201 | 400, 401, 403, 404, 409, 410, 422 |
| GET | `/api/v1/leader/rooms/{roomId}/invitations` | LEADER | Query `page,size` | 200 | 400, 401, 403, 404 |
| DELETE | `/api/v1/leader/rooms/{roomId}/invitations/{id}` | LEADER | Path `roomId,id` | 204 | 401, 403, 404, 422 |
| GET | `/actuator/health` | Public | None | 200 | Actuator-dependent |

Global exception mapping: validation/unreadable body/type mismatch 400; invalid credentials 401; inactive/access denied 403; not found 404; duplicate/integrity conflict 409; invalid invitation 410; business rule 422; unexpected exception 500. The HTTP response is `ProblemDetail`-style JSON with `title`, `status`, `detail`, `instance`; validation adds `errors` keyed by field. Security 401 adds `WWW-Authenticate: Bearer`.

## 5. Security

- **Roles:** `ADMIN`, `PASTOR`, `LEADER`, `MEMBER`; JWT roles are converted to `ROLE_*` authorities.
- **JWT:** HS256 HMAC signing; issuer `cells`; subject is user ID; claims include role name in `roles`, `iat`, `exp`; access TTL 15 minutes; key must be at least 32 bytes. No secrets/tokens are included here. The base YAML's masked fallback is shorter than that minimum; the local-profile YAML has a separate fallback. If the local-profile file is not loaded, JWT key construction may fail.
- **Refresh:** 32 random bytes, URL-safe Base64 without padding; only SHA-256 hash stored; UUID family; seven-day TTL. Rotation invalidates the submitted token; reuse revokes the family; logout revokes family. Cookie `cells_refresh`, HttpOnly, SameSite Strict, path `/api/v1/auth`, Secure configurable, max-age from TTL; clear sets zero max-age. Access/user JSON is returned separately.
- **Password storage:** BCrypt via `PasswordConfig`.
- **Filter chain:** stateless; CSRF disabled; unauthenticated by default except login/refresh/logout, invitation preview/accept, and `/actuator/health`; method security enabled. `/auth/me` requires authentication.
- **CORS, rate limits, login lockout:** NOT FOUND in backend source/config.
- **`EndpointSecurityGuardTest` allow-list:** `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `/api/v1/auth/me`, `/api/v1/probe/any`; the test requires application-controller routes to have class/method `@PreAuthorize` or explicit allow-list membership. Note `SecurityConfig` still requires authentication for `/auth/me`; the guard list is an annotation-coverage exception, not the public-path list.

## 6. Business rules already enforced in code

| Rule | Enforcement | Test evidence |
|---|---|---|
| Unknown email and wrong password have indistinguishable login responses. | `AuthService.login` | `AuthFlowTest.wrongPasswordAndUnknownEmailGiveIdenticalResponses` |
| Disabled accounts cannot log in or refresh; disabling invalidates refresh family. | `AuthService.login`, `AuthService.refresh`, `PastorService.setStatus`, `LeaderService.setStatus*` | `AuthFlowTest.disabledUserCannotLogIn`, `AuthFlowTest.refreshFailsAfterUserIsDisabled`, `PastorApiTest.disablingAPastorBlocksLoginAndEnablingRestoresIt`, `OrganizationChainTest.disablingALeaderEndsTheirSessionAtTheNextRefresh`, `OrganizationChainTest.disablingAPastorEndsTheirSessionAtTheNextRefresh` |
| Refresh rotation is single-use; token reuse revokes its family. | `RefreshTokenService.rotate`, `AuthService.refresh` | `AuthFlowTest.refreshRotatesTheTokenAndIssuesANewAccessToken`, `AuthFlowTest.reusingAnOldRefreshTokenRevokesTheWholeFamily` |
| Invitation tokens are hashed, time-limited, one-time, and acceptance is serialized. | `InvitationService.invite*`, `preview`, `accept`; `InvitationRepository.findByTokenHashForUpdate` | `InvitationApiTest.adminCreatesAnInvitationAndOnlyTheHashIsStored`, `InvitationApiTest.acceptingCreatesTheAccountLogsTheUserInAndIsSingleUse`, `InvitationApiTest.expiredInvitationCannotBeUsed`, `InvitationApiTest.concurrentAcceptsOfOneLinkSucceedOnlyOnce` |
| A newer pending invitation replaces matching prior pending invite(s) within inviter/scope; other inviter/room scope is preserved. | `InvitationService.invite*`, repository revoke queries | `InvitationApiTest.aNewInvitationReplacesTheOlderPendingOne`, `LeaderApiTest.aPastorInvitationDoesNotCancelAnAdminInvitationForTheSameEmail`, `MemberEnrollmentApiTest.aNewInvitationForTheSameRoomReplacesTheOldOne`, `MemberEnrollmentApiTest.invitationsIntoDifferentRoomsDoNotReplaceEachOther` |
| Admins choose a church for pastors; pastors invite leaders only into their own church; leaders inherit room church from the leader, not the request. | `InvitationService.invitePastor`, `inviteLeader`, `inviteMember` | `LeaderApiTest.pastorInvitesALeaderIntoTheirOwnChurchEvenIfTheyAskForAnotherOne`, `MemberEnrollmentApiTest.aLeaderInvitesANewPersonIntoTheirRoom` |
| A pastor sees/manages only assigned leaders and their own invitations; other pastors’ records are concealed as 404. | `LeaderService.listForPastor/getForPastor/setStatusForPastor`, `InvitationService` scoped list/revoke methods | `LeaderApiTest.aPastorSeesOnlyTheirOwnLeaders`, `LeaderApiTest.anotherPastorsLeaderLooksLikeItDoesNotExist`, `LeaderApiTest.aPastorSeesAndRevokesOnlyTheirOwnInvitations`, `OrganizationChainTest.aPastorCannotReachAnotherPastorsLeaderEvenByGuessingTheId` |
| Users cannot manually be changed to `PENDING`. | `PastorService.setStatus`, `LeaderService.setStatus*` | `PastorApiTest.pendingIsNotAValidManualStatusAndUnknownValuesAreRejected`, `LeaderApiTest.pendingIsNotAValidManualStatus` |
| Church names are case-insensitively unique; create/rename names are trimmed. | `ChurchService.create/rename`, unique lower-name index | `ChurchApiTest.duplicateNameIgnoringCaseAndSpacesIsRejected`, `ChurchApiTest.renameToAnotherChurchesNameIsRejected` |
| Room ownership is leader-scoped; leaders create rooms only in their church; active room names are unique per leader, case-insensitively. | `RoomService`, `RoomRepository`, migration index | `RoomApiTest.aLeaderCreatesARoomInTheirOwnChurchEvenIfTheyAskForAnotherOne`, `RoomApiTest.namesAreTrimmedAndDuplicatesAreRejectedIgnoringCase`, `RoomApiTest.differentLeadersMayUseTheSameName`, `RoomApiTest.anotherLeadersRoomLooksLikeItDoesNotExist` |
| Archived rooms cannot be renamed/enrolled into; restoring a conflicting name fails; archive frees an active name. | `Room.rename/archive/restore`, `RoomService.setStatus` | `RoomApiTest.archivingKeepsTheRoomAndFreesItsName`, `RoomApiTest.anArchivedRoomMustBeRestoredBeforeItIsRenamed`, `RoomApiTest.restoringFailsWhileAnotherActiveRoomHasTheName`, `MemberEnrollmentApiTest.anArchivedRoomRefusesBothWays` |
| Available members are active users in the room’s church not already active in that room; wrong-church/out-of-scope data is concealed. | `MemberRepository.searchAvailable`, `RoomMemberService` | `MemberEnrollmentApiTest.availableMembersAreActiveChurchMembersNotYetInTheRoom`, `MemberEnrollmentApiTest.searchNarrowsTheListAndAMemberOfAnotherRoomStaysAvailable`, `MemberEnrollmentApiTest.aMemberOfAnotherChurchLooksUnknown` |
| Active membership is unique per room/member and participation numbers are unique/positive; enrollment assigns next number under room lock; removed memberships free the active number/member slot. | `RoomMemberService.enroll`, `RoomMemberRepository`, migration partial indexes/check | `RoomMemberApiTest.numbersStartAtOneAndGrowByOne`, `RoomMemberApiTest.concurrentEnrollmentsGetDistinctConsecutiveNumbers`, `RoomMemberApiTest.theDatabaseRejectsTwoActiveMembersWithTheSameNumber`, `RoomMemberApiTest.theDatabaseRejectsTheSameMemberTwiceInOneRoom`, `RoomMemberApiTest.aRemovedMembershipNoLongerBlocksItsNumberOrItsMember` |
| Room invitations only apply to their room and archived room invitations cannot be accepted. | `InvitationService.inviteMember/accept`, V13 check | `MemberEnrollmentApiTest.invitationsIntoDifferentRoomsDoNotReplaceEachOther`, `MemberEnrollmentApiTest.anInvitationIntoAnArchivedRoomCannotBeUsed` |

## 7. Tests

### Backend

| Test class | Count | Test coverage (method names from source) |
|---|---:|---|
| `BackendApplicationTests` | 1 | `contextLoads` — application context starts. |
| `OrganizationChainTest` | 7 | `everyoneEndsUpWithTheirOwnRole`; `theAdministratorSeesTheWholeHierarchy`; `eachPastorSeesOnlyTheirOwnLeaders`; `aPastorCannotReachAnotherPastorsLeaderEvenByGuessingTheId`; `everyoneStaysInTheirOwnArea`; `disablingALeaderEndsTheirSessionAtTheNextRefresh`; `disablingAPastorEndsTheirSessionAtTheNextRefresh` — roles, hierarchy/scoping, disabled-account refresh. |
| `AuthFlowTest` | 12 | `loginReturnsTokenUserAndSecureRefreshCookie`; `wrongPasswordAndUnknownEmailGiveIdenticalResponses`; `loginWithInvalidBodyReturnsFieldErrors`; `malformedJsonReturns400`; `disabledUserCannotLogIn`; `meReturnsCurrentUserWithValidToken`; `meWithoutTokenReturns401`; `refreshRotatesTheTokenAndIssuesANewAccessToken`; `reusingAnOldRefreshTokenRevokesTheWholeFamily`; `refreshWithoutCookieReturns401`; `refreshFailsAfterUserIsDisabled`; `logoutRevokesTheRefreshToken`. |
| `ChurchApiTest` | 9 | `adminCreatesAChurch`; `duplicateNameIgnoringCaseAndSpacesIsRejected`; `blankNameIsRejectedWithFieldErrors`; `nonAdminsAreForbidden`; `noTokenReturns401`; `listIsSearchableAndPageSizeIsCapped`; `getUnknownChurchReturns404`; `renameWorksAndCanChangeOnlyTheCaseOfItsOwnName`; `renameToAnotherChurchesNameIsRejected`. |
| `InvitationApiTest` | 14 | `adminCreatesAnInvitationAndOnlyTheHashIsStored`; `nonAdminsCannotManageInvitations`; `unknownChurchReturns404`; `existingUserEmailIsRejected`; `invalidBodyReturnsFieldErrors`; `aNewInvitationReplacesTheOlderPendingOne`; `previewShowsDetailsAndNeverEchoesTheToken`; `unknownTokenReturns410`; `acceptingCreatesTheAccountLogsTheUserInAndIsSingleUse`; `expiredInvitationCannotBeUsed`; `shortPasswordIsRejectedAndTheLinkStaysUsable`; `concurrentAcceptsOfOneLinkSucceedOnlyOnce`; `adminCanListAndRevoke`; `anAcceptedInvitationCannotBeRevoked`. |
| `LeaderApiTest` | 12 | `pastorInvitesALeaderIntoTheirOwnChurchEvenIfTheyAskForAnotherOne`; `invitingAnExistingUserIsRejected`; `aPastorWithoutAChurchCannotInvite`; `aPastorInvitationDoesNotCancelAnAdminInvitationForTheSameEmail`; `aPastorSeesOnlyTheirOwnLeaders`; `anotherPastorsLeaderLooksLikeItDoesNotExist`; `aPastorSeesAndRevokesOnlyTheirOwnInvitations`; `aPastorCanDisableAndEnableTheirLeader`; `pendingIsNotAValidManualStatus`; `onlyPastorsReachThePastorArea`; `onlyAdminsReachTheAdminLeaderList`; `adminSeesEveryLeaderAndCanDisableOne`. |
| `PastorApiTest` | 6 | `adminListsPastorsWithTheirChurch`; `searchMatchesNameEmailAndChurch`; `nonAdminsAreForbidden`; `getOneAndUnknownIds`; `disablingAPastorBlocksLoginAndEnablingRestoresIt`; `pendingIsNotAValidManualStatusAndUnknownValuesAreRejected`. |
| `MemberEnrollmentApiTest` | 21 | `availableMembersAreActiveChurchMembersNotYetInTheRoom`; `searchNarrowsTheListAndAMemberOfAnotherRoomStaysAvailable`; `aRemovedMemberIsAvailableAgain`; `theAvailableListOfAnotherLeadersRoomIs404`; `anExistingMemberIsAddedWithTheNextNumber`; `theSameMemberCannotBeAddedTwice`; `aMemberOfAnotherChurchLooksUnknown`; `unknownAndMissingMemberIdsAreRejected`; `aDisabledMemberCannotBeAdded`; `anotherLeadersRoomCannotBeChanged`; `anArchivedRoomRefusesBothWays`; `aLeaderInvitesANewPersonIntoTheirRoom`; `existingAccountsAreNeverInvitedAndOnlyOwnChurchMembersAreDescribed`; `aNewInvitationForTheSameRoomReplacesTheOldOne`; `invitationsIntoDifferentRoomsDoNotReplaceEachOther`; `anInvalidInvitationBodyReturnsFieldErrors`; `anInvitedPersonJoinsTheRoomWithTheNextNumber`; `anInvitationIntoAnArchivedRoomCannotBeUsed`; `twoInvitedPeopleAcceptingAtOnceGetDistinctNumbers`; `aLeaderSeesAndRevokesOnlyTheirRoomsInvitations`; `onlyLeadersCanUseTheseEndpoints`. |
| `RoomApiTest` | 16 | `aLeaderCreatesARoomInTheirOwnChurchEvenIfTheyAskForAnotherOne`; `namesAreTrimmedAndDuplicatesAreRejectedIgnoringCase`; `differentLeadersMayUseTheSameName`; `blankAndTooLongNamesAreRejected`; `aLeaderUserWithoutALeaderRecordCannotCreateRooms`; `aLeaderSeesOnlyTheirOwnRoomsSortedByName`; `searchAndStatusFilterWork`; `badParameterValuesAnswer400`; `anotherLeadersRoomLooksLikeItDoesNotExist`; `renameWorksAndMayChangeOnlyTheCaseOfItsOwnName`; `renamingToAnotherOfMyRoomsNamesIsRejected`; `archivingKeepsTheRoomAndFreesItsName`; `anArchivedRoomMustBeRestoredBeforeItIsRenamed`; `restoringFailsWhileAnotherActiveRoomHasTheName`; `anUnknownStatusIsRejected`; `onlyLeadersReachTheRoomEndpoints`. |
| `RoomMemberApiTest` | 19 | `theRosterListsActiveMembersInParticipationOrder`; `anEmptyRoomHasAnEmptyRoster`; `removedMembersAreLeftOutOfTheRoster`; `theRoomListShowsTheActiveMemberCount`; `numbersStartAtOneAndGrowByOne`; `theSameMemberHasAnIndependentNumberInEachRoom`; `concurrentEnrollmentsGetDistinctConsecutiveNumbers`; `aMemberCannotJoinTheSameRoomTwice`; `anArchivedRoomRefusesNewMembers`; `aMemberOfAnotherChurchLooksLikeItDoesNotExist`; `aDisabledAccountIsRejected`; `aUserWhoIsNotAMemberIsRejected`; `onlyTheRoomsOwnerCanEnroll`; `theDatabaseRejectsTwoActiveMembersWithTheSameNumber`; `theDatabaseRejectsTheSameMemberTwiceInOneRoom`; `aRemovedMembershipNoLongerBlocksItsNumberOrItsMember`; `anotherLeadersRoomRosterLooksLikeItDoesNotExist`; `unknownAndMalformedRoomIdsAreHandled`; `onlyLeadersCanSeeARoster`. |
| `EndpointSecurityGuardTest` | 1 | `everyEndpointHasARoleRuleOrIsExplicitlyAllowListed` — controller endpoint annotation/allow-list coverage. |
| `RoleAccessTest` | 6 | `eachRoleReachesOnlyItsOwnArea`; `forbiddenResponseIsJson`; `noTokenReturns401OnEveryArea`; `tamperedTokenIsRejected`; `expiredTokenIsRejected`; `tokenWithWrongIssuerIsRejected`. |

Backend total: **124 annotated test methods**; parameterized methods counted once, not once per invocation. `IntegrationTestBase` uses `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, a static shared PostgreSQL Testcontainer `postgres:18`, dynamic datasource/JWT settings, and table cleanup between tests. `BackendApplicationTests` also imports `TestcontainersConfiguration`, which declares a `postgres:latest` service-connected container.

### Frontend

| Test file | Count | Test coverage (method names from source) |
|---|---:|---|
| `src/api/__tests__/http.spec.js` | 6 | `sends the bearer token`; `refreshes once on a 401 and retries with the new token`; `ends the session when the refresh fails`; `retries only once, so it can never loop`; `does not refresh on a 403`; `turns a network failure into a friendly error`. |
| `src/composables/__tests__/usePagedList.spec.js` | 4 | `loads the first page on mount`; `debounces searching and goes back to the first page`; `ignores a slow response that arrives after a newer one`; `shows an error and recovers on reload`. |
| `src/router/__tests__/guards.spec.js` | 33 | Nine named cases cover logged-out/unknown/public/invitation routes and nested role areas; 24 generated cases cover each role reaching its own home, `/login` and `/` redirects, and redirects away from other roles’ areas. |
| `src/stores/__tests__/auth.spec.js` | 9 | Covers initial logged-out state, login/session storage, rejected login, concurrent refresh sharing, later refresh, refresh failure, restore at startup, logout on network failure, and invitation acceptance. |
| `src/views/__tests__/AcceptInvitationView.spec.js` | 11 | Covers fragment token preview/removal, invalid/expired/offline links, preview retry, password validation, account acceptance/role redirect, and server field errors. |
| `src/views/admin/__tests__/ChurchManagementView.spec.js` | 7 | Covers list/empty/error states, create/trim/validation/duplicate, and rename. |
| `src/views/admin/__tests__/PastorManagementView.spec.js` | 5 | Covers pastor list/status, invitation/link refresh, incomplete form, and revoke. |
| `src/views/pastor/__tests__/MyLeadersView.spec.js` | 9 | Covers leader list/empty, invite payload/link/refresh/validation/errors, status change, and revoke. |

Frontend total: **84 tests**. No feature tests were found for dashboard placeholder pages, member room workflows in the UI, devotion, daily exemption, submission, reflection/EXP, or analytics screens.

## 8. Frontend

### Router

All page components are lazy-loaded in `frontend/src/router/index.js`; child routes inherit their layout's role metadata.

| Route | Name / component | Access |
|---|---|---|
| `/login` | `login` / `LoginView.vue` | Public; authenticated users are redirected to role home. |
| `/admin` | `AdminLayout.vue` + `AdminDashboardView.vue` | ADMIN |
| `/admin/churches` | `admin-churches` / `admin/ChurchManagementView.vue` | ADMIN |
| `/admin/pastors` | `admin-pastors` / `admin/PastorManagementView.vue` | ADMIN |
| `/admin/leaders` | `admin-leaders` / `admin/LeaderManagementView.vue` | ADMIN |
| `/pastor` | `PastorLayout.vue` + `PastorDashboardView.vue` | PASTOR |
| `/pastor/leaders` | `pastor-leaders` / `pastor/MyLeadersView.vue` | PASTOR |
| `/leader` | `LeaderLayout.vue` + `LeaderDashboardView.vue` | LEADER |
| `/member` | `MemberLayout.vue` + `MemberDashboardView.vue` | MEMBER |
| `/accept-invitation` | `accept-invitation` / `AcceptInvitationView.vue` | Public |
| `/` | Redirect to `login` | Auth guard sends authenticated user to role home. |
| `/:pathMatch(.*)*` | Redirect to `login` | Same guard behavior. |

Guard sends unauthenticated private-route visits to login with `query.redirect=to.fullPath`; wrong-role users go to their own `ROLE_HOME`.

### Pinia stores

| Store | State | Getters | Actions |
|---|---|---|---|
| `useAuthStore` (`auth`) | `accessToken`, `user` (token memory-only). | `isAuthenticated`, `role`. | `login(email,password)`, `acceptInvitation(token,fullName,password)`, `refresh()`, `restoreSession()`, `logout()`, `clearSession()`. Concurrent refreshes share `refreshPromise`; failed refresh clears session. |
| `useCounterStore` (`counter`) | `count`. | `doubleCount`. | `increment()`. No use found in `src`. |

### API/HTTP layer

- Base request prefix is `/api/v1`; Vite proxies `/api` to `http://localhost:8080`.
- `rawRequest` sets `Accept: application/json`, JSON body/content type when supplied, optional `Authorization: Bearer ...`, and `credentials: 'same-origin'`.
- Authenticated `request` supplies the in-memory access token, refreshes once on 401 and retries once; 403 does not refresh; failed refresh clears session/redirects to login.
- Startup awaits `restoreSession()` before router installation/mount. Refresh cookie is browser-managed; token not stored in local/session storage.
- `ApiError` exposes `status`, `problem`, `fieldErrors`; 204 becomes null; network failures are status 0.

| API wrapper | Exports and backend paths |
|---|---|
| `api/auth.js` | `login` POST `/auth/login`; `refresh` POST `/auth/refresh`; `logout` POST `/auth/logout`; `me` GET `/auth/me`. |
| `api/invitations.js` | `previewInvitation` POST `/invitations/preview`; `acceptInvitation` POST `/invitations/accept`. |
| `api/churches.js` | List GET `/admin/churches`; create POST `/admin/churches`; rename PUT `/admin/churches/{id}`. |
| `api/pastors.js` | List GET `/admin/pastors`; status PATCH `/admin/pastors/{id}/status`. |
| `api/leaders.js` | List GET `/admin/leaders`; status PATCH `/admin/leaders/{id}/status`. |
| `api/adminInvitations.js` | List GET `/admin/invitations`; invite POST `/admin/invitations`; revoke DELETE `/admin/invitations/{id}`. |
| `api/pastorArea.js` | Leaders GET/PATCH `/pastor/leaders[/{id}/status]`; invitations GET/POST/DELETE `/pastor/invitations[/{id}]`. |
| `api/query.js` | `withQuery` trims values, skips undefined/null/empty, preserves page 0. |

No frontend API wrappers for room/member roster CRUD were found.

### Pages, layouts, components

| Name | Role | Purpose / calls |
|---|---|---|
| `LoginView.vue` | Public | Sign-in, safe local redirect; POST `/auth/login`. |
| `AcceptInvitationView.vue` | Public | Preview/accept invitation from URL fragment, password checks; POST `/invitations/preview`, `/invitations/accept`. |
| `AdminDashboardView.vue` | ADMIN | Placeholder dashboard; no API. |
| `admin/ChurchManagementView.vue` | ADMIN | Search/list/create/rename churches; `/admin/churches` GET/POST/PUT. |
| `admin/PastorManagementView.vue` | ADMIN | List/status pastors, manage pastor invitations, load church choices; `/admin/pastors`, `/admin/invitations`, `/admin/churches`. |
| `admin/LeaderManagementView.vue` | ADMIN | Search/list leaders, status; `/admin/leaders`. |
| `PastorDashboardView.vue` | PASTOR | Placeholder dashboard; no API. |
| `pastor/MyLeadersView.vue` | PASTOR | Own leaders/status/invitations; `/pastor/leaders`, `/pastor/invitations`. |
| `LeaderDashboardView.vue` | LEADER | Placeholder dashboard; no API. |
| `MemberDashboardView.vue` | MEMBER | Placeholder dashboard; no API. |
| `HomeView.vue` | No route/role | Temporary API role-check probe; calls `/probe/{area}`; not registered in router. |
| Role layouts | ADMIN/PASTOR/LEADER/MEMBER | Role navigation and shared `AppShell`; logout through `POST /auth/logout`. |
| `AppShell.vue` | Shared | Responsive navigation, user/role badge, logout. |
| `BaseDialog.vue`, `ConfirmDialog.vue`, `StatusConfirmDialog.vue` | Shared | Native dialog wrapper, generic confirmation, enable/disable confirmation. |
| `InviteLinkPanel.vue` | Shared | Shows/copies one-time invitation link. |
| `ListState.vue`, `PaginationBar.vue` | Shared | Loading/error/empty/list state and pagination. |
| `StatusPill.vue`, `TextField.vue`, `buttonClasses.js` | Shared | Status display, labeled input/errors, button class constants. |
| `PlaceholderDashboard.vue` | Shared | Welcome and “Coming next” phase list. |
| `usePagedList.js`, `useStatusToggle.js` | Composable | Paginated search/loading state; confirmed status changes. |

`PastorManagementView.vue` fetches churches with `size:100`; its source comment notes a searchable picker is needed for more than 100 churches.

### Design tokens and comparison

`frontend/src/assets/main.css` defines:

| Token | Value |
|---|---|
| `navy-950`, `navy-900`, `navy-700`, `navy-500` | `#071433`, `#0e2250`, `#1b3a78`, `#3d5fa5` |
| `gold-500`, `gold-700`, `gold-100`, `gold-50` | `#d4af37`, `#8a6d12`, `#fbefc4`, `#fbf7e8` |
| `app-bg`, `line`, `line-input`, `track`, `muted` | `#f4f6fb`, `#dce3f2`, `#c5cfe6`, `#e3e8f4`, `#47557a` |
| `on-navy`, `on-navy-soft` | `#e6ecfa`, `#c9d4f0` |
| `font-heading`, `font-sans` | `"Montserrat", "Inter", system-ui, sans-serif`; `"Inter", system-ui, sans-serif` |

Also `.bg-brand-backdrop` and `.bg-room-card`. `base.css` is the Vue starter stylesheet and is not imported; `main.js` imports `main.css`. A CELLS Design System Guide is **NOT FOUND**, so token mismatches are **NOT ASSESSED**; the root README's claim that components follow the guide cannot be independently checked.

## 9. Conventions

| Convention | Evidence |
|---|---|
| Backend package layout | Domain packages under `com.cells.backend`; two file/package mismatches: `leader/LeaderInvitationRequest.java` declares `com.cells.backend.invitation`; `member/RoomMember*.java` declare `com.cells.backend.room`. |
| Backend class names | `*Controller`, `*Service`, `*Repository`, request/response records, exception suffixes; source does not establish a separate architecture document. |
| DTO mapping | Static `from(...)` factories on `ChurchResponse`, `InvitationResponse`, `UserResponse`, `PageResponse`; pastor/leader/member listings use repository projection records. |
| Pagination | `PageResponse<T>(content,page,size,totalElements,totalPages)`; default page 0/size 20, capped size 100. |
| Error format | Problem-detail style JSON (`title`, `status`, `detail`, `instance`); validation adds `errors` by field. |
| Migrations | `V<number>__description.sql`; versions V1–V13, with V1/V4/V6/V9 empty and duplicate descriptions at V4/V5 and V6/V7. |
| Frontend | Vue Composition API / `<script setup>`; lazy route components; API modules by backend area; tests under `__tests__` with `.spec.js`; Vitest/jsdom. |
| Frontend styles | Tailwind CSS v4 theme tokens in `main.css`; shared components/composables for dialogs, lists, and status toggles. |
| API query parameters | `withQuery` normalizes whitespace and omits blank values while retaining `page=0`. |

## 10. Gaps and risks

### Roadmap status (based only on source labels)

No phase-0-to-10 roadmap definitions or exit criteria were found. The root README labels organization design Phase 3; `frontend/README.md` labels auth Phase 2; dashboards include placeholders labeled phases 4–8. These statuses are evidence-based and not a claim that undocumented acceptance criteria are complete.

| Phase | Status | Evidence |
|---:|---|---|
| 0 | UNCLEAR | Phase scope NOT FOUND. |
| 1 | UNCLEAR | Phase scope NOT FOUND. |
| 2 | DONE for the documented auth slice | Frontend README labels auth Phase 2; login/refresh/logout, token guards, and auth tests exist. |
| 3 | PARTIAL | README documents invitation-only organization hierarchy; relevant backend APIs/tests exist, but no phase exit criteria found. |
| 4 | PARTIAL | Room/member backend APIs and integration tests exist; UI dashboard still labels room/member tasks “Coming next.” |
| 5 | NOT STARTED | Devotion/history items appear only as placeholders; no implementation/test found. |
| 6 | NOT STARTED | Exemption controls/member submissions appear only as placeholders; no implementation/test found. |
| 7 | NOT STARTED | Reflection/experience statistics appear only as placeholders; no implementation/test found. |
| 8 | PARTIAL | Room/member APIs exist, but monitoring, analytics, and report pages remain placeholders. |
| 9 | UNCLEAR | Phase scope NOT FOUND. |
| 10 | UNCLEAR | Phase scope NOT FOUND. |

### Product-documentation comparisons

Root README and frontend README were the product/project documents found. No Design System Guide or detailed roadmap/spec for participation rules, exemptions, EXP, naming `Church – Room`, or the leader subtitle was found; therefore conformance is **UNCLEAR**. Code does implement integer `participation_number` ordering/enrollment, but no devotion, daily exemption, submission, reflection, or EXP feature was found. The placeholder labels are not evidence that these rules are implemented.

### Other code-based risks / gaps

- `RoomMember.markRemoved(Instant)` and tests cover removed history, but there is no production member removal controller/endpoint.
- `HomeView.vue` is an unrouted temporary probe; backend `/probe/any` is test-only.
- `useCounterStore`, `HomeView`, `auth.me`, `base.css`, and `logo.svg` appear unused from `src`; `main.js` imports `main.css`, not `base.css`.
- Backend external `config/` location depends on Spring Boot config search path/current directory; packaged `application.yaml` only sets app name.
- `V1`, `V4`, `V6`, `V9` are empty migrations. Avoid editing already-applied migrations; add a new version for schema changes.
- No CORS, rate limiting, or login lockout implementation found.
- Invitation lists use `@EntityGraph` for church/room; pastor/leader query projections and room-member queries are explicit. A broader N+1 assessment is **UNCLEAR**; no N+1-focused test was found.
- Exact official phase criteria, product naming/subtitle requirements, and Design System Guide are **NOT FOUND**.

## 11. Postman

**NOT FOUND.** Root `README.md` references `docs/postman/cells.postman_collection.json`, but no `docs/postman/` directory or Postman collection export exists in the repository listing. No request folders/requests can be enumerated.

## Files not fully read or too large to summarize

The inventory is based on actual source reads plus targeted declaration/test-name searches. The following backend files were not fully read (their purposes are limited to the inspected declarations/names):

- `backend/src/main/java/com/cells/backend/BackendApplication.java`
- `backend/src/main/java/com/cells/backend/auth/AuthResult.java`
- `backend/src/main/java/com/cells/backend/auth/LoginResponse.java`
- `backend/src/main/java/com/cells/backend/church/ChurchRequest.java`
- `backend/src/main/java/com/cells/backend/church/ChurchResponse.java`
- `backend/src/main/java/com/cells/backend/config/InvitationProperties.java`
- `backend/src/main/java/com/cells/backend/config/JwtProperties.java`
- `backend/src/main/java/com/cells/backend/config/RefreshProperties.java`
- `backend/src/main/java/com/cells/backend/invitation/InvitationStatus.java`
- `backend/src/main/java/com/cells/backend/invitation/TokenRequest.java`
- `backend/src/main/java/com/cells/backend/leader/LeaderResponse.java`
- `backend/src/main/java/com/cells/backend/leader/UserStatusRequest.java`
- `backend/src/main/java/com/cells/backend/member/AvailableMemberResponse.java`
- `backend/src/main/java/com/cells/backend/pastor/PastorResponse.java`
- `backend/src/main/java/com/cells/backend/pastor/PastorStatusRequest.java`
- `backend/src/main/java/com/cells/backend/room/AddMemberRequest.java`
- `backend/src/main/java/com/cells/backend/room/RoomRequest.java`
- `backend/src/main/java/com/cells/backend/room/RoomStatusRequest.java`
- `backend/src/main/java/com/cells/backend/user/UserResponse.java`
- `backend/src/test/java/com/cells/backend/OrganizationChainTest.java`
- `backend/src/test/java/com/cells/backend/TestBackendApplication.java` (purpose UNCLEAR)
- `backend/src/test/java/com/cells/backend/church/ChurchApiTest.java`
- `backend/src/test/java/com/cells/backend/invitation/InvitationApiTest.java`
- `backend/src/test/java/com/cells/backend/leader/LeaderApiTest.java`
- `backend/src/test/java/com/cells/backend/pastor/PastorApiTest.java`
- `backend/src/test/java/com/cells/backend/probe/RoleProbeController.java`
- `backend/src/test/java/com/cells/backend/room/MemberEnrollmentApiTest.java`
- `backend/src/test/java/com/cells/backend/room/RoomApiTest.java`
- `backend/src/test/java/com/cells/backend/room/RoomMemberApiTest.java`
- `backend/HELP.md`

Backend config and migration files were read; no backend file was reported too large.

Frontend files not fully read:

- `frontend/index.html`
- `frontend/eslint.config.js`
- `frontend/.oxlintrc.json`
- `frontend/.prettierrc.json`
- `frontend/jsconfig.json`
- `frontend/vitest.config.js`
- `frontend/.editorconfig`
- `frontend/.gitattributes`
- `frontend/.gitignore`
- `frontend/.vscode/extensions.json`
- `frontend/.vscode/settings.json`
- `frontend/src/assets/logo.svg`
- `frontend/public/favicon.ico`
- Generated `frontend/dist/` assets (directory collapsed in tree; not inspected)
- `frontend/package-lock.json` (selected package versions inspected; entire lockfile not summarized)

Frontend tests under `src/**/__tests__` were read. The CELLS Design System Guide and Postman export are NOT FOUND.
