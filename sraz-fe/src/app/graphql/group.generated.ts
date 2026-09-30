/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { SummaryFieldsFragmentDoc, EventListFieldsFragmentDoc, MemberFieldsFragmentDoc, VenueFieldsFragmentDoc, TeamFieldsFragmentDoc } from './fragments.generated';
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
  /** Pokuty za pozdní odhlášení a neúčast (celý podíl). */
  finesEnabled?: boolean | null | undefined;
  /** IBAN pro QR platby (prázdné = bez účtu). */
  iban?: string | null | undefined;
  name: string;
};

export type MemberInviteInput = {
  email: string;
  memberType?: MemberType | null | undefined;
  position?: Position | null | undefined;
};

export type MemberType =
  | 'REGULAR'
  | 'SUBSTITUTE';

export type MemberUpdateInput = {
  memberType?: MemberType | null | undefined;
  organizer?: boolean | null | undefined;
  position?: Position | null | undefined;
};

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

export type TeamInput = {
  color?: string | null | undefined;
  id?: string | number | null | undefined;
  name: string;
  sortOrder?: number | null | undefined;
};

export type VenueInput = {
  address?: string | null | undefined;
  id?: string | number | null | undefined;
  latitude?: number | null | undefined;
  longitude?: number | null | undefined;
  mapUrl?: string | null | undefined;
  name: string;
};

export type GroupDetailQueryVariables = Exact<{
  id: string | number;
  from?: string | null | undefined;
}>;


export type GroupDetailQuery = { group: { id: string, name: string, description: string | null, amOrganizer: boolean, myMembership: { id: string } | null, teams: Array<{ id: string, name: string, color: string | null, sortOrder: number }>, venues: Array<{ id: string, name: string, address: string | null, mapUrl: string | null, latitude: number | null, longitude: number | null }>, members: Array<{ id: string, email: string | null, memberType: Types.MemberType, position: Types.Position, organizer: boolean, status: Types.MembershipStatus, respondedAt: string | null, user: { id: string, publicName: string } | null }>, events: Array<{ id: string, name: string, startsAt: string, status: Types.EventStatus, signupOpen: boolean, signupDeadline: string, group: { id: string, name: string }, venue: { id: string, name: string } | null, summary: { players: number, maxPlayers: number, goalies: number, maxGoalies: number, waitlist: number }, myRegistration: { id: string, status: Types.RegistrationStatus, team: { id: string, name: string } | null } | null }> } };

export type GroupUpdateMutationVariables = Exact<{
  id: string | number;
  input: Types.GroupInput;
}>;


export type GroupUpdateMutation = { groupUpdate: { id: string, name: string, description: string | null } };

export type TeamSaveMutationVariables = Exact<{
  groupId: string | number;
  input: Types.TeamInput;
}>;


export type TeamSaveMutation = { teamSave: { id: string, name: string, color: string | null, sortOrder: number } };

export type TeamDeleteMutationVariables = Exact<{
  id: string | number;
}>;


export type TeamDeleteMutation = { teamDelete: boolean };

export type VenueSaveMutationVariables = Exact<{
  groupId: string | number;
  input: Types.VenueInput;
}>;


export type VenueSaveMutation = { venueSave: { id: string, name: string, address: string | null, mapUrl: string | null, latitude: number | null, longitude: number | null } };

export type VenueDeleteMutationVariables = Exact<{
  id: string | number;
}>;


export type VenueDeleteMutation = { venueDelete: boolean };

export type MemberInviteMutationVariables = Exact<{
  groupId: string | number;
  input: Types.MemberInviteInput;
}>;


export type MemberInviteMutation = { memberInvite: { id: string, email: string | null, memberType: Types.MemberType, position: Types.Position, organizer: boolean, status: Types.MembershipStatus, respondedAt: string | null, user: { id: string, publicName: string } | null } };

export type MemberUpdateMutationVariables = Exact<{
  id: string | number;
  input: Types.MemberUpdateInput;
}>;


export type MemberUpdateMutation = { memberUpdate: { id: string, email: string | null, memberType: Types.MemberType, position: Types.Position, organizer: boolean, status: Types.MembershipStatus, respondedAt: string | null, user: { id: string, publicName: string } | null } };

export type MemberRemoveMutationVariables = Exact<{
  id: string | number;
}>;


export type MemberRemoveMutation = { memberRemove: { id: string, email: string | null, memberType: Types.MemberType, position: Types.Position, organizer: boolean, status: Types.MembershipStatus, respondedAt: string | null, user: { id: string, publicName: string } | null } };

export const GroupDetailDocument = gql`
    query GroupDetail($id: ID!, $from: DateTime) {
  group(id: $id) {
    id
    name
    description
    amOrganizer
    myMembership {
      id
    }
    teams {
      ...TeamFields
    }
    venues {
      ...VenueFields
    }
    members {
      ...MemberFields
    }
    events(from: $from) {
      ...EventListFields
    }
  }
}
    ${TeamFieldsFragmentDoc}
${VenueFieldsFragmentDoc}
${MemberFieldsFragmentDoc}
${EventListFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupDetailGQL extends Apollo.Query<GroupDetailQuery, GroupDetailQueryVariables> {
    document = GroupDetailDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupUpdateDocument = gql`
    mutation GroupUpdate($id: ID!, $input: GroupInput!) {
  groupUpdate(id: $id, input: $input) {
    id
    name
    description
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupUpdateGQL extends Apollo.Mutation<GroupUpdateMutation, GroupUpdateMutationVariables> {
    document = GroupUpdateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const TeamSaveDocument = gql`
    mutation TeamSave($groupId: ID!, $input: TeamInput!) {
  teamSave(groupId: $groupId, input: $input) {
    ...TeamFields
  }
}
    ${TeamFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class TeamSaveGQL extends Apollo.Mutation<TeamSaveMutation, TeamSaveMutationVariables> {
    document = TeamSaveDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const TeamDeleteDocument = gql`
    mutation TeamDelete($id: ID!) {
  teamDelete(id: $id)
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class TeamDeleteGQL extends Apollo.Mutation<TeamDeleteMutation, TeamDeleteMutationVariables> {
    document = TeamDeleteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const VenueSaveDocument = gql`
    mutation VenueSave($groupId: ID!, $input: VenueInput!) {
  venueSave(groupId: $groupId, input: $input) {
    ...VenueFields
  }
}
    ${VenueFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class VenueSaveGQL extends Apollo.Mutation<VenueSaveMutation, VenueSaveMutationVariables> {
    document = VenueSaveDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const VenueDeleteDocument = gql`
    mutation VenueDelete($id: ID!) {
  venueDelete(id: $id)
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class VenueDeleteGQL extends Apollo.Mutation<VenueDeleteMutation, VenueDeleteMutationVariables> {
    document = VenueDeleteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const MemberInviteDocument = gql`
    mutation MemberInvite($groupId: ID!, $input: MemberInviteInput!) {
  memberInvite(groupId: $groupId, input: $input) {
    ...MemberFields
  }
}
    ${MemberFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class MemberInviteGQL extends Apollo.Mutation<MemberInviteMutation, MemberInviteMutationVariables> {
    document = MemberInviteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const MemberUpdateDocument = gql`
    mutation MemberUpdate($id: ID!, $input: MemberUpdateInput!) {
  memberUpdate(id: $id, input: $input) {
    ...MemberFields
  }
}
    ${MemberFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class MemberUpdateGQL extends Apollo.Mutation<MemberUpdateMutation, MemberUpdateMutationVariables> {
    document = MemberUpdateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const MemberRemoveDocument = gql`
    mutation MemberRemove($id: ID!) {
  memberRemove(id: $id) {
    ...MemberFields
  }
}
    ${MemberFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class MemberRemoveGQL extends Apollo.Mutation<MemberRemoveMutation, MemberRemoveMutationVariables> {
    document = MemberRemoveDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }