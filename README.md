# Sraz

Web: [sraz.app](https://sraz.app) (připravuje se)

Open-source aplikace pro přihlašování na pravidelné sportovní akce (např. večerní hokej jednou týdně).

## Co bude umět
- **Pozvánky e-mailem** s unikátním odkazem (osoba × termín), platným do začátku akce.
- **Stálí členové a náhradníci** – různé časy rozesílání pozvánek, kapacita, fronta, uzávěrka přihlášek.
- **Týmy** (např. modří × červení) – hráč si při přihlášení vybere tým.
- **Opakované akce** s různými obdobími (např. září–březen každý týden).
- **Místa** s adresou, odkazem na mapu a GPS.
- **Platby** – cena ledu za hodinu, poplatek stálého člena, podíl náhradníků, brankáři zdarma, QR platba.
- **Bank** – přebytky a ruční pohyby (akce na Vánoce, konec sezóny).
- **Statistiky** hráčů – docházka, platby, góly.
- Webová aplikace (PWA), později Android přes Capacitor.

## Technologie
- `sraz-be/` – Java 25, Spring Boot 4, Spring GraphQL (DGS codegen), Liquibase, PostgreSQL
- `sraz-fe/` – Angular 21, ng-zorro-antd, Apollo GraphQL

## Spuštění pro vývoj
Potřeba: JDK 25, Node.js 24 (nvm), Docker.

Nejrychlejší je `./start-dev.sh` – otevře okna s databází, Mailpitem, backendem a frontendem,
nahraje testovací data (`dev/seed.sql`), otevře prohlížeč a po stisku klávesy vše ukončí.
Ručně:

```bash
# backend – Postgres z compose.yaml (port 5438) si nastartuje sám
cd sraz-be
./gradlew bootRun

# frontend → http://localhost:4200
cd sraz-fe
npm ci
npm start
```

Přihlašovací kód se ve vývoji jen vypíše do logu backendu. Ostatní e-maily (pozvánky) chytá
[Mailpit](https://mailpit.axllent.org/): `docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit`.

Testy: `./gradlew test` (vyžaduje Docker – Testcontainers) a `npm test`.

## Nasazení
Docker Compose na vlastním VPS (Caddy s automatickým HTTPS, Postgres, backend), build na serveru,
zálohy cronem – postup v [docs/nasazeni.md](docs/nasazeni.md), skripty v [ops/](ops/README.md).

Podrobnosti k architektuře jsou v [CLAUDE.md](CLAUDE.md).

## Licence
[GNU AGPL-3.0](LICENSE) – upravenou verzi provozovanou jako službu je nutné zveřejnit pod stejnou licencí.
