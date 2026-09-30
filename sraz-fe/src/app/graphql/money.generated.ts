/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type BankEntryKind =
  | 'EVENT'
  | 'MANUAL';

export type ChargeKind =
  | 'GOALIE'
  | 'REGULAR'
  | 'SUBSTITUTE';

/** Za co se platí: odehraná akce, nebo pokuta (celý podíl) za pozdní odhlášení / neomluvenou neúčast. */
export type ChargeReason =
  | 'LATE_CANCEL'
  | 'NO_SHOW'
  | 'PLAYED';

export type PaymentMethod =
  | 'CASH'
  | 'TRANSFER';

export type MyChargesQueryVariables = Exact<{ [key: string]: never; }>;


export type MyChargesQuery = { myCharges: Array<{ id: string, amount: number, kind: Types.ChargeKind, reason: Types.ChargeReason, paidAt: string | null, paidMethod: Types.PaymentMethod | null, iban: string | null, event: { id: string, name: string, startsAt: string, group: { id: string, name: string } } }> };

export type GroupBankQueryVariables = Exact<{
  id: string | number;
}>;


export type GroupBankQuery = { group: { id: string, name: string, description: string | null, iban: string | null, finesEnabled: boolean, amOrganizer: boolean, bankBalance: number, bankEntries: Array<{ id: string, kind: Types.BankEntryKind, amount: number, description: string, createdAt: string | null, createdBy: { id: string, publicName: string } | null, event: { id: string, name: string, startsAt: string } | null }> } };

export type AttendanceSetMutationVariables = Exact<{
  eventId: string | number;
  userId: string | number;
  attended: boolean;
}>;


export type AttendanceSetMutation = { attendanceSet: { id: string, attended: boolean | null } };

export type RegistrationSetExcusedMutationVariables = Exact<{
  eventId: string | number;
  userId: string | number;
  excused: boolean;
}>;


export type RegistrationSetExcusedMutation = { registrationSetExcused: { id: string, excused: boolean } };

export type EventCloseMutationVariables = Exact<{
  id: string | number;
}>;


export type EventCloseMutation = { eventClose: Array<{ id: string, amount: number }> };

export type EventReopenMutationVariables = Exact<{
  id: string | number;
}>;


export type EventReopenMutation = { eventReopen: { id: string, closedAt: string | null } };

export type ChargeSetPaidMutationVariables = Exact<{
  id: string | number;
  paid: boolean;
  method?: Types.PaymentMethod | null | undefined;
}>;


export type ChargeSetPaidMutation = { chargeSetPaid: { id: string, paidAt: string | null, paidMethod: Types.PaymentMethod | null } };

export type BankEntryAddMutationVariables = Exact<{
  groupId: string | number;
  amount: number;
  description: string;
}>;


export type BankEntryAddMutation = { bankEntryAdd: { id: string } };

export type BankEntryDeleteMutationVariables = Exact<{
  id: string | number;
}>;


export type BankEntryDeleteMutation = { bankEntryDelete: boolean };

export const MyChargesDocument = gql`
    query MyCharges {
  myCharges {
    id
    amount
    kind
    reason
    paidAt
    paidMethod
    iban
    event {
      id
      name
      startsAt
      group {
        id
        name
      }
    }
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class MyChargesGQL extends Apollo.Query<MyChargesQuery, MyChargesQueryVariables> {
    document = MyChargesDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupBankDocument = gql`
    query GroupBank($id: ID!) {
  group(id: $id) {
    id
    name
    description
    iban
    finesEnabled
    amOrganizer
    bankBalance
    bankEntries {
      id
      kind
      amount
      description
      createdAt
      createdBy {
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
  export class GroupBankGQL extends Apollo.Query<GroupBankQuery, GroupBankQueryVariables> {
    document = GroupBankDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const AttendanceSetDocument = gql`
    mutation AttendanceSet($eventId: ID!, $userId: ID!, $attended: Boolean!) {
  attendanceSet(eventId: $eventId, userId: $userId, attended: $attended) {
    id
    attended
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class AttendanceSetGQL extends Apollo.Mutation<AttendanceSetMutation, AttendanceSetMutationVariables> {
    document = AttendanceSetDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const RegistrationSetExcusedDocument = gql`
    mutation RegistrationSetExcused($eventId: ID!, $userId: ID!, $excused: Boolean!) {
  registrationSetExcused(eventId: $eventId, userId: $userId, excused: $excused) {
    id
    excused
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class RegistrationSetExcusedGQL extends Apollo.Mutation<RegistrationSetExcusedMutation, RegistrationSetExcusedMutationVariables> {
    document = RegistrationSetExcusedDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventCloseDocument = gql`
    mutation EventClose($id: ID!) {
  eventClose(id: $id) {
    id
    amount
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventCloseGQL extends Apollo.Mutation<EventCloseMutation, EventCloseMutationVariables> {
    document = EventCloseDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventReopenDocument = gql`
    mutation EventReopen($id: ID!) {
  eventReopen(id: $id) {
    id
    closedAt
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventReopenGQL extends Apollo.Mutation<EventReopenMutation, EventReopenMutationVariables> {
    document = EventReopenDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const ChargeSetPaidDocument = gql`
    mutation ChargeSetPaid($id: ID!, $paid: Boolean!, $method: PaymentMethod) {
  chargeSetPaid(id: $id, paid: $paid, method: $method) {
    id
    paidAt
    paidMethod
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class ChargeSetPaidGQL extends Apollo.Mutation<ChargeSetPaidMutation, ChargeSetPaidMutationVariables> {
    document = ChargeSetPaidDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const BankEntryAddDocument = gql`
    mutation BankEntryAdd($groupId: ID!, $amount: Float!, $description: String!) {
  bankEntryAdd(groupId: $groupId, amount: $amount, description: $description) {
    id
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class BankEntryAddGQL extends Apollo.Mutation<BankEntryAddMutation, BankEntryAddMutationVariables> {
    document = BankEntryAddDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const BankEntryDeleteDocument = gql`
    mutation BankEntryDelete($id: ID!) {
  bankEntryDelete(id: $id)
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class BankEntryDeleteGQL extends Apollo.Mutation<BankEntryDeleteMutation, BankEntryDeleteMutationVariables> {
    document = BankEntryDeleteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }