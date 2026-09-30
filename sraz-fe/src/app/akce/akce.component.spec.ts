import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { ApolloTestingController, ApolloTestingModule } from 'apollo-angular/testing';
import { AkceComponent } from './akce.component';
import { EventDetailDocument, EventDetailQuery, RegistrationRespondDocument } from '../graphql/event.generated';

const team = (id: string, name: string, color: string) => ({ __typename: 'Team' as const, id, name, color, sortOrder: 0 });
const user = (id: string, publicName: string) => ({ __typename: 'PublicUser' as const, id, publicName });
const reg = (id: string, u: ReturnType<typeof user>, status: 'IN' | 'OUT' | 'WAITLIST', teamId: string | null, queuedAt: string | null = null) => ({
  __typename: 'Registration' as const,
  id,
  status,
  position: 'PLAYER' as const,
  source: 'WEB' as const,
  queuedAt,
  updatedAt: null,
  attended: null,
  user: u,
  team: teamId ? { __typename: 'Team' as const, id: teamId } : null,
});

const event: EventDetailQuery['event'] = {
  __typename: 'Event',
  id: '10',
  name: 'Hokej',
  startsAt: '2026-10-02T20:00:00+02:00',
  durationMinutes: 60,
  maxPlayersPerTeam: 1,
  maxGoalies: 2,
  signupDeadline: '2026-10-01T20:00:00+02:00',
  inviteRegularsHoursBefore: 96,
  inviteSubstitutesHoursBefore: 48,
  regularsInvitedAt: null,
  substitutesInvitedAt: null,
  status: 'OPEN',
  note: null,
  signupOpen: true,
  detached: false,
  reminderHoursBefore: null,
  series: null,
  pricePerHour: null,
  regularFee: null,
  closedAt: null,
  charges: [],
  venue: null,
  summary: { __typename: 'EventSummary', players: 2, maxPlayers: 2, goalies: 0, maxGoalies: 2, waitlist: 1 },
  myRegistration: null,
  registrations: [
    reg('1', user('u1', 'Adam'), 'IN', 't1'),
    reg('2', user('u2', 'Bára'), 'IN', 't2'),
    reg('3', user('u3', 'Cyril'), 'WAITLIST', 't1', '2026-09-30T10:00:00Z'),
  ],
  group: {
    __typename: 'SportGroup',
    id: '1',
    name: 'Večerní hokej',
    amOrganizer: false,
    teams: [team('t1', 'Modří', '#1677ff'), team('t2', 'Červení', '#f5222d')],
    venues: [],
    members: [],
  },
} as EventDetailQuery['event'];

describe('AkceComponent', () => {
  let fixture: ComponentFixture<AkceComponent>;
  let apollo: ApolloTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AkceComponent, ApolloTestingModule],
      providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } } }],
    });
    apollo = TestBed.inject(ApolloTestingController);
    fixture = TestBed.createComponent(AkceComponent);
    apollo.expectOne(EventDetailDocument).flush({ data: { event } });
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => apollo.verify());

  it('splits roster by team and shows the waitlist', () => {
    const c = fixture.componentInstance;
    expect(c.columns().map((col) => [col.title, col.names])).toEqual([
      ['Modří', ['Adam']],
      ['Červení', ['Bára']],
      ['Brankáři', []],
    ]);
    expect(c.waitlist().map((r) => r.user.publicName)).toEqual(['Cyril']);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Přijdu – Modří');
  });

  it('sends my registration with the chosen team', async () => {
    fixture.componentInstance.respond('IN', event.group.teams[1]);
    const op = apollo.expectOne(RegistrationRespondDocument);
    expect(op.operation.variables).toEqual({ eventId: '10', status: 'IN', teamId: 't2' });
    op.flush({ data: { registrationRespond: { __typename: 'Registration', id: '4', status: 'IN' } } });
    // po úspěšné mutaci se detail načte znovu (výsledek Apolla přijde asynchronně)
    await new Promise((resolve) => setTimeout(resolve));
    apollo.expectOne(EventDetailDocument).flush({ data: { event } });
  });
});
