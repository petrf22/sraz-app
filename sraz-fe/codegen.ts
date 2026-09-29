import { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  // schéma čteme přímo ze souborů backendu – není potřeba běžící server
  schema: '../sraz-be/src/main/resources/graphql/*.graphqls',
  documents: 'src/app/graphql/**/*.graphql',
  overwrite: true,
  generates: {
    'src/app/graphql/graphql-types.ts': {
      plugins: [
        'typescript',
      ],
      config: {
        scalars: { DateTime: 'string', Date: 'string', Time: 'string' },
        enumsAsTypes: true,
      },
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
        scalars: { DateTime: 'string', Date: 'string', Time: 'string' },
        enumsAsTypes: true,
      },
    },
  }
};
export default config;