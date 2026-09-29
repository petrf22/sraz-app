import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { Observable } from 'rxjs';
import {
  EventCancelGQL,
  EventDetailGQL,
  EventDetailQuery,
  EventSendInvitationsGQL,
  EventUpdateGQL,
  RegistrationRespondGQL,
  RegistrationSetGQL,
} from '../graphql/event.generated';
import { EventInput, MemberType, RegistrationStatus } from '../graphql/graphql-types';
import { AkceFormularComponent, EventFormValue } from '../akce-formular/akce-formular.component';
import { EVENT_STATUS, gqlErrorMessage, MEMBER_TYPE, POSITION, REGISTRATION_COLOR, REGISTRATION_STATUS } from '../shared/labels';

type EventDetail = EventDetailQuery['event'];
type Team = EventDetail['group']['teams'][number];

/**
 * Detail termínu: moje přihláška, soupiska podle týmů a fronta; organizátor navíc
 * rozesílá pozvánky, upravuje/ruší akci a mění přihlášky členů (i po uzávěrce).
 */
@Component({
  selector: 'app-akce',
  imports: [DatePipe, FormsModule, RouterLink, AkceFormularComponent, NzAlertModule, NzButtonModule, NzCardModule, NzGridModule,
    NzInputModule, NzModalModule, NzSelectModule, NzTableModule, NzTagModule],
  templateUrl: './akce.component.html',
  styleUrl: './akce.component.scss',
})
export class AkceComponent {
  private route = inject(ActivatedRoute);
  private message = inject(NzMessageService);
  private eventGQL = inject(EventDetailGQL);
  private respondGQL = inject(RegistrationRespondGQL);
  private setGQL = inject(RegistrationSetGQL);
  private sendGQL = inject(EventSendInvitationsGQL);
  private updateGQL = inject(EventUpdateGQL);
  private cancelGQL = inject(EventCancelGQL);

  readonly eventStatus = EVENT_STATUS;
  readonly regStatus = REGISTRATION_STATUS;
  readonly regColor = REGISTRATION_COLOR;
  readonly memberType = MEMBER_TYPE;
  readonly position = POSITION;

  private eventId = this.route.snapshot.paramMap.get('id')!;
  event = signal<EventDetail | null>(null);
  saving = signal(false);
  editVisible = signal(false);
  cancelVisible = signal(false);
  cancelReason = '';

  organizer = computed(() => this.event()?.group.amOrganizer ?? false);
  cancelled = computed(() => this.event()?.status === 'CANCELLED');

  /** Sloupce soupisky: týmy (nebo „Hráči“ bez týmů) a brankáři. */
  columns = computed(() => {
    const e = this.event();
    if (!e) {
      return [];
    }
    const inPlayers = e.registrations.filter((r) => r.status === 'IN' && r.position === 'PLAYER');
    const cols = e.group.teams.map((t) => ({
      title: t.name,
      color: t.color,
      names: inPlayers.filter((r) => r.team?.id === t.id).map((r) => r.user.publicName),
      max: e.maxPlayersPerTeam,
    }));
    if (e.group.teams.length === 0) {
      cols.push({ title: 'Hráči', color: null, names: inPlayers.map((r) => r.user.publicName), max: e.maxPlayersPerTeam });
    }
    cols.push({
      title: 'Brankáři',
      color: null,
      names: e.registrations.filter((r) => r.status === 'IN' && r.position === 'GOALIE').map((r) => r.user.publicName),
      max: e.maxGoalies,
    });
    return cols;
  });

  waitlist = computed(() =>
    (this.event()?.registrations ?? [])
      .filter((r) => r.status === 'WAITLIST')
      .sort((a, b) => (a.queuedAt ?? '').localeCompare(b.queuedAt ?? '')),
  );

  notComing = computed(() => (this.event()?.registrations ?? []).filter((r) => r.status === 'OUT'));

  /** Pro organizátora: všichni aktivní členové s jejich přihláškou. */
  roster = computed(() => {
    const e = this.event();
    if (!e) {
      return [];
    }
    return e.group.members
      .filter((m) => m.status === 'ACTIVE' && m.user)
      .map((m) => ({ member: m, reg: e.registrations.find((r) => r.user.id === m.user!.id) ?? null }))
      .sort((a, b) => a.member.user!.publicName.localeCompare(b.member.user!.publicName, 'cs'));
  });

  formValue = computed<EventFormValue | null>(() => {
    const e = this.event();
    return e
      ? {
          name: e.name,
          startsAt: new Date(e.startsAt),
          durationMinutes: e.durationMinutes,
          venueId: e.venue?.id ?? null,
          maxPlayersPerTeam: e.maxPlayersPerTeam,
          maxGoalies: e.maxGoalies,
          signupDeadline: new Date(e.signupDeadline),
          inviteRegularsHoursBefore: e.inviteRegularsHoursBefore,
          inviteSubstitutesHoursBefore: e.inviteSubstitutesHoursBefore,
          note: e.note ?? '',
        }
      : null;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.eventGQL.fetch({ variables: { id: this.eventId }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => this.event.set(r.data?.event ?? null),
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  private run(op: Observable<unknown>, success: string | ((result: unknown) => string), after?: () => void): void {
    this.saving.set(true);
    op.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.message.success(typeof success === 'function' ? success(result) : success);
        after?.();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }

  respond(status: RegistrationStatus, team: Team | null = null): void {
    this.run(
      this.respondGQL.mutate({ variables: { eventId: this.eventId, status, teamId: team?.id ?? null } }),
      (r) => {
        const s = (r as { data?: { registrationRespond: { status: RegistrationStatus } } }).data?.registrationRespond.status;
        return s === 'WAITLIST' ? 'Kapacita je plná – jste ve frontě.' : s === 'IN' ? 'Jste přihlášen(a).' : 'Jste odhlášen(a).';
      },
    );
  }

  setRegistration(userId: string, status: RegistrationStatus, teamId: string | null): void {
    this.run(this.setGQL.mutate({ variables: { eventId: this.eventId, userId, status, teamId } }), 'Přihláška změněna.');
  }

  sendInvitations(memberType: MemberType): void {
    this.run(
      this.sendGQL.mutate({ variables: { id: this.eventId, memberType } }),
      (r) => `Odesláno pozvánek: ${(r as { data?: { eventSendInvitations: number } }).data?.eventSendInvitations ?? 0}.`,
    );
  }

  update(input: EventInput): void {
    this.run(this.updateGQL.mutate({ variables: { id: this.eventId, input } }), 'Akce upravena.', () => this.editVisible.set(false));
  }

  cancel(): void {
    this.run(
      this.cancelGQL.mutate({ variables: { id: this.eventId, reason: this.cancelReason.trim() || null } }),
      'Akce zrušena, přihlášeným odešel e-mail.',
      () => this.cancelVisible.set(false),
    );
  }
}
