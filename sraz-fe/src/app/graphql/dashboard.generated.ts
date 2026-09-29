/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { EventListFieldsFragmentDoc } from './fragments.generated';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type EventStatus =
  | 'CANCELLED'
  | 'DONE'
  | 'LOCKED'
  | 'OPEN'
  | 'PLANNED';

export type GroupInput = {
  description?: string | null | undefined;
  name: string;
};

export type MemberType =
  | 'REGULAR'
  | 'SUBSTITUTE';

export type MembershipStatus =
  | 'ACTIVE'
  | 'DECLINED'
  | 'INVITED'
  | 'REMOVED';

export type Position =
  | 'GOALIE'
  | 'PLAYER';

export type RegistrationStatus =
  | 'IN'
  | 'OUT'
  | 'WAITLIST';

export type MyDashboardQueryVariables = Exact<{ [key: string]: never; }>;


export type MyDashboardQuery = { myUpcomingEvents: Array<{ id: string, name: string, startsAt: string, status: Types.EventStatus, signupOpen: boolean, signupDeadline: string, group: { id: string, name: string }, venue: { id: string, name: string } | null, summary: { players: number, maxPlayers: number, goalies: number, maxGoalies: number, waitlist: number }, myRegistration: { id: string, status: Types.RegistrationStatus, team: { id: string, name: string } | null } | null }>, myGroupInvites: Array<{ id: string, memberType: Types.MemberType, position: Types.Position, group: { id: string, name: string } }>, myGroups: Array<{ id: string, name: string, amOrganizer: boolean }> };

export type GroupInviteRespondMutationVariables = Exact<{
  memberId: string | number;
  accept: boolean;
}>;


export type GroupInviteRespondMutation = { groupInviteRespond: { id: string, status: Types.MembershipStatus } };

export type GroupCreateMutationVariables = Exact<{
  input: Types.GroupInput;
}>;


export type GroupCreateMutation = { groupCreate: { id: string, name: string } };

export const MyDashboardDocument = gql`
    query MyDashboard {
  myUpcomingEvents {
    ...EventListFields
  }
  myGroupInvites {
    id
    memberType
    position
    group {
      id
      name
    }
  }
  myGroups {
    id
    name
    amOrganizer
  }
}
    ${EventListFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class MyDashboardGQL extends Apollo.Query<MyDashboardQuery, MyDashboardQueryVariables> {
    document = MyDashboardDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupInviteRespondDocument = gql`
    mutation GroupInviteRespond($memberId: ID!, $accept: Boolean!) {
  groupInviteRespond(memberId: $memberId, accept: $accept) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupInviteRespondGQL extends Apollo.Mutation<GroupInviteRespondMutation, GroupInviteRespondMutationVariables> {
    document = GroupInviteRespondDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupCreateDocument = gql`
    mutation GroupCreate($input: GroupInput!) {
  groupCreate(input: $input) {
    id
    name
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupCreateGQL extends Apollo.Mutation<GroupCreateMutation, GroupCreateMutationVariables> {
    document = GroupCreateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }