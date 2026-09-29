/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { TeamFieldsFragmentDoc, SummaryFieldsFragmentDoc, VenueFieldsFragmentDoc } from './fragments.generated';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
/** Nevyplněné hodnoty = výchozí nastavení (60 min, 10 hráčů na tým, 2 brankáři, pozvánky 96/48 h předem, uzávěrka 24 h předem). */
export type EventInput = {
  durationMinutes?: number | null | undefined;
  inviteRegularsHoursBefore?: number | null | undefined;
  inviteSubstitutesHoursBefore?: number | null | undefined;
  maxGoalies?: number | null | undefined;
  maxPlayersPerTeam?: number | null | undefined;
  name: string;
  note?: string | null | undefined;
  reminderHoursBefore?: number | null | undefined;
  signupDeadline?: string | null | undefined;
  startsAt: string;
  venueId?: string | number | null | undefined;
};

export type EventStatus =
  | 'CANCELLED'
  | 'DONE'
  | 'LOCKED'
  | 'OPEN'
  | 'PLANNED';

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

export type RegistrationSource =
  | 'EMAIL_LINK'
  | 'ORGANIZER'
  | 'WEB';

export type RegistrationStatus =
  | 'IN'
  | 'OUT'
  | 'WAITLIST';

export type EventDetailQueryVariables = Exact<{
  id: string | number;
}>;


export type EventDetailQuery = { event: { id: string, name: string, startsAt: string, durationMinutes: number, maxPlayersPerTeam: number, maxGoalies: number, signupDeadline: string, inviteRegularsHoursBefore: number, inviteSubstitutesHoursBefore: number, regularsInvitedAt: string | null, substitutesInvitedAt: string | null, status: Types.EventStatus, note: string | null, signupOpen: boolean, detached: boolean, reminderHoursBefore: number | null, series: { id: string, name: string } | null, venue: { id: string, name: string, address: string | null, mapUrl: string | null, latitude: number | null, longitude: number | null } | null, summary: { players: number, maxPlayers: number, goalies: number, maxGoalies: number, waitlist: number }, myRegistration: { id: string, status: Types.RegistrationStatus, team: { id: string } | null } | null, registrations: Array<{ id: string, status: Types.RegistrationStatus, position: Types.Position, source: Types.RegistrationSource, queuedAt: string | null, updatedAt: string | null, user: { id: string, publicName: string }, team: { id: string } | null }>, group: { id: string, name: string, amOrganizer: boolean, teams: Array<{ id: string, name: string, color: string | null, sortOrder: number }>, venues: Array<{ id: string, name: string, address: string | null, mapUrl: string | null, latitude: number | null, longitude: number | null }>, members: Array<{ id: string, status: Types.MembershipStatus, position: Types.Position, memberType: Types.MemberType, user: { id: string, publicName: string } | null }> } } };

export type EventCreateMutationVariables = Exact<{
  groupId: string | number;
  input: Types.EventInput;
}>;


export type EventCreateMutation = { eventCreate: { id: string } };

export type EventUpdateMutationVariables = Exact<{
  id: string | number;
  input: Types.EventInput;
}>;


export type EventUpdateMutation = { eventUpdate: { id: string } };

export type EventCancelMutationVariables = Exact<{
  id: string | number;
  reason?: string | null | undefined;
}>;


export type EventCancelMutation = { eventCancel: { id: string, status: Types.EventStatus } };

export type EventSendInvitationsMutationVariables = Exact<{
  id: string | number;
  memberType: Types.MemberType;
}>;


export type EventSendInvitationsMutation = { eventSendInvitations: number };

export type RegistrationRespondMutationVariables = Exact<{
  eventId: string | number;
  status: Types.RegistrationStatus;
  teamId?: string | number | null | undefined;
}>;


export type RegistrationRespondMutation = { registrationRespond: { id: string, status: Types.RegistrationStatus } };

export type RegistrationSetMutationVariables = Exact<{
  eventId: string | number;
  userId: string | number;
  status: Types.RegistrationStatus;
  teamId?: string | number | null | undefined;
  position?: Types.Position | null | undefined;
}>;


export type RegistrationSetMutation = { registrationSet: { id: string, status: Types.RegistrationStatus } };

export const EventDetailDocument = gql`
    query EventDetail($id: ID!) {
  event(id: $id) {
    id
    name
    startsAt
    durationMinutes
    maxPlayersPerTeam
    maxGoalies
    signupDeadline
    inviteRegularsHoursBefore
    inviteSubstitutesHoursBefore
    regularsInvitedAt
    substitutesInvitedAt
    status
    note
    signupOpen
    detached
    reminderHoursBefore
    series {
      id
      name
    }
    venue {
      ...VenueFields
    }
    summary {
      ...SummaryFields
    }
    myRegistration {
      id
      status
      team {
        id
      }
    }
    registrations {
      id
      status
      position
      source
      queuedAt
      updatedAt
      user {
        id
        publicName
      }
      team {
        id
      }
    }
    group {
      id
      name
      amOrganizer
      teams {
        ...TeamFields
      }
      venues {
        ...VenueFields
      }
      members {
        id
        status
        position
        memberType
        user {
          id
          publicName
        }
      }
    }
  }
}
    ${VenueFieldsFragmentDoc}
${SummaryFieldsFragmentDoc}
${TeamFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class EventDetailGQL extends Apollo.Query<EventDetailQuery, EventDetailQueryVariables> {
    document = EventDetailDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventCreateDocument = gql`
    mutation EventCreate($groupId: ID!, $input: EventInput!) {
  eventCreate(groupId: $groupId, input: $input) {
    id
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventCreateGQL extends Apollo.Mutation<EventCreateMutation, EventCreateMutationVariables> {
    document = EventCreateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventUpdateDocument = gql`
    mutation EventUpdate($id: ID!, $input: EventInput!) {
  eventUpdate(id: $id, input: $input) {
    id
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventUpdateGQL extends Apollo.Mutation<EventUpdateMutation, EventUpdateMutationVariables> {
    document = EventUpdateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventCancelDocument = gql`
    mutation EventCancel($id: ID!, $reason: String) {
  eventCancel(id: $id, reason: $reason) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventCancelGQL extends Apollo.Mutation<EventCancelMutation, EventCancelMutationVariables> {
    document = EventCancelDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const EventSendInvitationsDocument = gql`
    mutation EventSendInvitations($id: ID!, $memberType: MemberType!) {
  eventSendInvitations(id: $id, memberType: $memberType)
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class EventSendInvitationsGQL extends Apollo.Mutation<EventSendInvitationsMutation, EventSendInvitationsMutationVariables> {
    document = EventSendInvitationsDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const RegistrationRespondDocument = gql`
    mutation RegistrationRespond($eventId: ID!, $status: RegistrationStatus!, $teamId: ID) {
  registrationRespond(eventId: $eventId, status: $status, teamId: $teamId) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class RegistrationRespondGQL extends Apollo.Mutation<RegistrationRespondMutation, RegistrationRespondMutationVariables> {
    document = RegistrationRespondDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const RegistrationSetDocument = gql`
    mutation RegistrationSet($eventId: ID!, $userId: ID!, $status: RegistrationStatus!, $teamId: ID, $position: Position) {
  registrationSet(
    eventId: $eventId
    userId: $userId
    status: $status
    teamId: $teamId
    position: $position
  ) {
    id
    status
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class RegistrationSetGQL extends Apollo.Mutation<RegistrationSetMutation, RegistrationSetMutationVariables> {
    document = RegistrationSetDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }