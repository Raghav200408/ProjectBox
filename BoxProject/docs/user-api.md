# User CRUD APIs

These APIs follow ProjectS's `entity` → `repository` → `service` → `controller` structure, with Lombok DTOs in `model` and manual entity-to-response mapping in the service. Users and roles use database-generated sequential numeric IDs (BIGINT), mapped to Long in Java. Timestamps use UTC.

| Method | Endpoint | Result |
| --- | --- | --- |
| POST | `/api/users/appOwner` | Create APP_OWNER using AppOwnerRequestDTO, 201 |
| POST | `/api/users/venueOwner` | Create VENUE_OWNER using VenueOwnerRequestDTO, 201 |
| POST | `/api/users/player` | Create PLAYER using PlayerRequestDTO, 201 |
| POST | `/api/users` | Alias for player creation, 201 |
| GET | `/api/users/all` | List non-deleted users, 200 |
| GET | `/api/users/{userId}` | Read a non-deleted user, 200 |
| PUT | `/api/users/appOwner/{userId}` | Update APP_OWNER using AppOwnerRequestDTO, 200 |
| PUT | `/api/users/venueOwner/{userId}` | Update VENUE_OWNER using VenueOwnerRequestDTO, 200 |
| PUT | `/api/users/player/{userId}` | Update PLAYER using PlayerRequestDTO, 200 |
| DELETE | `/api/users/{userId}` | Soft delete a user, 204 |

Create request:

```json
{
  "fullName": "Arjun Kumar",
  "phone": "+919876543210",
  "email": "arjun@example.com",
  "password": "StrongPass123!"
}
```

There are three independent request DTOs: `AppOwnerRequestDTO`, `VenueOwnerRequestDTO`, and `PlayerRequestDTO`. Each contains `fullName`, `phone`, optional `email`, `password`, and optional `status`, and is reused for both creation and updates. There is no shared user request DTO or separate update request DTO. Each service method sets the role corresponding to its create endpoint. Updating through a different role's endpoint returns 404 and never changes the account's role. Venue ownership and access checks are outside this CRUD change.

POST requires `fullName`, `phone`, and `password` and creates an ACTIVE account. The `Create` validation group includes the default constraints and adds the password requirement. PUT requires `fullName` and `phone`; omitting the password preserves the current hash. An omitted or empty email clears the email. An omitted status is preserved; a supplied status may be `ACTIVE` or `BLOCKED`. Use DELETE for deletion.

Passwords require at least 8 characters and at most 72 UTF-8 bytes. They are BCrypt hashed and never returned. Phone numbers are stored with a leading `+`; callers should supply the country code. Email is stored in lowercase. Duplicate contact details return 409; invalid requests return 400; missing or deleted users return 404. Deleted users retain contact uniqueness to preserve historical identity.

## Database setup

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. For a new PostgreSQL database, run `scripts/sql/roles.sql` first, then `scripts/sql/users.sql`. The role script inserts APP_OWNER, VENUE_OWNER, and PLAYER. Both tables use BIGINT identity primary keys; users.role_id references roles.id. Role names and user statuses are strings, with lowercase statuses. Verification timestamp columns are omitted. Hibernate automatic DDL is disabled. These creation scripts do not convert an existing UUID schema.

## Security and testing

Existing Spring Security defaults remain enabled. This change adds no login, public registration, role authorization, or JWT endpoints. Calls require the existing configured Spring Security credentials; mutations also require a valid CSRF token under the default filter chain. Newly created database users are not automatically Spring Security login accounts. Treat these as administrative CRUD endpoints until the application's authentication and role rules are implemented.

Run `./mvnw.cmd test` from the nested BoxProject directory. Integration tests use an isolated H2 database and exercise controller validation, service behavior, JPA persistence, password hashing, duplicates, updates, soft deletion, and the existing authentication boundary. They do not connect to your PostgreSQL database.
