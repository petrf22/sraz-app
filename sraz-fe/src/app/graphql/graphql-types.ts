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

export type BankEntry = {
  __typename?: 'BankEntry';
  /** Kladně příjem, záporně výdaj (Kč). */
  amount: Scalars['Float']['output'];
  createdAt?: Maybe<Scalars['DateTime']['output']>;
  createdBy?: Maybe<PublicUser>;
  description: Scalars['String']['output'];
  event?: Maybe<Event>;
  id: Scalars['ID']['output'];
  kind: BankEntryKind;
};

export type BankEntryKind =
  | 'EVENT'
  | 'MANUAL';

/** Pohyb na účtu skupiny stažený z Fio API. */
export type BankTransaction = {
  __typename?: 'BankTransaction';
  /** Kladně příchozí, záporně odchozí (v měně účtu). */
  amount: Scalars['Float']['output'];
  bookedOn: Scalars['Date']['output'];
  charge?: Maybe<Charge>;
  counterAccount?: Maybe<Scalars['String']['output']>;
  counterName?: Maybe<Scalars['String']['output']>;
  currency?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  message?: Maybe<Scalars['String']['output']>;
  /** Proč pohyb nešel spárovat (nebo přeplatek). */
  note?: Maybe<Scalars['String']['output']>;
  status: BankTransactionStatus;
  variableSymbol?: Maybe<Scalars['String']['output']>;
};

export type BankTransactionStatus =
  | 'IGNORED'
  | 'MATCHED'
  | 'UNMATCHED';

/** Platba účastníka za termín; variabilní symbol = id. */
export type Charge = {
  __typename?: 'Charge';
  amount: Scalars['Float']['output'];
  event: Event;
  /** IBAN skupiny pro QR platbu (null = platit organizátorovi). */
  iban?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  kind: ChargeKind;
  paidAt?: Maybe<Scalars['DateTime']['output']>;
  paidMethod?: Maybe<PaymentMethod>;
  reason: ChargeReason;
  user: PublicUser;
};

export type ChargeKind =
  | 'GOALIE'
  | 'REGULAR'
  | 'SUBSTITUTE';

/** Za co se platí: odehraná akce, nebo pokuta (celý podíl) za pozdní odhlášení / neomluvenou neúčast. */
export type ChargeReason =
  | 'LATE_CANCEL'
  | 'NO_SHOW'
  | 'PLAYED';

export type DayOfWeek =
  | 'FRIDAY'
  | 'MONDAY'
  | 'SATURDAY'
  | 'SUNDAY'
  | 'THURSDAY'
  | 'TUESDAY'
  | 'WEDNESDAY';

export type Event = {
  __typename?: 'Event';
  /** Platby: organizátor vidí všechny, člen jen svou. */
  charges: Array<Charge>;
  /** Kdy bylo vyúčtování uzavřeno (null = otevřené). */
  closedAt?: Maybe<Scalars['DateTime']['output']>;
  /** Organizátor termín ručně upravil – přegenerování období ho nepřepíše. */
  detached: Scalars['Boolean']['output'];
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
  /** Cena ledu/hřiště za hodinu v Kč (null = zdarma). */
  pricePerHour?: Maybe<Scalars['Float']['output']>;
  registrations: Array<Registration>;
  /** Poplatek stálého člena – platí max(poplatek, podíl). */
  regularFee?: Maybe<Scalars['Float']['output']>;
  regularsInvitedAt?: Maybe<Scalars['DateTime']['output']>;
  /** Připomínka přihlášeným X hodin před začátkem (null = bez připomínky). */
  reminderHoursBefore?: Maybe<Scalars['Int']['output']>;
  /** Série, ze které termín vznikl (null = jednorázová akce). */
  series?: Maybe<EventSeries>;
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
  pricePerHour?: InputMaybe<Scalars['Float']['input']>;
  regularFee?: InputMaybe<Scalars['Float']['input']>;
  reminderHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  signupDeadline?: InputMaybe<Scalars['DateTime']['input']>;
  startsAt: Scalars['DateTime']['input'];
  venueId?: InputMaybe<Scalars['ID']['input']>;
};

export type EventSeries = {
  __typename?: 'EventSeries';
  id: Scalars['ID']['output'];
  name: Scalars['String']['output'];
  periods: Array<SeriesPeriod>;
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

/** Výsledek stažení pohybů. */
export type FioSyncResult = {
  __typename?: 'FioSyncResult';
  /** Nově uložených. */
  created: Scalars['Int']['output'];
  /** Pohybů ve staženém období. */
  fetched: Scalars['Int']['output'];
  matched: Scalars['Int']['output'];
  unmatched: Scalars['Int']['output'];
};

export type GroupInput = {
  description?: InputMaybe<Scalars['String']['input']>;
  /** Pokuty za pozdní odhlášení a neúčast (celý podíl). */
  finesEnabled?: InputMaybe<Scalars['Boolean']['input']>;
  /** IBAN pro QR platby (prázdné = bez účtu). */
  iban?: InputMaybe<Scalars['String']['input']>;
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
  /** Potvrzení účasti po akci (jen u přihlášeného člena). */
  attendanceSet: Registration;
  /** Ruční pohyb v banku (kladně příjem, záporně výdaj). */
  bankEntryAdd: BankEntry;
  bankEntryDelete: Scalars['Boolean']['output'];
  /** Ruční spárování pohybu s platbou. */
  bankTransactionAssign: BankTransaction;
  /** Pohyb nesouvisí s platbami – odložit. */
  bankTransactionIgnore: BankTransaction;
  chargeSetPaid: Charge;
  eventCancel: Event;
  /** Uzavře vyúčtování: vzniknou platby a pohyb v banku. */
  eventClose: Array<Charge>;
  eventCreate: Event;
  /** Znovu otevře vyúčtování – jen dokud nikdo nezaplatil. */
  eventReopen: Event;
  /** Rozešle pozvánky hned (vrací počet odeslaných e-mailů). */
  eventSendInvitations: Scalars['Int']['output'];
  eventUpdate: Event;
  /** Stáhne nové pohyby a spáruje platby. */
  fioSync: FioSyncResult;
  groupCreate: SportGroup;
  groupInviteRespond: GroupMember;
  /** Uloží token Fio API (jen pro čtení) a hned stáhne pohyby; null/prázdný token = odpojit (vrací null). */
  groupSetFioToken?: Maybe<FioSyncResult>;
  groupUpdate: SportGroup;
  memberInvite: GroupMember;
  /** Odebrání člena organizátorem nebo odchod ze skupiny. */
  memberRemove: GroupMember;
  memberUpdate: GroupMember;
  periodDelete: SyncResult;
  /** Uloží období (id = null → nové) a srovná s ním budoucí termíny. */
  periodSave: SyncResult;
  /** Přihlášení/odhlášení přihlášeného uživatele. */
  registrationRespond: Registration;
  /** Organizátor nastaví přihlášku libovolnému členovi (i po uzávěrce). */
  registrationSet: Registration;
  /** Omluví pozdní odhlášení / neúčast – bez pokuty. */
  registrationSetExcused: Registration;
  roleSave?: Maybe<Role>;
  /** Góly a asistence hráče na termínu. */
  scoreSet: Registration;
  seriesCreate: EventSeries;
  /** Smaže sérii: budoucí termíny bez aktivity zmizí, ostatní zůstanou jako samostatné akce. */
  seriesDelete: SyncResult;
  seriesRename: EventSeries;
  teamDelete: Scalars['Boolean']['output'];
  teamSave: Team;
  userCreate: User;
  userDelete?: Maybe<User>;
  userUpdate: User;
  venueDelete: Scalars['Boolean']['output'];
  venueSave: Venue;
};


export type MutationAttendanceSetArgs = {
  attended: Scalars['Boolean']['input'];
  eventId: Scalars['ID']['input'];
  userId: Scalars['ID']['input'];
};


export type MutationBankEntryAddArgs = {
  amount: Scalars['Float']['input'];
  description: Scalars['String']['input'];
  groupId: Scalars['ID']['input'];
};


export type MutationBankEntryDeleteArgs = {
  id: Scalars['ID']['input'];
};


export type MutationBankTransactionAssignArgs = {
  chargeId: Scalars['ID']['input'];
  id: Scalars['ID']['input'];
};


export type MutationBankTransactionIgnoreArgs = {
  id: Scalars['ID']['input'];
};


export type MutationChargeSetPaidArgs = {
  id: Scalars['ID']['input'];
  method?: InputMaybe<PaymentMethod>;
  paid: Scalars['Boolean']['input'];
};


export type MutationEventCancelArgs = {
  id: Scalars['ID']['input'];
  reason?: InputMaybe<Scalars['String']['input']>;
};


export type MutationEventCloseArgs = {
  id: Scalars['ID']['input'];
};


export type MutationEventCreateArgs = {
  groupId: Scalars['ID']['input'];
  input: EventInput;
};


export type MutationEventReopenArgs = {
  id: Scalars['ID']['input'];
};


export type MutationEventSendInvitationsArgs = {
  id: Scalars['ID']['input'];
  memberType: MemberType;
};


export type MutationEventUpdateArgs = {
  id: Scalars['ID']['input'];
  input: EventInput;
};


export type MutationFioSyncArgs = {
  groupId: Scalars['ID']['input'];
};


export type MutationGroupCreateArgs = {
  input: GroupInput;
};


export type MutationGroupInviteRespondArgs = {
  accept: Scalars['Boolean']['input'];
  memberId: Scalars['ID']['input'];
};


export type MutationGroupSetFioTokenArgs = {
  groupId: Scalars['ID']['input'];
  token?: InputMaybe<Scalars['String']['input']>;
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


export type MutationPeriodDeleteArgs = {
  id: Scalars['ID']['input'];
};


export type MutationPeriodSaveArgs = {
  id?: InputMaybe<Scalars['ID']['input']>;
  input: PeriodInput;
  seriesId: Scalars['ID']['input'];
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


export type MutationRegistrationSetExcusedArgs = {
  eventId: Scalars['ID']['input'];
  excused: Scalars['Boolean']['input'];
  userId: Scalars['ID']['input'];
};


export type MutationRoleSaveArgs = {
  name: Scalars['String']['input'];
};


export type MutationScoreSetArgs = {
  assists: Scalars['Int']['input'];
  eventId: Scalars['ID']['input'];
  goals: Scalars['Int']['input'];
  userId: Scalars['ID']['input'];
};


export type MutationSeriesCreateArgs = {
  groupId: Scalars['ID']['input'];
  name: Scalars['String']['input'];
};


export type MutationSeriesDeleteArgs = {
  id: Scalars['ID']['input'];
};


export type MutationSeriesRenameArgs = {
  id: Scalars['ID']['input'];
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

export type PaymentMethod =
  | 'CASH'
  | 'TRANSFER';

/** Nevyplněné hodnoty = výchozí nastavení jako u jednorázové akce; uzávěrka je v hodinách před začátkem. */
export type PeriodInput = {
  daysOfWeek: Array<DayOfWeek>;
  deadlineHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  durationMinutes?: InputMaybe<Scalars['Int']['input']>;
  intervalCount?: InputMaybe<Scalars['Int']['input']>;
  inviteRegularsHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  inviteSubstitutesHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  maxGoalies?: InputMaybe<Scalars['Int']['input']>;
  maxPlayersPerTeam?: InputMaybe<Scalars['Int']['input']>;
  monthWeeks?: InputMaybe<Array<Scalars['Int']['input']>>;
  note?: InputMaybe<Scalars['String']['input']>;
  pricePerHour?: InputMaybe<Scalars['Float']['input']>;
  recurrence: Recurrence;
  regularFee?: InputMaybe<Scalars['Float']['input']>;
  reminderHoursBefore?: InputMaybe<Scalars['Int']['input']>;
  /** HH:mm */
  startTime: Scalars['String']['input'];
  validFrom: Scalars['Date']['input'];
  validTo: Scalars['Date']['input'];
  venueId?: InputMaybe<Scalars['ID']['input']>;
};

/** Statistiky hráče za období (jen odehrané, nezrušené termíny). */
export type PlayerStats = {
  __typename?: 'PlayerStats';
  assists: Scalars['Int']['output'];
  attendanceRate: Scalars['Float']['output'];
  attended: Scalars['Int']['output'];
  charged: Scalars['Float']['output'];
  declined: Scalars['Int']['output'];
  /** Odehraných termínů v období. */
  events: Scalars['Int']['output'];
  goals: Scalars['Int']['output'];
  /** Neomluveně odhlášen po uzávěrce. */
  lateCancels: Scalars['Int']['output'];
  memberType: MemberType;
  noAnswer: Scalars['Int']['output'];
  /** Přihlášen, ale neomluveně nepřišel. */
  noShow: Scalars['Int']['output'];
  paid: Scalars['Float']['output'];
  position: Position;
  user: PublicUser;
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
  /** Moje platby (nejnovější první). */
  myCharges: Array<Charge>;
  /** Pozvánky do skupin, které čekají na moji odpověď. */
  myGroupInvites: Array<GroupMember>;
  myGroups: Array<SportGroup>;
  /** Nadcházející termíny ve všech mých skupinách. */
  myUpcomingEvents: Array<Event>;
  /** Náhled termínů období před uložením. */
  periodPreview: Array<Scalars['DateTime']['output']>;
  roles: Array<Role>;
  userProfile: User;
};


export type QueryEventArgs = {
  id: Scalars['ID']['input'];
};


export type QueryGroupArgs = {
  id: Scalars['ID']['input'];
};


export type QueryPeriodPreviewArgs = {
  input: PeriodInput;
};

export type Recurrence =
  | 'MONTHLY'
  | 'WEEKLY';

export type Registration = {
  __typename?: 'Registration';
  assists: Scalars['Int']['output'];
  /** Potvrzení účasti organizátorem (null = nepotvrzeno). */
  attended?: Maybe<Scalars['Boolean']['output']>;
  createdAt?: Maybe<Scalars['DateTime']['output']>;
  /** Pozdní odhlášení / neúčast omluvena – bez pokuty. */
  excused: Scalars['Boolean']['output'];
  goals: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  /** Odhlášen po uzávěrce (organizátorem). */
  lateCancel: Scalars['Boolean']['output'];
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

/** Období série s pravidlem opakování (např. září–březen každý pátek). */
export type SeriesPeriod = {
  __typename?: 'SeriesPeriod';
  daysOfWeek: Array<DayOfWeek>;
  deadlineHoursBefore: Scalars['Int']['output'];
  durationMinutes: Scalars['Int']['output'];
  id: Scalars['ID']['output'];
  /** Každý N-tý týden / měsíc. */
  intervalCount: Scalars['Int']['output'];
  inviteRegularsHoursBefore: Scalars['Int']['output'];
  inviteSubstitutesHoursBefore: Scalars['Int']['output'];
  maxGoalies: Scalars['Int']['output'];
  maxPlayersPerTeam: Scalars['Int']['output'];
  /** Jen MONTHLY: kolikátý výskyt dne v měsíci (1–5, -1 = poslední). */
  monthWeeks: Array<Scalars['Int']['output']>;
  note?: Maybe<Scalars['String']['output']>;
  pricePerHour?: Maybe<Scalars['Float']['output']>;
  recurrence: Recurrence;
  regularFee?: Maybe<Scalars['Float']['output']>;
  reminderHoursBefore?: Maybe<Scalars['Int']['output']>;
  /** Čas začátku HH:mm (Europe/Prague). */
  startTime: Scalars['String']['output'];
  validFrom: Scalars['Date']['output'];
  validTo: Scalars['Date']['output'];
  venue?: Maybe<Venue>;
};

export type SportGroup = {
  __typename?: 'SportGroup';
  amOrganizer: Scalars['Boolean']['output'];
  /** Zůstatek banku skupiny v Kč. */
  bankBalance: Scalars['Float']['output'];
  bankEntries: Array<BankEntry>;
  bankTransactions: Array<BankTransaction>;
  description?: Maybe<Scalars['String']['output']>;
  events: Array<Event>;
  /** Pozdní odhlášení a neomluvená neúčast platí celý podíl. */
  finesEnabled: Scalars['Boolean']['output'];
  /** Skupina má nastavený token Fio API. */
  fioConnected: Scalars['Boolean']['output'];
  /** Chyba posledního stažení (null = v pořádku). */
  fioLastError?: Maybe<Scalars['String']['output']>;
  fioLastSyncAt?: Maybe<Scalars['DateTime']['output']>;
  /** Účet pro QR platby (IBAN). */
  iban?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  members: Array<GroupMember>;
  /** Členství přihlášeného uživatele (null u admina, který není členem). */
  myMembership?: Maybe<GroupMember>;
  name: Scalars['String']['output'];
  /** Opakované akce skupiny. */
  series: Array<EventSeries>;
  /** Statistiky aktivních členů za období (bez zadání = aktuální sezóna září–srpen). */
  stats: Array<PlayerStats>;
  teams: Array<Team>;
  /** Nezaplacené platby skupiny (pro ruční spárování). */
  unpaidCharges: Array<Charge>;
  venues: Array<Venue>;
};


export type SportGroupBankTransactionsArgs = {
  status?: InputMaybe<BankTransactionStatus>;
};


export type SportGroupEventsArgs = {
  from?: InputMaybe<Scalars['DateTime']['input']>;
  to?: InputMaybe<Scalars['DateTime']['input']>;
};


export type SportGroupStatsArgs = {
  from?: InputMaybe<Scalars['Date']['input']>;
  to?: InputMaybe<Scalars['Date']['input']>;
};

/** Co se stalo s termíny po uložení/smazání období. */
export type SyncResult = {
  __typename?: 'SyncResult';
  created: Scalars['Int']['output'];
  /** Termíny s přihláškami/pozvánkami, zrušené nebo ručně upravené – zůstaly beze změny. */
  kept: Scalars['Int']['output'];
  removed: Scalars['Int']['output'];
  updated: Scalars['Int']['output'];
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
