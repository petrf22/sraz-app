/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
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

export type RegistrationStatus =
  | 'IN'
  | 'OUT'
  | 'WAITLIST';

export type TeamFieldsFragment = { id: string, name: string, color: string | null, sortOrder: number };

export type VenueFieldsFragment = { id: string, name: string, address: string | null, mapUrl: string | null, latitude: number | null, longitude: number | null };

export type SummaryFieldsFragment = { players: number, maxPlayers: number, goalies: number, maxGoalies: number, waitlist: number };

export type EventListFieldsFragment = { id: string, name: string, startsAt: string, status: Types.EventStatus, signupOpen: boolean, signupDeadline: string, group: { id: string, name: string }, venue: { id: string, name: string } | null, summary: { players: number, maxPlayers: number, goalies: number, maxGoalies: number, waitlist: number }, myRegistration: { id: string, status: Types.RegistrationStatus, team: { id: string, name: string } | null } | null };

export type MemberFieldsFragment = { id: string, email: string | null, memberType: Types.MemberType, position: Types.Position, organizer: boolean, status: Types.MembershipStatus, respondedAt: string | null, user: { id: string, publicName: string } | null };

export const TeamFieldsFragmentDoc = gql`
    fragment TeamFields on Team {
  id
  name
  color
  sortOrder
}
    `;
export const VenueFieldsFragmentDoc = gql`
    fragment VenueFields on Venue {
  id
  name
  address
  mapUrl
  latitude
  longitude
}
    `;
export const SummaryFieldsFragmentDoc = gql`
    fragment SummaryFields on EventSummary {
  players
  maxPlayers
  goalies
  maxGoalies
  waitlist
}
    `;
export const EventListFieldsFragmentDoc = gql`
    fragment EventListFields on Event {
  id
  name
  startsAt
  status
  signupOpen
  signupDeadline
  group {
    id
    name
  }
  venue {
    id
    name
  }
  summary {
    ...SummaryFields
  }
  myRegistration {
    id
    status
    team {
      id
      name
    }
  }
}
    ${SummaryFieldsFragmentDoc}`;
export const MemberFieldsFragmentDoc = gql`
    fragment MemberFields on GroupMember {
  id
  email
  memberType
  position
  organizer
  status
  respondedAt
  user {
    id
    publicName
  }
}
    `;