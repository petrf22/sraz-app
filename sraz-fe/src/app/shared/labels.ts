import { CombinedGraphQLErrors } from '@apollo/client';
import { EventStatus, MemberType, MembershipStatus, Position, RegistrationStatus } from '../graphql/graphql-types';

/** České popisky výčtových hodnot z API. */
export const MEMBER_TYPE: Record<MemberType, string> = { REGULAR: 'stálý člen', SUBSTITUTE: 'náhradník' };
export const POSITION: Record<Position, string> = { PLAYER: 'hráč', GOALIE: 'brankář' };
export const MEMBERSHIP_STATUS: Record<MembershipStatus, string> = {
  INVITED: 'pozván',
  ACTIVE: 'aktivní',
  DECLINED: 'odmítl',
  REMOVED: 'odebrán',
};
export const EVENT_STATUS: Record<EventStatus, string> = {
  PLANNED: 'naplánováno',
  OPEN: 'pozvánky odeslány',
  LOCKED: 'uzavřeno',
  DONE: 'proběhlo',
  CANCELLED: 'zrušeno',
};
export const REGISTRATION_STATUS: Record<RegistrationStatus, string> = { IN: 'přijde', OUT: 'nepřijde', WAITLIST: 've frontě' };
export const REGISTRATION_COLOR: Record<RegistrationStatus, string> = { IN: 'success', OUT: 'default', WAITLIST: 'warning' };

/** Zpráva z GraphQL chyby (backend posílá česky) nebo obecný text. */
export function gqlErrorMessage(error: unknown): string {
  if (CombinedGraphQLErrors.is(error) && error.errors.length > 0) {
    return error.errors[0].message;
  }
  return 'Něco se nepovedlo, zkuste to prosím znovu.';
}
