import { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  schema: 'http://localhost:8080/graphql',
  documents: 'src/app/graphql/**/*.graphql',
  overwrite: true,
  generates: {
    'src/app/graphql/graphql-types.ts': {
      plugins: [
        'typescript',
      ],
    },
    'src/app/graphql/': {
      preset: 'near-operation-file',
      presetConfig: {
        baseTypesPath: 'graphql-types.ts',
        extension: '.generated.ts',
      },
      plugins: [
        'typescript-operations',
        'typescript-apollo-angular',
      ],
      config: {
        gqlImport: 'apollo-angular#gql',
      },
    },
  }
};
export default config;