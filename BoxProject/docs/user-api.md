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
  "phone": "9876543210",
  "email": "arjun@example.com",
  "password": "StrongPass123!"
}
```

There are three independent request DTOs: `AppOwnerRequestDTO`, `VenueOwnerRequestDTO`, and `PlayerRequestDTO`. Each contains `fullName`, `phone`, optional `email`, `password`, and optional `status`, and is reused for both creation and updates. There is no shared user request DTO or separate update request DTO. Each service method sets the role corresponding to its create endpoint. Updating through a different role's endpoint returns 404 and never changes the account's role. Venue ownership and access checks are outside this CRUD change.

POST requires `fullName`, `phone`, and `password` and creates an ACTIVE account. The `Create` validation group includes the default constraints and adds the password requirement. PUT requires `fullName` and `phone`; omitting the password preserves the current hash. An omitted or empty email clears the email. An omitted status is preserved; a supplied status may be `ACTIVE` or `BLOCKED`. Use DELETE for deletion.

Passwords require at least 8 characters and at most 72 UTF-8 bytes. They are BCrypt hashed and never returned. Phone numbers are stored as supplied; no country code or + prefix is added or required. Email is stored in lowercase. Duplicate contact details return 409; invalid requests return 400; missing or deleted users return 404. Deleted users retain contact uniqueness to preserve historical identity.

## Database setup

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. For a new PostgreSQL database, run `scripts/sql/roles.sql` first, then `scripts/sql/users.sql`. The role script inserts APP_OWNER, VENUE_OWNER, and PLAYER. Both tables use BIGINT identity primary keys; users.role_id references roles.id. Role names and user statuses are strings, with lowercase statuses. Verification timestamp columns are omitted. Hibernate automatic DDL is disabled. These creation scripts do not convert an existing UUID schema.

## Login and security

Set `JWT_SECRET` to a randomly generated secret of at least 32 bytes before starting the app. There is no default production secret. `JWT_EXPIRATION_SECONDS` defaults to 3600 (one hour). ProjectS was used as a reference for JJWT token signing and bearer authentication.

Send `POST /api/auth/login` with JSON:

```json
{"email":"player@example.com","password":"StrongPass123!"}
```

Email matching is case-insensitive. A successful response contains `token`, `tokenType` (`Bearer`), `expiresIn` in seconds, and `user` details. Password hashes are never returned. Invalid credentials, blocked accounts, and deleted accounts return 401 with the same message. Invalid input returns 400.

For protected requests, send `Authorization: Bearer <token>`. Tokens contain the numeric user ID as subject, role, issuer, issue time, and expiry. Signature and expiration are checked, and current account status is checked on every request. Blocking or deleting a user also rejects their existing tokens. Authentication is stateless: session cookies, HTTP Basic, form login, and CSRF tokens are not used. Access tokens are sent explicitly in the Authorization header.

Login and POST registration routes (/api/users, /api/users/player, /api/users/venueOwner, /api/users/appOwner) are public. Anyone can register any of these roles, including APP_OWNER. Read, update, and delete routes require authentication; ownership and role-based authorization rules are not implemented. Accounts need a non-null email and a BCrypt password hash to log in. Register the initial account through a public registration route. There is no refresh-token or logout revocation endpoint; access tokens expire automatically.

## Testing

Run `./mvnw.cmd test` from the nested BoxProject directory. Tests use an isolated H2 database and a test-only JWT signing secret. They cover user CRUD, login, password validation, stateless bearer authentication, invalid/expired/forged tokens, and blocked/deleted accounts. They do not connect to your PostgreSQL database.

