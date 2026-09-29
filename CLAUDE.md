# CLAUDE.md

Průvodce pro Claude Code v tomto repozitáři. Pravidla a příkazy specifické pro jednotlivé části
jsou v [`sraz-be/CLAUDE.md`](sraz-be/CLAUDE.md) a [`sraz-fe/CLAUDE.md`](sraz-fe/CLAUDE.md).

## Přehled

**Sraz** – open-source (AGPL-3.0) aplikace pro přihlašování na pravidelné sportovní akce
(např. večerní hokej): skupiny, pozvánky e-mailem, týmy, kapacita a fronta, uzávěrka přihlášek.
Monorepo se dvěma samostatně sestavovanými projekty:

- `sraz-be/` – Spring Boot 4 (Java 25, Gradle Groovy DSL), GraphQL API pro doménová data a REST pro
  přihlašování a veřejné stránky z e-mailů.
- `sraz-fe/` – Angular 21 (standalone komponenty, signály, ng-zorro-antd, Apollo Angular).

Frontend volá relativní `/api/**` a `/graphql` – ve vývoji je přeposílá `sraz-fe/proxy.conf.json`
na `localhost:8080`, v produkci Caddy (stejný origin, viz „Nasazení").

Nasazení, provozní skripty a CI jsou **záměrně stejné jako v aplikaci `kvalita-cena`**
(`/home/petr/pracovni/github/kvalita-cena`) – jedna sada postupů pro obě aplikace. Při změně
nasazení nejdřív zkontroluj, jak to řeší tam, a drž se stejného vzoru.

## Rychlý start (vývoj)

`./start-dev.sh [--no-seed] [--no-open]` (stejný spouštěč jako v kvalita-cena) otevře gnome-terminal
okna s DB, Mailpitem, backendem a frontendem, nahraje `dev/seed.sql` (testovací účty
`organizator@example.com`, `hrac01..12@example.com`, `brankar1/2@example.com`, skupina a akce)
a po stisku klávesy vše korektně ukončí. Ručně:

```bash
cd sraz-be && ./gradlew bootRun   # Postgres z ../compose.yaml (port 5438) si Boot nastartuje sám
cd sraz-fe && npm ci && npm start # http://localhost:4200
```

Ve výchozím (dev) nastavení se přihlašovací kód neposílá e-mailem, jen se vypíše do logu backendu
(`[DEV] Přihlašovací kód pro ...`). Ostatní e-maily (pozvánky) jdou na `localhost:1025` – spusť
Mailpit: `docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit` (UI na :8025).
Lokální přepisy patří do `sraz-be/src/main/resources/application-dev.properties` (gitignored,
profil `dev` zapíná i `DataSeed` s falešnými uživateli).

## Architektura

### Přihlašování (převzato z kvalita-cena)
**Žádná hesla.** E-mail → šestimístný kód:

1. `POST /api/auth/otp/request {email}` – `OtpService` uloží `LoginChallenge` (BCrypt hash kódu,
   platnost 10 min, 5 pokusů, jedna aktivní výzva na e-mail) a pošle kód. Limity žádostí v paměti
   (`OtpRateLimiter`, Caffeine) na e-mail i IP – IP určuje `ClientIpResolver` z `X-Forwarded-For`
   (Caddy ho přepisuje skutečnou IP).
2. `POST /api/auth/otp/verify {challengeUid, code, email, termsAccepted}` – účet vzniká až tady a jen
   se souhlasem. Všechna selhání vrací stejné `INVALID_CHALLENGE`.

Tokeny:
- **Access token** (JWT, 10 min) v těle odpovědi; frontend ho drží **jen v paměti** a obnovuje předem.
  `JwtRequestFilter`: neplatný/prošlý token = anonymní požadavek, zablokovaný uživatel se nepřihlásí.
- **Refresh token** = 32 náhodných bajtů v httpOnly `SameSite=Strict` cookie `refresh_token`
  (path `/api/auth`, 90 dní), v DB jen SHA-256. `POST /api/auth/refresh` ho **rotuje**; opakované
  použití už použitého tokenu zneplatní celou rodinu (= přihlášení na tom zařízení).
  „Zapamatování" = frontend při startu zavolá `/api/auth/refresh`.
- Nastavení v `AuthProperties` (`app.auth.*`).

**Token z pozvánky na akci není přihlášení.** `/api/public/invitations/{token}` (FE
`/prihlaska/:token`) smí jen přihlásit/odhlásit držitele na tento jeden termín;
`/api/public/group-invites/{token}` (FE `/pozvanka/:token`) potvrzuje členství ve skupině.

### Oprávnění
Bezstavové (žádná session). `SecurityConfig` pouští `/api/auth/**`, `/api/public/**` a actuator
health/info, zbytek `/api/**` vyžaduje přihlášení. `/graphql` je dostupné anonymně, ale každý resolver
vyžaduje uživatele (`CurrentUserService`) a oprávnění ve skupině (`AccessService`: člen / organizátor /
globální `ROLE_ADMIN`). Chyby GraphQL nesou `extensions.code` (`UNAUTHENTICATED`, `FORBIDDEN`,
`NOT_FOUND`, `BAD_REQUEST`).

### Doména
Skupina (`SportGroup`) → týmy, místa, členové (`GroupMember`: stálý/náhradník, hráč/brankář,
organizátor; pozvaný musí členství potvrdit) → akce (`Event`) → pozvánky (`Invitation`, token osoba ×
termín platný do začátku akce) a přihlášky (`Registration`: IN/OUT/WAITLIST). Pravidla kapacity,
fronty a uzávěrky jsou v `RegistrationService`, vlny pozvánek (stálí/náhradníci v různém předstihu,
plánovač každých 5 min) v `InvitationService`.

### GraphQL je kontrakt mezi FE a BE
Schéma: `sraz-be/src/main/resources/graphql/*.graphqls`. Operace frontendu:
`sraz-fe/src/app/graphql/*.graphql`; `npm run codegen` z nich generuje typy a Apollo služby
(commitují se, CI hlídá shodu). **Po změně schématu vždy přegeneruj.**

## Nasazení (stejné jako kvalita-cena)

Vlastní Hetzner VPS, build přímo na serveru, žádný registry ani CD. Podrobný checklist:
[`docs/nasazeni.md`](docs/nasazeni.md), skripty: [`ops/README.md`](ops/README.md).

- `compose.prod.yaml`: `postgres` (bez portu ven), `backend` (profil `prod`), `web` = Caddy se zapečeným
  Angular buildem, proxuje `/api/*`, `/graphql`, `/actuator/health|info` na backend, TLS z Let's Encrypt.
- Tajemství v `.env` vedle `compose.prod.yaml` (vzor `.env.example`). `SMTP_FROM` vždy v uvozovkách
  (skripty `.env` načítají přes bash `source`). `POSTGRES_PASSWORD` přes `openssl rand -hex 32`.
- `application-prod.properties`: proměnné bez výchozí hodnoty – chybějící appku shodí při startu.
- Vydání: zvýšit `version` v `sraz-be/build.gradle` (+ `sraz-fe/package.json`), tag `vX.Y.Z`,
  na serveru `./ops/deploy.sh X.Y.Z` (ověří i verzi a commit v `/actuator/info`).
- Zálohy: `ops/backup.sh` (cron na serveru), `ops/pull-backup.sh` (cron na lokálním PC).
- **Nikdy `docker compose -f compose.prod.yaml down -v`** (smaže DB i certifikáty).
- `compose.yaml` v kořeni je jen dev Postgres pro `bootRun`.

## Konvence

- Komentáře, commity, UI texty a řada identifikátorů jsou **česky** – drž se jazyka okolního kódu.
- Commity **tematicky** (jedna ucelená změna na commit), česky.
- Tajemství se nikdy necommitují (`.env`, `application-dev.properties` jsou gitignored).
