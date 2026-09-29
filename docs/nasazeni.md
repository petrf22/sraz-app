# Nasazení do produkce

Stejný setup jako `kvalita-cena` (jedna sada postupů pro obě aplikace), na **vlastním** serveru:
Hetzner Cloud VPS, Docker Compose (`compose.prod.yaml`: `postgres`, `backend`, `web`), build přímo
na serveru, Caddy s automatickým TLS (Let's Encrypt), SMTP přes STARTTLS 587, zálohy cronem na
server a odtud na lokální PC. Provozní skripty a jejich podrobnosti: `ops/README.md`.

Odškrtávej rovnou v tomhle souboru a commituj (k datu dopiš, kdy bylo ověřeno).

## Architektura

- `web` = Caddy image se zapečeným Angular buildem (`sraz-fe/Dockerfile`, `sraz-fe/Caddyfile`).
  Servíruje SPA a proxuje `/api/*`, `/graphql`, `/actuator/health` a `/actuator/info` na
  `backend:8080` — **web i API na jednom originu**, protože refresh token je httpOnly cookie
  se `SameSite=Strict`. Caddy přepisuje `X-Forwarded-For` skutečnou IP klienta (limity žádostí
  o přihlašovací kód, `ClientIpResolver`).
- `backend` = Spring Boot jar (`sraz-be/Dockerfile`), profil `prod`
  (`application-prod.properties` — chybějící proměnná prostředí appku shodí hned při startu).
- `postgres` = Postgres 17 bez portu ven, data ve volume `sraz-postgres-data-prod`.
- Tajemství v `.env` vedle `compose.prod.yaml` (vzor `.env.example`, gitignored).

## Vydání verze

1. Na `main` zvýšit `version` v `sraz-be/build.gradle` (a `version` v `sraz-fe/package.json`),
   commit „Vydání X.Y.Z", push, počkat na zelené CI.
2. `git tag vX.Y.Z && git push origin vX.Y.Z`.
3. Na serveru `./ops/deploy.sh X.Y.Z` — ověří i to, že `/actuator/info` hlásí právě tuhle verzi
   a commit.

## 1. Předem (nezávislé na serveru)

- [ ] **Doména** `sraz.app` zaregistrovaná (u jiného dodavatele než VPS — jeden výpadek nesmí vzít
  server i doménu).
- [ ] **E-mailová schránka** pro odesílání (např. `kontakt@sraz.app`) u poskytovatele s SMTP na
  portu **587** (STARTTLS). Hetzner blokuje odchozí porty **25 a 465** na všech cloud serverech.
- [ ] **SPF/DKIM** pro odesílací doménu podle poskytovatele schránky. Nový `include:` vždy do
  STÁVAJÍCÍHO SPF záznamu, nikdy druhý SPF záznam (= neprojde žádná pošta). DMARC zpočátku `p=none`.
- [ ] **Vygenerovat tajemství** a uložit offline kopii `.env` mimo server
  (`backup/sraz-prod.env` na lokálním PC — `/backup/` je gitignored):
  - `POSTGRES_PASSWORD` — `openssl rand -hex 32` (**ne base64**: jde přes `${}` interpolaci
    v compose, kde by se znak `$` expandoval)
  - `JWT_SECRET` — `openssl rand -base64 64 | tr -d '\n'` (jde přes `env_file` doslova)

## 2. Server

Nejdřív rozjet appku **proti IP bez DNS** (kroky 1–3), teprve pak přepnout doménu (krok 4–5) —
build na serveru je nejpravděpodobnější místo prvního selhání a nemá se ladit zároveň s DNS
a limity Let's Encrypt.

1. [ ] Založit Hetzner Cloud instanci **minimálně 2 vCPU / 4 GB RAM** (build Gradle + `ng build`
   běží přímo na serveru) + **2 GB swap**, Docker + Compose plugin, non-root uživatel `sraz`
   se SSH klíčem, `ufw` (22/80/443), `unattended-upgrades`.
2. [ ] `git clone` repa do `/home/sraz/sraz-app`, `cp .env.example .env`, doplnit tajemství
   z kroku 1 výš a `SITE_ADDRESS=http://<IP serveru>` (s `http://` Caddy poslouchá jen na :80
   a ACME se nespustí).
3. [ ] **Sekvenční build** (ne jedno `up -d --build`, které staví backend i web paralelně):
   ```bash
   export GIT_SHA=$(git rev-parse --short HEAD)
   docker compose -f compose.prod.yaml build backend
   docker compose -f compose.prod.yaml build web
   docker compose -f compose.prod.yaml up -d
   ```
   Ověřit: `curl http://<IP>/` (Angular), `curl http://<IP>/actuator/health` (`UP`), v logu
   backendu (`docker compose -f compose.prod.yaml logs backend`) doběhlý Liquibase bez chyb.
   Přihlášení proti IP nefunguje — `app.auth.cookie-secure=true` v produkci znamená, že se
   refresh cookie po obyčejném HTTP nenastaví.
4. [ ] **DNS**: `A`/`AAAA` `sraz.app` → IP serveru (předem snížit TTL na 300 s). Volitelně
   `www.sraz.app` + `WWW_ADDRESS` v `.env` (přesměrování na adresu bez www).
   Ověřit `dig +short sraz.app` z jiného stroje.
5. [ ] V `.env` `SITE_ADDRESS=https://sraz.app` (při prvním pokusu klidně
   `ACME_CA=https://acme-staging-v02.api.letsencrypt.org/directory`, pak zakomentovat),
   `docker compose -f compose.prod.yaml up -d`. Ověřit `https://sraz.app` bez varování
   prohlížeče a `https://sraz.app/actuator/health` → `UP`.

   **Nikdy `docker compose -f compose.prod.yaml down -v`** — smaže produkční databázi
   i certifikáty (opakované mazání certifikátů navíc vyčerpá limit Let's Encrypt).
6. [ ] **SMTP** do `.env` (`SMTP_HOST`, `SMTP_PORT=587`, `SMTP_USERNAME`, `SMTP_PASSWORD`,
   `SMTP_FROM="Sraz <kontakt@sraz.app>"`), `up -d`, přihlásit se na **externí** schránku
   (Gmail apod.) a ověřit, že kód dorazil a neskončil ve spamu.
7. [ ] **Zálohy**: cron `ops/backup.sh` a **vyzkoušet obnovu** na čistou instanci
   (`ops/README.md`, „Zkouška obnovy"); na lokálním PC cron `ops/pull-backup.sh` s vyhrazeným
   klíčem omezeným přes `rrsync`.
8. [ ] **Prompt serveru** `ops/prod-prompt.sh` (červený štítek `PRODUKCE`).
9. [ ] **První přihlášení** vlastním e-mailem a nastavení globálního administrátora ručně v DB:
   ```bash
   docker compose -f compose.prod.yaml exec -T postgres psql -U "$POSTGRES_USER" -d sraz -c \
     "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r
      WHERE u.email = '<tvůj e-mail>' AND r.name = 'ROLE_ADMIN';"
   ```
   Admin se projeví po obnovení přihlášení (access token platí 10 minut).

## Zbývá do budoucna

- [ ] Retence provozních logů (kontejnerový `stdout` bez `logging:` konfigurace, Caddy bez
  access logu) — stejné nevyřešené jako u kvalita-cena.
- [ ] Bezpečnostní hlavičky v Caddyfile (HSTS, CSP) — stejně jako u kvalita-cena zatím nejsou.
