# CLAUDE.md – backend

Konvence a příkazy pro `sraz-be/` (Spring Boot 4.1, Java 25, Gradle Groovy DSL). Společná pravidla,
architektura a nasazení jsou v kořenovém [`CLAUDE.md`](../CLAUDE.md).

## Příkazy

```bash
./gradlew bootRun                    # Boot si přes spring-boot-docker-compose sám nastartuje ../compose.yaml (Postgres :5438)
./gradlew test                       # všechny testy – VYŽADUJE běžící Docker (Testcontainers Postgres)
./gradlew test --tests "*.RegistrationServiceTest"   # jeden test
./gradlew build                      # kompilace + testy (totéž dělá CI)
```

Gradle není nainstalovaný globálně – vždy `./gradlew`.

## Konvence

- Package `cz.petrf.sraz`: `config`, `controller` (REST i GraphQL controllery), `service`, `security`,
  `exception`, `graphql` (`GraphqlExceptionHandler`), `db/entity`, `db/repo`, `db/seed` (`DataSeed`,
  jen profil `dev`).
- Lombok: `@RequiredArgsConstructor` (konstruktorová injektáž), `@Slf4j`, `@Builder`; entity dědí
  `BaseEntity` (`Persistable<Long>`, `createdAt`/`updatedAt`), sloupce `TIMESTAMPTZ` ↔ `OffsetDateTime`,
  výčty `@Enumerated(STRING)` do `VARCHAR`.
- Aktuální čas přes injektovaný `Clock` (bean v `SchedulingConfig`), ne `OffsetDateTime.now()` bez hodin.
- Doménová pravidla jsou ve službách; porušení = `DomainException` s českou zprávou pro uživatele
  (`NotFoundException`, `ForbiddenException` pro oprávnění). Controllery jen předávají.
- Každá změna přihlášky/členství jde do `AuditService` (kdo, co, kdy).

## Databáze a migrace

Schéma vlastní **Liquibase** (`spring.jpa.hibernate.ddl-auto=validate`) – každá změna entity potřebuje
changelog v `src/main/resources/db/changelog/<datum>/NN-nazev.yaml` zařazený do
`db.changelog-master.yaml`. Už nasazený changeset nikdy neupravuj, přidej nový. Changesety se schématem
nemají context; `dev` context jen pro seed data. Pozor na YAML flow zápis: typ s čárkou dej do uvozovek
(`type: "DECIMAL(9,6)"`).

## Konfigurace

- `application.properties` = výchozí **dev** hodnoty (Postgres :5438, Mailpit :1025, kód jen do logu,
  dev JWT secret) – nic dalšího pro vývoj není potřeba.
- `application-prod.properties` = produkce, hodnoty z proměnných prostředí bez výchozích hodnot
  (vzor v kořenovém `.env.example`).
- `application-dev.properties` = lokální přepisy vývojáře, gitignored, nikdy necommitovat.
- Testy: `src/test/resources/application.properties` **nahrazuje** hlavní `application.properties`
  (stejné jméno na classpath) – novou povinnou vlastnost přidej i tam.

## Testy

Integrační testy běží proti reálnému Postgresu (Testcontainers, `@Import(TestcontainersConfiguration.class)`).
Služby: potomci `ServiceTestSupport` (transakce se na konci vrátí, `EmailService` je mock).
HTTP/GraphQL: `@SpringBootTest` + `@AutoConfigureMockMvc` (`SportGraphqlTest`, `AuthControllerTest`,
`PublicTokenControllerTest`).
