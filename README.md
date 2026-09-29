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
Potřeba: JDK 25, Node.js 24, Docker.

```bash
# backend (PostgreSQL se spustí přes compose.yaml)
cd sraz-be
./gradlew bootRun --args='--spring.profiles.active=dev'

# frontend → http://localhost:4200
cd sraz-fe
npm ci
npm start
```

Testy: `./gradlew test` (vyžaduje Docker – Testcontainers) a `npm test`.

Podrobnosti k architektuře jsou v [CLAUDE.md](CLAUDE.md).

## Licence
[GNU AGPL-3.0](LICENSE) – upravenou verzi provozovanou jako službu je nutné zveřejnit pod stejnou licencí.
