import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type UserProfileQueryVariables = Types.Exact<{ [key: string]: never; }>;


export type UserProfileQuery = { __typename?: 'Query', userProfile: { __typename?: 'User', id?: string | null, publicName: string, firstName?: string | null, lastName?: string | null, email: string, emailVerifiedAt?: any | null, roles: Array<{ __typename?: 'Role', name: string }> } };

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