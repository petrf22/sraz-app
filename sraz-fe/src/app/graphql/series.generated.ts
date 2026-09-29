/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type DayOfWeek =
  | 'FRIDAY'
  | 'MONDAY'
  | 'SATURDAY'
  | 'SUNDAY'
  | 'THURSDAY'
  | 'TUESDAY'
  | 'WEDNESDAY';

export type EventStatus =
  | 'CANCELLED'
  | 'DONE'
  | 'LOCKED'
  | 'OPEN'
  | 'PLANNED';

/** Nevyplněné hodnoty = výchozí nastavení jako u jednorázové akce; uzávěrka je v hodinách před začátkem. */
export type PeriodInput = {
  daysOfWeek: Array<DayOfWeek>;
  deadlineHoursBefore?: number | null | undefined;
  durationMinutes?: number | null | undefined;
  intervalCount?: number | null | undefined;
  inviteRegularsHoursBefore?: number | null | undefined;
  inviteSubstitutesHoursBefore?: number | null | undefined;
  maxGoalies?: number | null | undefined;
  maxPlayersPerTeam?: number | null | undefined;
  monthWeeks?: Array<number> | null | undefined;
  note?: string | null | undefined;
  recurrence: Recurrence;
  reminderHoursBefore?: number | null | undefined;
  /** HH:mm */
  startTime: string;
  validFrom: string;
  validTo: string;
  venueId?: string | number | null | undefined;
};

export type Recurrence =
  | 'MONTHLY'
  | 'WEEKLY';

export type RegistrationStatus =
  | 'IN'
  | 'OUT'
  | 'WAITLIST';

export type PeriodFieldsFragment = { id: string, validFrom: string, validTo: string, recurrence: Types.Recurrence, intervalCount: number, daysOfWeek: Array<Types.DayOfWeek>, monthWeeks: Array<number>, startTime: string, durationMinutes: number, maxPlayersPerTeam: number, maxGoalies: number, deadlineHoursBefore: number, inviteRegularsHoursBefore: number, inviteSubstitutesHoursBefore: number, reminderHoursBefore: number | null, note: string | null, venue: { id: string, name: string } | null };

export type GroupSeriesQueryVariables = Exact<{
  id: string | number;
}>;


export type GroupSeriesQuery = { group: { id: string, amOrganizer: boolean, venues: Array<{ id: string, name: string }>, series: Array<{ id: string, name: string, periods: Array<{ id: string, validFrom: string, validTo: string, recurrence: Types.Recurrence, intervalCount: number, daysOfWeek: Array<Types.DayOfWeek>, monthWeeks: Array<number>, startTime: string, durationMinutes: number, maxPlayersPerTeam: number, maxGoalies: number, deadlineHoursBefore: number, inviteRegularsHoursBefore: number, inviteSubstitutesHoursBefore: number, reminderHoursBefore: number | null, note: string | null, venue: { id: string, name: string } | null }> }> } };

export type GroupCalendarQueryVariables = Exact<{
  id: string | number;
  from: string;
  to: string;
}>;


export type GroupCalendarQuery = { group: { id: string, events: Array<{ id: string, name: string, startsAt: string, status: Types.EventStatus, summary: { players: number, maxPlayers: number }, myRegistration: { id: string, status: Types.RegistrationStatus } | null }> } };

export type PeriodPreviewQueryVariables = Exact<{
  input: Types.PeriodInput;
}>;


export type PeriodPreviewQuery = { periodPreview: Array<string> };

export type SeriesCreateMutationVariables = Exact<{
  groupId: string | number;
  name: string;
}>;


export type SeriesCreateMutation = { seriesCreate: { id: string, name: string } };

export type SeriesRenameMutationVariables = Exact<{
  id: string | number;
  name: string;
}>;


export type SeriesRenameMutation = { seriesRename: { id: string, name: string } };

export type SeriesDeleteMutationVariables = Exact<{
  id: string | number;
}>;


export type SeriesDeleteMutation = { seriesDelete: { created: number, updated: number, removed: number, kept: number } };

export type PeriodSaveMutationVariables = Exact<{
  seriesId: string | number;
  id?: string | number | null | undefined;
  input: Types.PeriodInput;
}>;


export type PeriodSaveMutation = { periodSave: { created: number, updated: number, removed: number, kept: number } };

export type PeriodDeleteMutationVariables = Exact<{
  id: string | number;
}>;


export type PeriodDeleteMutation = { periodDelete: { created: number, updated: number, removed: number, kept: number } };

export const PeriodFieldsFragmentDoc = gql`
    fragment PeriodFields on SeriesPeriod {
  id
  validFrom
  validTo
  recurrence
  intervalCount
  daysOfWeek
  monthWeeks
  startTime
  durationMinutes
  venue {
    id
    name
  }
  maxPlayersPerTeam
  maxGoalies
  deadlineHoursBefore
  inviteRegularsHoursBefore
  inviteSubstitutesHoursBefore
  reminderHoursBefore
  note
}
    `;
export const GroupSeriesDocument = gql`
    query GroupSeries($id: ID!) {
  group(id: $id) {
    id
    amOrganizer
    venues {
      id
      name
    }
    series {
      id
      name
      periods {
        ...PeriodFields
      }
    }
  }
}
    ${PeriodFieldsFragmentDoc}`;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupSeriesGQL extends Apollo.Query<GroupSeriesQuery, GroupSeriesQueryVariables> {
    document = GroupSeriesDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const GroupCalendarDocument = gql`
    query GroupCalendar($id: ID!, $from: DateTime!, $to: DateTime!) {
  group(id: $id) {
    id
    events(from: $from, to: $to) {
      id
      name
      startsAt
      status
      summary {
        players
        maxPlayers
      }
      myRegistration {
        id
        status
      }
    }
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupCalendarGQL extends Apollo.Query<GroupCalendarQuery, GroupCalendarQueryVariables> {
    document = GroupCalendarDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const PeriodPreviewDocument = gql`
    query PeriodPreview($input: PeriodInput!) {
  periodPreview(input: $input)
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class PeriodPreviewGQL extends Apollo.Query<PeriodPreviewQuery, PeriodPreviewQueryVariables> {
    document = PeriodPreviewDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const SeriesCreateDocument = gql`
    mutation SeriesCreate($groupId: ID!, $name: String!) {
  seriesCreate(groupId: $groupId, name: $name) {
    id
    name
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class SeriesCreateGQL extends Apollo.Mutation<SeriesCreateMutation, SeriesCreateMutationVariables> {
    document = SeriesCreateDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const SeriesRenameDocument = gql`
    mutation SeriesRename($id: ID!, $name: String!) {
  seriesRename(id: $id, name: $name) {
    id
    name
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class SeriesRenameGQL extends Apollo.Mutation<SeriesRenameMutation, SeriesRenameMutationVariables> {
    document = SeriesRenameDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const SeriesDeleteDocument = gql`
    mutation SeriesDelete($id: ID!) {
  seriesDelete(id: $id) {
    created
    updated
    removed
    kept
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class SeriesDeleteGQL extends Apollo.Mutation<SeriesDeleteMutation, SeriesDeleteMutationVariables> {
    document = SeriesDeleteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const PeriodSaveDocument = gql`
    mutation PeriodSave($seriesId: ID!, $id: ID, $input: PeriodInput!) {
  periodSave(seriesId: $seriesId, id: $id, input: $input) {
    created
    updated
    removed
    kept
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class PeriodSaveGQL extends Apollo.Mutation<PeriodSaveMutation, PeriodSaveMutationVariables> {
    document = PeriodSaveDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const PeriodDeleteDocument = gql`
    mutation PeriodDelete($id: ID!) {
  periodDelete(id: $id) {
    created
    updated
    removed
    kept
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class PeriodDeleteGQL extends Apollo.Mutation<PeriodDeleteMutation, PeriodDeleteMutationVariables> {
    document = PeriodDeleteDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }