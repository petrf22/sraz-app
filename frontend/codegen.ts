import { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  schema: 'http://localhost:8080/graphql', // URL tvého GraphQL serveru
  //documents: 'src/**/*.graphql',           // kam píšeš .graphql soubory
  overwrite: true,
  generates: {
    'src/app/graphql/generated.ts': {      // kam se vygeneruje kód
      plugins: [
        'typescript',
        'typescript-operations',
        'typescript-apollo-angular'
      ],
      config: {
        gqlImport: 'apollo-angular#gql', // použije Apollo Angular gql
        ngModule: 'GraphQLModule',       // volitelné – vygeneruje NgModule
        serviceName: 'GraphQL',          // prefix pro vygenerované služby
      }
    }
  }
};
export default config;