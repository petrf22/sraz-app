# CLAUDE.md – frontend

Konvence a příkazy pro `sraz-fe/` (Angular 21 + ng-zorro-antd + Apollo Angular). Společná pravidla,
architektura a nasazení jsou v kořenovém [`CLAUDE.md`](../CLAUDE.md).

## Příkazy

Node 24 (stejně jako CI a `Dockerfile`). `npm start` používá `proxy.conf.json` (`/api` a `/graphql`
→ `localhost:8080`), backend tedy musí běžet zároveň.

```bash
npm ci
npm run codegen      # typy a Apollo služby z ../sraz-be/src/main/resources/graphql/*.graphqls (běžící backend netřeba)
npm start            # dev server :4200
npm test             # Karma + Jasmine (headless: npm test -- --watch=false --browsers=ChromeHeadless)
npm run build        # produkční build do dist/sraz/browser (kopíruje ho sraz-fe/Dockerfile do Caddy)
```

`npm run codegen` spusť po každé změně schématu nebo `.graphql` operace – vygenerované
`src/app/graphql/*.generated.ts` a `graphql-types.ts` se commitují a CI selže, když se rozejdou.

## Konvence

- Standalone komponenty s lazy routami (`loadComponent` v `app.routes.ts`), stav v signálech
  (`signal`, `computed`), `inject()` místo konstruktorových parametrů.
- Data: GraphQL přes vygenerované služby (`XxxGQL.fetch({ variables, fetchPolicy: 'network-only' })`,
  `.mutate({ variables })`); chyby převádí `gqlErrorMessage()` ze `shared/labels.ts` (backend posílá
  české zprávy). REST jen pro přihlašování (`services/auth.service.ts`) a veřejné stránky z e-mailů
  (`services/public-api.service.ts`).
- Přihlášení: `AuthService` drží access token jen v paměti, `func/token-func.ts` ho přidává a obnovuje
  předem, `func/auth-initializer.ts` obnoví přihlášení při startu. Chráněné routy `authGuard`.
- Veřejné stránky z e-mailů (`prihlaska/`, `pozvanka/`) fungují bez přihlášení; volba z odkazu
  v e-mailu se jen předvybere a odešle až po potvrzení (e-mailové skenery odkazy samy otevírají).
- Lokalizace `cs-CZ`: `registerLocaleData` pod `cs` (ng-zorro) i `cs-CZ` (LOCALE_ID) – bez `cs`
  se výběr data otevře prázdný. České popisky výčtů v `shared/labels.ts`.
- Prettier: `printWidth: 100`, `singleQuote: true` (v `package.json`).
- Testy komponent s Apollem: `ApolloTestingModule` + `ApolloTestingController`; výsledek Apolla
  přichází asynchronně (`await fixture.whenStable()`).
