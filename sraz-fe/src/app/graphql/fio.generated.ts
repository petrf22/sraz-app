/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type BankTransactionStatus =
  | 'IGNORED'
  | 'MATCHED'
  | 'UNMATCHED';

export type GroupFioQueryVariables = Exact<{
  id: string | number;
}>;


export type GroupFioQuery = { group: { id: string, fioConnected: boolean, fioLastSyncAt: string | null, fioLastError: string | null, bankTransactions: Array<{ id: string, bookedOn: string, amount: number, currency: string | null, variableSymbol: string | null, counterAccount: string | null, counterName: string | null, message: string | null, note: string | null }>, unpaidCharges: Array<{ id: string, amount: number, user: { id: string, publicName: string }, event: { id: string, name: string, startsAt: string } }> } };

export type GroupSetFioTokenMutationVariables = Exact<{
  groupId: string | number;
  token?: string | null | undefined;
}>;


export type GroupSetFioTokenMutation = { groupSetFioToken: { fetched: number, created: number, matched: number, unmatched: number } | null };

export type FioSyncMutationVariables = Exact<{
  groupId: string | number;
}>;


export type FioSyncMutation = { fioSync: { fetched: number, created: number, matched: number, unmatched: number } };

export type BankTransactionAssignMutationVariables = Exact<{
  id: string | number;
  chargeId: string | number;
}>;


export type BankTransactionAssignMutation = { bankTransactionAssign: { id: string, status: Types.BankTransactionStatus } };

export type BankTransactionIgnoreMutationVariables = Exact<{
  id: string | number;
}>;


export type BankTransactionIgnoreMutation = { bankTransactionIgnore: { id: string, status: Types.BankTransactionStatus } };

export const GroupFioDocument = gql`
    query GroupFio($id: ID!) {
  group(id: $id) {
    id
    fioConnected
    fioLastSyncAt
    fioLastError
    bankTransactions(status: UNMATCHED) {
      id
      bookedOn
      amount
      currency
      variableSymbol
      counterAccount
      counterName
      message
      note
    }
    unpaidCharges {
      id
      amount
      user {
        id
        publicName
      }
      event {
        id
        name
        startsAt
      }
    }
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupFioGQL extends Apollo.Query<GroupFioQuery, GroupFioQueryVariables> {
    document = GroupFioDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupSetFioTokenDocument = gql`
    mutation GroupSetFioToken($groupId: ID!, $token: String) {
  groupSetFioToken(groupId: $groupId, token: $token) {
    fetched
    created
    matched
    unmatched
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupSetFioTokenGQL extends Apollo.Mutation<GroupSetFioTokenMutation, GroupSetFioTokenMutationVariables> {
    document = GroupSetFioTokenDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const FioSyncDocument = gql`
    mutation FioSync($groupId: ID!) {
  fioSync(groupId: $groupId) {
    fetched
    created
    matched
    unmatched
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class FioSyncGQL extends Apollo.Mutation<FioSyncMutation, FioSyncMutationVariables> {
    document = FioSyncDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const BankTransactionAssignDocument = gql`
    mutation BankTransactionAssign($id: ID!, $chargeId: ID!) {
  bankTransactionAssign(id: $id, chargeId: $chargeId) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class BankTransactionAssignGQL extends Apollo.Mutation<BankTransactionAssignMutation, BankTransactionAssignMutationVariables> {
    document = BankTransactionAssignDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const BankTransactionIgnoreDocument = gql`
    mutation BankTransactionIgnore($id: ID!) {
  bankTransactionIgnore(id: $id) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class BankTransactionIgnoreGQL extends Apollo.Mutation<BankTransactionIgnoreMutation, BankTransactionIgnoreMutationVariables> {
    document = BankTransactionIgnoreDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }