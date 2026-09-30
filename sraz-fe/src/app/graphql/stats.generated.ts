/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import * as Types from './graphql-types';

import { gql } from 'apollo-angular';
import { Injectable } from '@angular/core';
import * as Apollo from 'apollo-angular';
export type MemberType =
  | 'REGULAR'
  | 'SUBSTITUTE';

export type Position =
  | 'GOALIE'
  | 'PLAYER';

export type GroupStatsQueryVariables = Exact<{
  id: string | number;
  from?: string | null | undefined;
  to?: string | null | undefined;
}>;


export type GroupStatsQuery = { group: { id: string, name: string, stats: Array<{ memberType: Types.MemberType, position: Types.Position, events: number, attended: number, noShow: number, declined: number, noAnswer: number, attendanceRate: number, goals: number, assists: number, charged: number, paid: number, user: { id: string, publicName: string } }> } };

export type ScoreSetMutationVariables = Exact<{
  eventId: string | number;
  userId: string | number;
  goals: number;
  assists: number;
}>;


export type ScoreSetMutation = { scoreSet: { id: string, goals: number, assists: number } };

export const GroupStatsDocument = gql`
    query GroupStats($id: ID!, $from: Date, $to: Date) {
  group(id: $id) {
    id
    name
    stats(from: $from, to: $to) {
      user {
        id
        publicName
      }
      memberType
      position
      events
      attended
      noShow
      declined
      noAnswer
      attendanceRate
      goals
      assists
      charged
      paid
    }
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class GroupStatsGQL extends Apollo.Query<GroupStatsQuery, GroupStatsQueryVariables> {
    document = GroupStatsDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }
export const ScoreSetDocument = gql`
    mutation ScoreSet($eventId: ID!, $userId: ID!, $goals: Int!, $assists: Int!) {
  scoreSet(eventId: $eventId, userId: $userId, goals: $goals, assists: $assists) {
    id
    goals
    assists
  }
}
    `;

  @Injectable({
    providedIn: 'root'
  })
  export class ScoreSetGQL extends Apollo.Mutation<ScoreSetMutation, ScoreSetMutationVariables> {
    document = ScoreSetDocument;
    
    constructor(apollo: Apollo.Apollo) {
      super(apollo);
    }
  }