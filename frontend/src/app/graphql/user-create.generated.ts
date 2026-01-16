import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type UserCreateMutationVariables = Types.Exact<{
  userInput: Types.UserInput;
}>;


export type UserCreateMutation = { __typename?: 'Mutation', userCreate: { __typename?: 'User', id?: string | null, publicName: string, firstName?: string | null, lastName?: string | null, email: string, emailVerifiedAt?: any | null, roles: Array<{ __typename?: 'Role', name: string }> } };

export const UserCreateDocument = gql`
    mutation UserCreate($userInput: UserInput!) {
  userCreate(userInput: $userInput) {
    id
    publicName
    firstName
    lastName
    email
    emailVerifiedAt
    roles {
      name
    }
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class UserCreateGQL extends Apollo.Mutation<UserCreateMutation, UserCreateMutationVariables> {
    document = UserCreateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }