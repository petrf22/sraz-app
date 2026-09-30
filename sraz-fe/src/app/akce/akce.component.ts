import { Component, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
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
import { AttendanceSetGQL, ChargeSetPaidGQL, EventCloseGQL, EventReopenGQL, RegistrationSetExcusedGQL } from '../graphql/money.generated';
import { ScoreSetGQL } from '../graphql/stats.generated';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { DecimalPipe } from '@angular/common';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { CHARGE_KIND, CHARGE_REASON, EVENT_STATUS, gqlErrorMessage, MEMBER_TYPE, POSITION, REGISTRATION_COLOR, REGISTRATION_STATUS } from '../shared/labels';

type EventDetail = EventDetailQuery['event'];
type Team = EventDetail['group']['teams'][number];

/**
 * Detail termínu: moje přihláška, soupiska podle týmů a fronta; organizátor navíc
 * rozesílá pozvánky, upravuje/ruší akci a mění přihlášky členů (i po uzávěrce).
 */
@Component({
  selector: 'app-akce',
  imports: [DatePipe, DecimalPipe, FormsModule, NzCheckboxModule, NzInputNumberModule, NzPopconfirmModule, RouterLink, AkceFormularComponent, NzAlertModule, NzButtonModule, NzCardModule, NzGridModule,
    NzInputModule, NzModalModule, NzSelectModule, NzTableModule, NzTagModule],
  templateUrl: './akce.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  private attendanceGQL = inject(AttendanceSetGQL);
  private closeGQL = inject(EventCloseGQL);
  private reopenGQL = inject(EventReopenGQL);
  private paidGQL = inject(ChargeSetPaidGQL);
  private scoreGQL = inject(ScoreSetGQL);
  private excusedGQL = inject(RegistrationSetExcusedGQL);
  readonly chargeKind = CHARGE_KIND;
  readonly chargeReason = CHARGE_REASON;

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
  /** Vyúčtovat jde až akci, která začala. */
  started = computed(() => {
    const e = this.event();
    return !!e && new Date(e.startsAt).getTime() <= Date.now();
  });
  /** Kandidáti na vyúčtování: přihlášení (IN); zaškrtnutí = přišel. */
  attendees = computed(() =>
    (this.event()?.registrations ?? [])
      .filter((r) => r.status === 'IN')
      .sort((a, b) => a.user.publicName.localeCompare(b.user.publicName, 'cs')),
  );
  /** Odhlášení po uzávěrce – při zapnutých pokutách platí celý podíl, pokud je organizátor neomluví. */
  lateCancels = computed(() =>
    (this.event()?.registrations ?? [])
      .filter((r) => r.status === 'OUT' && r.lateCancel)
      .sort((a, b) => a.user.publicName.localeCompare(b.user.publicName, 'cs')),
  );
  chargesTotal = computed(() => (this.event()?.charges ?? []).reduce((s, c) => s + c.amount, 0));
  chargesPaid = computed(() => (this.event()?.charges ?? []).filter((c) => c.paidAt).reduce((s, c) => s + c.amount, 0));

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
          reminderHoursBefore: e.reminderHoursBefore ?? null,
          pricePerHour: e.pricePerHour ?? null,
          regularFee: e.regularFee ?? null,
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

  setAttendance(userId: string, attended: boolean): void {
    this.run(this.attendanceGQL.mutate({ variables: { eventId: this.eventId, userId, attended } }), attended ? 'Přišel.' : 'Nepřišel.');
  }

  setExcused(userId: string, excused: boolean): void {
    this.run(this.excusedGQL.mutate({ variables: { eventId: this.eventId, userId, excused } }), excused ? 'Omluveno.' : 'Omluva zrušena.');
  }

  setScore(userId: string, goals: number | null, assists: number | null): void {
    this.run(
      this.scoreGQL.mutate({ variables: { eventId: this.eventId, userId, goals: goals ?? 0, assists: assists ?? 0 } }),
      'Uloženo.',
    );
  }

  closeAccounting(): void {
    this.run(this.closeGQL.mutate({ variables: { id: this.eventId } }),
      (r) => `Vyúčtováno – plateb: ${(r as { data?: { eventClose: unknown[] } }).data?.eventClose.length ?? 0}, účastníkům odešel e-mail.`);
  }

  reopenAccounting(): void {
    this.run(this.reopenGQL.mutate({ variables: { id: this.eventId } }), 'Vyúčtování je znovu otevřené.');
  }

  setPaid(chargeId: string, state: 'NO' | 'CASH' | 'TRANSFER'): void {
    this.run(
      this.paidGQL.mutate({ variables: { id: chargeId, paid: state !== 'NO', method: state === 'NO' ? null : state } }),
      state === 'NO' ? 'Označeno jako nezaplacené.' : 'Zaplaceno.',
    );
  }

  cancel(): void {
    this.run(
      this.cancelGQL.mutate({ variables: { id: this.eventId, reason: this.cancelReason.trim() || null } }),
      'Akce zrušena, přihlášeným odešel e-mail.',
      () => this.cancelVisible.set(false),
    );
  }
}
