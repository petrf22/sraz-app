/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type UserInput = {
  email: string;
  firstName?: string | null | undefined;
  id?: string | number | null | undefined;
  lastName?: string | null | undefined;
  password?: string | null | undefined;
  publicName: string;
};

export type UserCreateMutationVariables = Exact<{
  userInput: Types.UserInput;
}>;


export type UserCreateMutation = { userCreate: { id: string | null, publicName: string, firstName: string | null, lastName: string | null, email: string, emailVerifiedAt: string | null, roles: Array<{ name: string }> } };

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