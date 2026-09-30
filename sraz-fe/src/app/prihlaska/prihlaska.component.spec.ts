import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { PrihlaskaComponent } from './prihlaska.component';
import { PublicInvitation } from '../services/public-api.service';

const invitation: PublicInvitation = {
  groupName: 'Večerní hokej',
  eventName: 'Hokej',
  startsAt: '2026-10-02T20:00:00+02:00',
  durationMinutes: 60,
  signupDeadline: '2026-10-01T20:00:00+02:00',
  eventStatus: 'OPEN',
  note: null,
  venue: null,
  playerName: 'Petr',
  position: 'PLAYER',
  myStatus: null,
  myTeamId: null,
  teams: [
    { id: 1, name: 'Modří', color: '#1677ff' },
    { id: 2, name: 'Červení', color: '#f5222d' },
  ],
  summary: { players: 1, maxPlayers: 20, goalies: 0, maxGoalies: 2, waitlist: 0 },
  roster: [{ name: 'Jan', status: 'IN', position: 'PLAYER', teamId: 2 }],
  signupOpen: true,
  expired: false,
};

describe('PrihlaskaComponent', () => {
  let fixture: ComponentFixture<PrihlaskaComponent>;
  let http: HttpTestingController;

  function create(query: Record<string, string> = {}) {
    TestBed.configureTestingModule({
      imports: [PrihlaskaComponent],
      providers: [
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ token: 'abc' }), queryParamMap: convertToParamMap(query) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PrihlaskaComponent);
    http.expectOne('/api/public/invitations/abc').flush(invitation);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('shows event and one button per team', () => {
    create();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Hokej');
    expect(text).toContain('Přijdu – Modří');
    expect(text).toContain('Přijdu – Červení');
    expect(fixture.componentInstance.columns()[1].names).toEqual(['Jan']);
  });

  it('link from e-mail only preselects the choice, nothing is sent until confirmed', () => {
    create({ volba: 'IN', tym: '2' });
    expect(fixture.componentInstance.pending()?.team?.name).toBe('Červení');
    http.expectNone('/api/public/invitations/abc');
  });

  it('sends chosen team and shows result', () => {
    create();
    fixture.componentInstance.choose('IN', invitation.teams[0]);
    const req = http.expectOne('/api/public/invitations/abc');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ status: 'IN', teamId: 1 });
    req.flush({ ...invitation, myStatus: 'WAITLIST', myTeamId: 1 });
    expect(fixture.componentInstance.saved()).toContain('frontě');
  });
});
