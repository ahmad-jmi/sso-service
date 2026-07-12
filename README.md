# sso-service
This project is part of microservice architecture

# User Service — Requirements & Architecture Spec

## 1. Overview
A standalone microservice responsible for identity, authentication, authorization, and user profile management across a multi-tenant platform. Other services in the ecosystem will depend on it for user identity and permission checks.

**Stack:** Java 21 + Spring Boot 3.x, PostgreSQL, Redis, Kafka, Docker Compose (VM-based deployment).

---

## 2. Core Responsibilities
- User registration, login, logout
- JWT issuance (access + refresh tokens) with revocation support
- Forgot password / reset password via email & SMS OTP
- Email + mobile verification (OTP-based)
- Social login (Google, Facebook) with account linking
- Multi-tenant user & permission management
- Full RBAC — custom roles/permissions per tenant
- GDPR compliance — data export & right-to-be-forgotten
- Publish domain events (user.created, user.updated, user.deleted, etc.) to Kafka
- Expose REST APIs for synchronous calls from other services

---

## 3. Tech Stack Detail

| Concern | Choice |
|---|---|
| Language/Framework | Java 21, Spring Boot 3.x (Spring Web, Spring Security, Spring Data JPA) |
| Database | PostgreSQL (primary datastore) |
| Cache/Session store | Redis (token blacklist, OTP storage, rate limiting) |
| Messaging | Kafka (domain events) + REST (synchronous calls) |
| Auth | JWT (self-issued), OAuth2 client for Google/Facebook |
| Deployment | Docker Compose / VMs (not Kubernetes) |
| Observability | ELK (logs), Prometheus + Grafana (metrics), Jaeger/OpenTelemetry (tracing) |
| Migrations | Flyway or Liquibase |
| API Docs | springdoc-openapi (Swagger UI) |

---

## 4. Data Model (initial draft)

### `tenants`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | varchar | |
| status | enum | active/suspended |
| created_at | timestamp | |

### `users`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | FK → tenants, indexed |
| email | varchar | unique per tenant |
| phone | varchar | unique per tenant, nullable |
| password_hash | varchar | nullable (social-login-only users) |
| email_verified | boolean | |
| phone_verified | boolean | |
| status | enum | active/disabled/deleted (soft delete for GDPR) |
| created_at / updated_at | timestamp | |

### `identities` (social login linkage)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK → users |
| provider | enum | google/facebook/local |
| provider_user_id | varchar | |

### `roles` / `permissions` / `user_roles` (per-tenant RBAC)
- `roles`: id, tenant_id, name, description
- `permissions`: id, name, description (e.g. `user:read`, `billing:write`)
- `role_permissions`: role_id, permission_id
- `user_roles`: user_id, role_id, tenant_id

### `refresh_tokens`
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK |
| token_hash | varchar | never store raw token |
| expires_at | timestamp | |
| revoked | boolean | |

### `otp_verifications` (stored in Redis with TTL, not Postgres)
- key: `otp:{channel}:{user_id}`, value: hashed OTP, TTL: 5–10 min

---

## 5. Authentication & Token Design

- **Access token:** short-lived JWT (~15 min), signed (RS256 recommended for cross-service verification without sharing secret).
- **Refresh token:** longer-lived (7–30 days), opaque or JWT, stored hashed in DB, rotated on each use.
- **Logout / revoke-all:** refresh tokens revoked in DB; access tokens checked against a Redis blacklist (keyed by `jti`, TTL = remaining token life) since JWTs can't be invalidated otherwise.
- **JWT claims:** `sub` (user id), `tenant_id`, `roles`, `permissions` (or a reference to fetch them), `jti`, `exp`.
- Other services verify JWTs locally using the public key (JWKS endpoint exposed by this service) — no network call needed per request.

---

## 6. Verification & Password Reset Flows

1. **Registration:** create user → generate email OTP + SMS OTP → store hashed in Redis with TTL → send via provider (SES/Twilio) → user confirms → mark verified.
2. **Forgot password:** user requests reset → OTP sent to verified email/phone → user submits OTP + new password → validate against Redis → update `password_hash` → invalidate all existing refresh tokens.
3. **Rate limiting:** OTP requests limited per user/IP (e.g. 5/hour) via Redis counters to prevent abuse.

---

## 7. Social Login

- OAuth2/OIDC flow (Authorization Code) against Google & Facebook.
- On callback: look up `identities` by `(provider, provider_user_id)`. If not found, check if email matches existing local account → prompt to link, or create new user + identity record.
- Issue same internal JWT/refresh token pair regardless of login method.

---

## 8. Multi-Tenancy Strategy

- Shared database, shared schema, `tenant_id` column on all tenant-scoped tables (simplest to operate at this stage; can evolve to schema-per-tenant later if needed).
- All queries must be tenant-scoped — enforce via a base repository/service layer or Hibernate filters to prevent cross-tenant leaks.
- JWT carries `tenant_id`; every downstream service uses it to scope its own data.

---

## 9. RBAC Design

- Permissions are fine-grained strings (`resource:action`).
- Roles bundle permissions and are tenant-scoped (tenant admins can define custom roles).
- A small set of system-level roles (e.g. `platform_admin`) exist outside tenant scope for internal ops.
- Authorization check: Spring Security method-level (`@PreAuthorize`) evaluating permissions extracted from JWT.

---

## 10. Kafka Events Published

| Topic | Trigger |
|---|---|
| `user.created` | New user registered (any method) |
| `user.updated` | Profile/role/status change |
| `user.deleted` | Soft/hard delete (GDPR) |
| `user.email_verified` | Email verification complete |
| `user.password_reset` | Password successfully reset |

Events should carry minimal payload (user id, tenant id, event type, timestamp) — consumers fetch full detail via REST if needed, avoiding large/stale payloads in the event bus.

---

## 11. GDPR Compliance

- **Data export:** endpoint (`GET /users/{id}/export`) returning all personal data as JSON/PDF.
- **Right to be forgotten:** soft-delete + anonymize PII (email/phone hashed or nulled) while preserving referential integrity for audit/financial records elsewhere; publish `user.deleted` so dependent services purge their copies too.
- **Audit trail:** log who accessed/exported/deleted user data and when.

---

## 12. Observability

- **Logging:** structured JSON logs shipped to ELK; correlation/trace IDs propagated across services.
- **Metrics:** Prometheus + Micrometer (login rate, token issuance rate, OTP failures, RBAC denials).
- **Tracing:** OpenTelemetry + Jaeger for distributed request tracing across microservices.

---

## 13. Open Items to Decide Next

- Email/SMS provider choice (SES + SNS vs Twilio vs others)
- Password policy specifics (complexity, expiry, history)
- Exact list of permission strings needed by the rest of the platform
- Whether hard-delete is ever required, or soft-delete + anonymization is sufficient
- CI/CD pipeline tooling
- API versioning strategy (URI-based `/v1/` recommended to start)
