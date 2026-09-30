import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PozvankaComponent } from './pozvanka.component';

describe('PozvankaComponent', () => {
  let fixture: ComponentFixture<PozvankaComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PozvankaComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ token: 'xyz' }) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PozvankaComponent);
    http.expectOne('/api/public/group-invites/xyz').flush({
      groupName: 'Večerní hokej',
      invitedBy: 'Petr',
      email: 'novy@example.com',
      memberType: 'SUBSTITUTE',
      position: 'PLAYER',
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('shows the invite and accepts it', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('náhradník');

    fixture.componentInstance.respond(true);
    const req = http.expectOne('/api/public/group-invites/xyz');
    expect(req.request.body).toEqual({ accept: true });
    req.flush({ status: 'ACTIVE' });

    expect(fixture.componentInstance.answered()).toBe('accepted');
  });
});
