# SSO Service & User Service — Project Plan

**Scope:** Working MVP for `sso-service` (auth, JWT, RBAC, social login, OTP) and `user-service` (user profile data), as part of a microservice platform.

**Estimate basis:** Solo developer, focused pace, MVP-level (not fully hardened production system). Add ~15-20% buffer for unexpected framework issues (already hit two Spring Boot 4 modularization surprises with Flyway and Kafka).

**Total estimate: ~71 hours baseline, ~80-85 hours with buffer.**

---

## Phase 0 — Local Environment Setup (2.5 hrs)

- [ ] Install Docker Desktop for Mac (0.5 hrs)
- [ ] Get Redis + Kafka running via `docker-compose.yml` (1.5 hrs)
- [ ] Verify connectivity for both from the app (0.5 hrs)

---

## Phase 1 — Core Auth Completion, sso-service (6 hrs)

- [ ] Wire real roles/permissions into JWT claims on login (currently stubbed empty) (2 hrs)
- [ ] Fix logout to pull real user ID + jti from security context (1.5 hrs)
- [ ] Seed default tenant + roles/permissions via Flyway migration (1 hr)
- [ ] Manual testing of register/login/refresh/logout end-to-end via Postman (1.5 hrs)

---

## Phase 2 — OTP & Verification, Email + SMS (7.5 hrs)

- [ ] Pick provider (AWS SES/SNS vs Twilio); account setup/sandbox limits (1 hr)
- [ ] Integrate real provider into `OtpService` (replace `System.out.println` stub) (3 hrs)
- [ ] Build `/forgot-password`, `/reset-password`, `/verify-otp` endpoints in `AuthController` (2 hrs)
- [ ] Test OTP expiry, rate-limiting on requests (1.5 hrs)

---

## Phase 3 — Social Login, Google + Facebook (10.5 hrs)

- [ ] Register OAuth2 apps with Google & Facebook, get client ID/secret (1.5 hrs)
- [ ] Configure Spring Security OAuth2 client (`application.yml` registration blocks) (2 hrs)
- [ ] Build callback handler: look up/create `Identity` + `User`, issue own JWT (3 hrs)
- [ ] Handle account-linking edge case (existing email, new provider) (2 hrs)
- [ ] Test both providers end-to-end (2 hrs)

---

## Phase 4 — RBAC & Admin Endpoints (7 hrs)

- [ ] Build endpoints to create/list roles & permissions per tenant (3 hrs)
- [ ] Assign/revoke roles for users (2 hrs)
- [ ] `@PreAuthorize` wiring across protected endpoints; test with different role combos (2 hrs)

---

## Phase 5 — GDPR Endpoints (4.5 hrs)

- [ ] Data export endpoint (JSON dump of user's data) (2 hrs)
- [ ] Right-to-be-forgotten (soft-delete + anonymize PII) (2 hrs)
- [ ] Publish `user.deleted` event on deletion (0.5 hrs)

---

## Phase 6 — Kafka Event Integration (2.5 hrs)

*(blocked on Phase 0 — Docker/Kafka setup)*

- [ ] Verify topic auto-creation, test producer end-to-end (1 hr)
- [ ] Add basic throwaway consumer to confirm events land (1.5 hrs)

---

## Phase 7 — Observability (4.5 hrs)

- [ ] Verify Actuator/Prometheus metrics endpoints work (1 hr)
- [ ] Add structured JSON logging config (`logback-spring.xml`, using existing logstash encoder dependency) (1.5 hrs)
- [ ] OpenTelemetry/tracing setup (currently commented out — revisit) (2 hrs)

---

## Phase 8 — Testing (8 hrs)

- [ ] Unit tests for `AuthService`, `JwtService`, `OtpService` (4 hrs)
- [ ] Integration tests with Testcontainers (Postgres + Kafka) (4 hrs)

---

## Phase 9 — user-service (New Microservice) (13 hrs)

- [ ] Project setup (pom.xml, package structure) (1 hr)
- [ ] Profile entity + repository (name, address, preferences, references shared `user_id`) (2 hrs)
- [ ] Kafka consumer for `user.created`/`user.deleted` events from sso-service (3 hrs)
- [ ] REST endpoints for profile CRUD (JWT validation via sso-service's JWKS) (4 hrs)
- [ ] Cross-service integration testing (register in sso-service → confirm profile created in user-service) (3 hrs)

---

## Phase 10 — Deployment Prep (5 hrs)

- [ ] Dockerize both services (`Dockerfile` each) (2 hrs)
- [ ] Full `docker-compose.yml` spanning both services + shared infra (2 hrs)
- [ ] Basic README/runbook for local setup (1 hr)

---

## Summary Table

| Phase | Description | Hours |
|---|---|---|
| 0 | Environment Setup | 2.5 |
| 1 | Core Auth Completion | 6 |
| 2 | OTP & Verification | 7.5 |
| 3 | Social Login | 10.5 |
| 4 | RBAC & Admin | 7 |
| 5 | GDPR | 4.5 |
| 6 | Kafka Integration | 2.5 |
| 7 | Observability | 4.5 |
| 8 | Testing | 8 |
| 9 | user-service | 13 |
| 10 | Deployment Prep | 5 |
| **Total** | | **~71 hrs** |
| **With 15-20% buffer** | | **~80-85 hrs** |

---

## Explicitly Out of Scope (Future / Post-MVP)

- Production-grade secret management (Vault / AWS Secrets Manager) in place of the placeholder JWT secret
- CI/CD pipeline
- Load testing
- API gateway in front of both services
- Kubernetes deployment (currently targeting Docker Compose / VMs)

---

## Known Gotchas Encountered So Far (Spring Boot 4 Modularization)

Spring Boot 4 split the old monolithic `spring-boot-autoconfigure` jar into per-technology modules. Several dependencies that "just worked" via autoconfiguration in Boot 3 now require an explicit `spring-boot-starter-<technology>` module even if the underlying third-party library is already on the classpath:

- **Flyway** → needed `spring-boot-starter-flyway` (raw `flyway-core` alone did nothing, silently)
- **Kafka** → needed `spring-boot-starter-kafka` (raw `spring-kafka` alone left `KafkaTemplate` unresolvable)

If a new dependency silently doesn't autoconfigure (no error, just missing beans), check the Spring Boot 4.0 Migration Guide's starter list before assuming it's a code bug.