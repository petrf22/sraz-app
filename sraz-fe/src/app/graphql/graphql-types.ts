export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Date: { input: string; output: string; }
  DateTime: { input: string; output: string; }
  Time: { input: string; output: string; }
};

export type Event = {
  __typename?: 'Event';
  durationMinutes: Scalars['Int']['output'];
  group: SportGroup;
  id: Scalars['ID']['output'];
  inviteRegularsHoursBefore: Scalars['Int']['output'];
  inviteSubstitutesHoursBefore: Scalars['Int']['output'];
  maxGoalies: Scalars['Int']['output'];
  maxPlayersPerTeam: Scalars['Int']['output'];
  myRegistration?: Maybe<Registration>;
  name: Scalars['String']['output'];
  note?: Maybe<Scalars['String']['output']>;
  registrations: Array<Registration>;
  regularsInvitedAt?: Maybe<Scalars['DateTime']['output']>;
  signupDeadline: Scalars['DateTime']['output'];
  /** Lze se ještě sám přihlásit/odhlásit (před uzávěrkou, akce není zrušená). */
  signupOpen: Scalars['Boolean']['output'];
  startsAt: Scalars['DateTime']['output'];
  status: EventStatus;
  substitutesInvitedAt?: Maybe<Scalars['DateTime']['output']>;
  summary: EventSummary;
  venue?: Maybe<Venue>;
};

/** Nevyplněné hodnoty = výchozí nastavení (60 min, 10 hráčů na tým, 2 brankáři, pozvánky 96/48 h předem, uzávěrka 24 h předem). */
export type EventInput = {
  durationMinutes?: InputMaybe<Scalars['Int']['input']>;
  inviteRegularsHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  inviteSubstitutesHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  maxGoalies?: InputMaybe<Scalars['Int']['input']>;
  maxPlayersPerTeam?: InputMaybe<Scalars['Int']['input']>;
  name: Scalars['String']['input'];
  note?: InputMaybe<Scalars['String']['input']>;
  signupDeadline?: InputMaybe<Scalars['DateTime']['input']>;
  startsAt: Scalars['DateTime']['input'];
  venueId?: InputMaybe<Scalars['ID']['input']>;
};

export type EventStatus =
  | 'CANCELLED'
  | 'DONE'
  | 'LOCKED'
  | 'OPEN'
  | 'PLANNED';

export type EventSummary = {
  __typename?: 'EventSummary';
  goalies: Scalars['Int']['output'];
  maxGoalies: Scalars['Int']['output'];
  maxPlayers: Scalars['Int']['output'];
  players: Scalars['Int']['output'];
  waitlist: Scalars['Int']['output'];
};

export type GroupInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
};

export type GroupMember = {
  __typename?: 'GroupMember';
  /** E-mail vidí jen organizátor a člen sám. */
  email?: Maybe<Scalars['String']['output']>;
  group: SportGroup;
  id: Scalars['ID']['output'];
  invitedBy?: Maybe<PublicUser>;
  memberType: MemberType;
  organizer: Scalars['Boolean']['output'];
  position: Position;
  respondedAt?: Maybe<Scalars['DateTime']['output']>;
  status: MembershipStatus;
  user?: Maybe<PublicUser>;
};

export type MemberInviteInput = {
  email: Scalars['String']['input'];
  memberType?: InputMaybe<MemberType>;
  position?: InputMaybe<Position>;
};

export type MemberType =
  | 'REGULAR'
  | 'SUBSTITUTE';

export type MemberUpdateInput = {
  memberType?: InputMaybe<MemberType>;
  organizer?: InputMaybe<Scalars['Boolean']['input']>;
  position?: InputMaybe<Position>;
};

export type MembershipStatus =
  | 'ACTIVE'
  | 'DECLINED'
  | 'INVITED'
  | 'REMOVED';

export type Mutation = {
  __typename?: 'Mutation';
  eventCancel: Event;
  eventCreate: Event;
  /** Rozešle pozvánky hned (vrací počet odeslaných e-mailů). */
  eventSendInvitations: Scalars['Int']['output'];
  eventUpdate: Event;
  groupCreate: SportGroup;
  groupInviteRespond: GroupMember;
  groupUpdate: SportGroup;
  memberInvite: GroupMember;
  /** Odebrání člena organizátorem nebo odchod ze skupiny. */
  memberRemove: GroupMember;
  memberUpdate: GroupMember;
  /** Přihlášení/odhlášení přihlášeného uživatele. */
  registrationRespond: Registration;
  /** Organizátor nastaví přihlášku libovolnému členovi (i po uzávěrce). */
  registrationSet: Registration;
  roleSave?: Maybe<Role>;
  teamDelete: Scalars['Boolean']['output'];
  teamSave: Team;
  userCreate: User;
  userDelete?: Maybe<User>;
  userUpdate: User;
  venueDelete: Scalars['Boolean']['output'];
  venueSave: Venue;
};


export type MutationEventCancelArgs = {
  id: Scalars['ID']['input'];
  reason?: InputMaybe<Scalars['String']['input']>;
};


export type MutationEventCreateArgs = {
  groupId: Scalars['ID']['input'];
  input: EventInput;
};


export type MutationEventSendInvitationsArgs = {
  id: Scalars['ID']['input'];
  memberType: MemberType;
};


export type MutationEventUpdateArgs = {
  id: Scalars['ID']['input'];
  input: EventInput;
};


export type MutationGroupCreateArgs = {
  input: GroupInput;
};


export type MutationGroupInviteRespondArgs = {
  accept: Scalars['Boolean']['input'];
  memberId: Scalars['ID']['input'];
};


export type MutationGroupUpdateArgs = {
  id: Scalars['ID']['input'];
  input: GroupInput;
};


export type MutationMemberInviteArgs = {
  groupId: Scalars['ID']['input'];
  input: MemberInviteInput;
};


export type MutationMemberRemoveArgs = {
  id: Scalars['ID']['input'];
};


export type MutationMemberUpdateArgs = {
  id: Scalars['ID']['input'];
  input: MemberUpdateInput;
};


export type MutationRegistrationRespondArgs = {
  eventId: Scalars['ID']['input'];
  status: RegistrationStatus;
  teamId?: InputMaybe<Scalars['ID']['input']>;
};


export type MutationRegistrationSetArgs = {
  eventId: Scalars['ID']['input'];
  position?: InputMaybe<Position>;
  status: RegistrationStatus;
  teamId?: InputMaybe<Scalars['ID']['input']>;
  userId: Scalars['ID']['input'];
};


export type MutationRoleSaveArgs = {
  name: Scalars['String']['input'];
};


export type MutationTeamDeleteArgs = {
  id: Scalars['ID']['input'];
};


export type MutationTeamSaveArgs = {
  groupId: Scalars['ID']['input'];
  input: TeamInput;
};


export type MutationUserCreateArgs = {
  userInput: UserInput;
};


export type MutationUserDeleteArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
};


export type MutationUserUpdateArgs = {
  userInput: UserInput;
};


export type MutationVenueDeleteArgs = {
  id: Scalars['ID']['input'];
};


export type MutationVenueSaveArgs = {
  groupId: Scalars['ID']['input'];
  input: VenueInput;
};

export type Position =
  | 'GOALIE'
  | 'PLAYER';

/** Veřejně viditelné údaje o uživateli (bez e-mailu). */
export type PublicUser = {
  __typename?: 'PublicUser';
  id: Scalars['ID']['output'];
  publicName: Scalars['String']['output'];
};

export type Query = {
  __typename?: 'Query';
  event: Event;
  group: SportGroup;
  /** Pozvánky do skupin, které čekají na moji odpověď. */
  myGroupInvites: Array<GroupMember>;
  myGroups: Array<SportGroup>;
  /** Nadcházející termíny ve všech mých skupinách. */
  myUpcomingEvents: Array<Event>;
  roles: Array<Role>;
  userProfile: User;
};


export type QueryEventArgs = {
  id: Scalars['ID']['input'];
};


export type QueryGroupArgs = {
  id: Scalars['ID']['input'];
};

export type Registration = {
  __typename?: 'Registration';
  createdAt?: Maybe<Scalars['DateTime']['output']>;
  id: Scalars['ID']['output'];
  position: Position;
  queuedAt?: Maybe<Scalars['DateTime']['output']>;
  source: RegistrationSource;
  status: RegistrationStatus;
  team?: Maybe<Team>;
  updatedAt?: Maybe<Scalars['DateTime']['output']>;
  user: PublicUser;
};

export type RegistrationSource =
  | 'EMAIL_LINK'
  | 'ORGANIZER'
  | 'WEB';

export type RegistrationStatus =
  | 'IN'
  | 'OUT'
  | 'WAITLIST';

export type Role = {
  __typename?: 'Role';
  id?: Maybe<Scalars['ID']['output']>;
  name: Scalars['String']['output'];
};

export type SportGroup = {
  __typename?: 'SportGroup';
  amOrganizer: Scalars['Boolean']['output'];
  description?: Maybe<Scalars['String']['output']>;
  events: Array<Event>;
  id: Scalars['ID']['output'];
  members: Array<GroupMember>;
  /** Členství přihlášeného uživatele (null u admina, který není členem). */
  myMembership?: Maybe<GroupMember>;
  name: Scalars['String']['output'];
  teams: Array<Team>;
  venues: Array<Venue>;
};


export type SportGroupEventsArgs = {
  from?: InputMaybe<Scalars['DateTime']['input']>;
  to?: InputMaybe<Scalars['DateTime']['input']>;
};

export type Team = {
  __typename?: 'Team';
  color?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  sortOrder: Scalars['Int']['output'];
};

export type TeamInput = {
  color?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['ID']['input']>;
  name: Scalars['String']['input'];
  sortOrder?: InputMaybe<Scalars['Int']['input']>;
};

export type User = {
  __typename?: 'User';
  email: Scalars['String']['output'];
  emailVerifiedAt?: Maybe<Scalars['DateTime']['output']>;
  firstName?: Maybe<Scalars['String']['output']>;
  id?: Maybe<Scalars['ID']['output']>;
  lastName?: Maybe<Scalars['String']['output']>;
  publicName: Scalars['String']['output'];
  roles: Array<Role>;
};

export type UserInput = {
  email: Scalars['String']['input'];
  firstName?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['ID']['input']>;
  lastName?: InputMaybe<Scalars['String']['input']>;
  password?: InputMaybe<Scalars['String']['input']>;
  publicName: Scalars['String']['input'];
};

export type Venue = {
  __typename?: 'Venue';
  address?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  latitude?: Maybe<Scalars['Float']['output']>;
  longitude?: Maybe<Scalars['Float']['output']>;
  mapUrl?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
};

export type VenueInput = {
  address?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['ID']['input']>;
  latitude?: InputMaybe<Scalars['Float']['input']>;
  longitude?: InputMaybe<Scalars['Float']['input']>;
  mapUrl?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
};
