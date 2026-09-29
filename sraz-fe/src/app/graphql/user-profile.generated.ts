/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type UserProfileQueryVariables = Exact<{ [key: string]: never; }>;


export type UserProfileQuery = { userProfile: { id: string | null, publicName: string, firstName: string | null, lastName: string | null, email: string, emailVerifiedAt: string | null, roles: Array<{ name: string }> } };

export const UserProfileDocument = gql`
    query UserProfile {
  userProfile {
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
  export class UserProfileGQL extends Apollo.Query<UserProfileQuery, UserProfileQueryVariables> {
    document = UserProfileDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }