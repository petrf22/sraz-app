# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A monorepo for the "Sraz" application (signup for recurring sports events, e.g. weekly hockey), split into two independently-built projects:

- `sraz-be/` — Spring Boot 4 backend (Java 25, Gradle), serving a GraphQL API for domain data and a REST API for authentication.
- `sraz-fe/` — Angular 21 frontend (standalone components, ng-zorro-antd UI, Apollo GraphQL client).

The two communicate over GraphQL (`/graphql`) and REST (`/api/**`). The Angular dev server proxies `/api` and `/graphql` to the backend on port 8080 (`sraz-fe/proxy.conf.json`).

## Commands

### Backend (`sraz-be/`, run from that directory)
- Run app: `./gradlew bootRun` (default profile expects PostgreSQL; use `dev` for seed data: `./gradlew bootRun --args='--spring.profiles.active=dev'`)
- Build: `./gradlew build`
- All tests: `./gradlew test` (**requires Docker** — uses Testcontainers Postgres)
- Single test class: `./gradlew test --tests "cz.petrf.sraz.security.JwtServiceTest"`
- Single test method: `./gradlew test --tests "cz.petrf.sraz.security.JwtServiceTest.methodName"`
- Start a local PostgreSQL for dev: `docker compose up` (uses `compose.yaml`; Postgres on `localhost:5436`, db `sraz`). Spring Boot's docker-compose support may also start it automatically.

### Frontend (`sraz-fe/`, run from that directory)
- Dev server: `npm start` (or `ng serve`) → http://localhost:4200
- Build: `npm run build`
- Tests (Karma/Jasmine): `npm test` (or `ng test`)
- Regenerate GraphQL TS types: `npm run codegen` (or `codegen:watch`). `codegen.ts` reads the schema files directly from `../sraz-be/src/main/resources/graphql/`, no running backend needed.

## Architecture

### Authentication flow (the central concept)
**No passwords.** Login is e-mail + one-time code (ported from the `kvalita-cena` app):

1. `POST /api/auth/otp/request {email}` → `OtpService` stores a `LoginChallenge` (BCrypt hash of a 6-digit code, 10 min TTL, max 5 attempts, one active challenge per e-mail) and e-mails the code (`templates/email/otp-code-email.html`). Returns `{challengeUid, expiresInSec, resendAfterSec}`. Rate limited in memory by `OtpRateLimiter` (Caffeine). With `app.auth.otp.mail-enabled=false` the code is only logged (`[DEV]`).
2. `POST /api/auth/otp/verify {challengeUid, code, email, termsAccepted}` → a new account is created only here and only with `termsAccepted=true`. All failures return the same `INVALID_CHALLENGE`.

Token model:
- **Access token (JWT, 10 min)** in the response body (`TokenResponse`); the FE keeps it **only in memory** and refreshes it proactively before expiry. Sent as `Authorization: Bearer`, validated by `JwtRequestFilter` (invalid/expired token = anonymous request, blocked users are never authenticated).
- **Refresh token** = random 32 bytes in the httpOnly `SameSite=Strict` cookie `refresh_token` (path `/api/auth`, 90 days); only its SHA-256 is stored (`refresh_token` table). `POST /api/auth/refresh` **rotates** it; reusing an already-used token (after `reuse-grace-period`) revokes the whole family (= that login on that device). `POST /api/auth/logout[?deleteAccount=true]` revokes the family (and optionally deletes the account). "Remember me" = the FE calls `/api/auth/refresh` on app start.
- Settings live in `AuthProperties` (`app.auth.*`); `app.auth.cookie-secure=false` is needed only for plain-HTTP non-localhost dev.

**Per-event e-mail tokens are not logins.** `/api/public/invitations/{token}` (FE `/prihlaska/:token`) lets the holder sign up/out for that one event only; `/api/public/group-invites/{token}` (FE `/pozvanka/:token`) accepts a group membership.

Security is **stateless** (no server session). `SecurityConfig` permits `/api/auth/**`, `/api/public/**` and actuator health/info; everything else under `/api/**` requires authentication. GraphQL (`/graphql`) is reachable anonymously but every resolver requires a user via `CurrentUserService` and group permissions via `AccessService` (member / organizer / global `ROLE_ADMIN`); errors carry `extensions.code` (`UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `BAD_REQUEST`). CORS is locked to `localhost:4200`/`127.0.0.1:4200`.

### Backend package layout (`cz.petrf.sraz`)
- `db/entity` — JPA entities; `db/repo` — Spring Data repositories; `db/seed/DataSeed` — fake data generator, **`dev` profile only**.
- `controller` — REST (`AuthController`, `UserController`) and GraphQL (`UserGraphqlController`) controllers.
- `service`, `security`, `config`, `exception`, `graphql` (`GraphqlExceptionHandler` maps exceptions to GraphQL error codes).

### Database & migrations
Schema is owned by **Liquibase**, not Hibernate — `spring.jpa.hibernate.ddl-auto=validate`, so entity changes must be matched by a changelog. Add changelogs under `sraz-be/src/main/resources/db/changelog/<date>/` and `include` them in `db.changelog-master.yaml`. Changesets are gated by Liquibase **contexts** (`prod`, `dev`, plus `test` for tests); the active context is set per profile (`spring.liquibase.contexts`). Schema changesets carry no context (always run); `dev`-context changesets load seed data only.

Tests run against a **real Postgres via Testcontainers** (`TestcontainersConfiguration`, wired with `@ServiceConnection`); Liquibase runs the migrations under context `test` and Hibernate validates entities against them. This means `./gradlew test` **requires Docker running**. A `@SpringBootTest` must `@Import(TestcontainersConfiguration.class)` to get a datasource (plain unit tests like `JwtServiceTest` don't need it).

### GraphQL schema is the contract between FE and BE
Backend schema lives in `sraz-be/src/main/resources/graphql/*.graphqls` (Spring GraphQL default location; the DGS codegen Gradle plugin generates Java types from it via `generateJava.schemaPaths` in `build.gradle`). Frontend operations live in `sraz-fe/src/app/graphql/*.graphql`; `npm run codegen` generates `graphql-types.ts` and per-operation `*.generated.ts` files (Apollo Angular services). **When you change the schema, regenerate types on both sides.**

### Spring profiles
- default — production-like, real PostgreSQL, external config via env vars (see `info.md` for the Railway deployment variables).
- `dev` (`application-dev.properties`) — debug logging, enables `DataSeed`, Liquibase contexts `prod,dev`, non-secure cookies. This file is **gitignored and untracked**; create it locally.
- `test` (`src/test/resources/application.properties`) — Postgres via Testcontainers, Liquibase context `test`, throwaway test-only JWT secret.

### Secrets / configuration
No secrets are committed. They are supplied as **environment variables** and resolved via Spring's relaxed binding (`JWT_SECRET` → `jwt.secret`, `APP_MAIL_RESEND_API_KEY` → `app.mail.resend.api.key`, `SPRING_DATASOURCE_*` → `spring.datasource.*`, plus `MAIL_USER`/`MAIL_PASS`). See `sraz-be/.env.example` for the full list. For local dev, copy it to `sraz-be/.env` (gitignored) or set the vars in your IDE run configuration. In production (Railway) they are set as service env vars.

## Conventions
- Code comments, commit messages, and many identifiers are in **Czech**; match the surrounding language when editing.
- Backend uses **Lombok** (`@RequiredArgsConstructor` constructor injection, `@Slf4j`, `@Builder`).
- Frontend uses Angular standalone components with lazy-loaded routes (`loadComponent`), `ng-zorro-antd`, and is localized to `cs-CZ`. Prettier config (single quotes, width 100) is in `package.json`.
