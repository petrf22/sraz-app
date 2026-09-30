import { Component, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { Observable } from 'rxjs';
import { EventCreateGQL } from '../graphql/event.generated';
import {
  GroupDetailGQL,
  GroupDetailQuery,
  MemberInviteGQL,
  MemberRemoveGQL,
  MemberUpdateGQL,
  TeamDeleteGQL,
  TeamSaveGQL,
  VenueDeleteGQL,
  VenueSaveGQL,
} from '../graphql/group.generated';
import { EventInput, MemberType, Position } from '../graphql/graphql-types';
import { AkceFormularComponent } from '../akce-formular/akce-formular.component';
import { KalendarComponent } from '../kalendar/kalendar.component';
import { BankComponent } from '../bank/bank.component';
import { StatistikyComponent } from '../statistiky/statistiky.component';
import { OpakovaniComponent } from '../opakovani/opakovani.component';
import { EVENT_STATUS, gqlErrorMessage, MEMBER_TYPE, MEMBERSHIP_STATUS, POSITION, REGISTRATION_COLOR, REGISTRATION_STATUS } from '../shared/labels';

type Group = GroupDetailQuery['group'];

interface VenueForm {
  id: string | null;
  name: string;
  address: string;
  mapUrl: string;
  latitude: number | null;
  longitude: number | null;
}

/**
 * Detail skupiny: akce, členové (pozvánky), týmy a místa. Úpravy smí jen organizátor.
 */
@Component({
  selector: 'app-skupina',
  imports: [DatePipe, FormsModule, RouterLink, AkceFormularComponent, KalendarComponent, OpakovaniComponent, BankComponent, StatistikyComponent, NzButtonModule, NzCardModule, NzCheckboxModule,
    NzFormModule, NzInputModule, NzInputNumberModule, NzModalModule, NzPopconfirmModule, NzSelectModule, NzTableModule,
    NzTabsModule, NzTagModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './skupina.component.html',
})
export class SkupinaComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private message = inject(NzMessageService);
  private groupGQL = inject(GroupDetailGQL);
  private memberInviteGQL = inject(MemberInviteGQL);
  private memberUpdateGQL = inject(MemberUpdateGQL);
  private memberRemoveGQL = inject(MemberRemoveGQL);
  private teamSaveGQL = inject(TeamSaveGQL);
  private teamDeleteGQL = inject(TeamDeleteGQL);
  private venueSaveGQL = inject(VenueSaveGQL);
  private venueDeleteGQL = inject(VenueDeleteGQL);
  private eventCreateGQL = inject(EventCreateGQL);

  readonly memberType = MEMBER_TYPE;
  readonly position = POSITION;
  readonly membershipStatus = MEMBERSHIP_STATUS;
  readonly eventStatus = EVENT_STATUS;
  readonly regStatus = REGISTRATION_STATUS;
  readonly regColor = REGISTRATION_COLOR;

  private groupId = this.route.snapshot.paramMap.get('id')!;
  group = signal<Group | null>(null);
  organizer = computed(() => this.group()?.amOrganizer ?? false);
  /** Aktivní a pozvaní nahoře, odmítnutí a odebraní dole. */
  members = computed(() => {
    const order = { ACTIVE: 0, INVITED: 1, DECLINED: 2, REMOVED: 3 };
    return [...(this.group()?.members ?? [])].sort((a, b) => order[a.status] - order[b.status]);
  });

  invite = { email: '', memberType: 'SUBSTITUTE' as MemberType, position: 'PLAYER' as Position };
  newTeamName = '';
  venueForm: VenueForm | null = null;
  eventModalVisible = signal(false);
  saving = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.groupGQL
      .fetch({ variables: { id: this.groupId, from: new Date(Date.now() - 7 * 86400_000).toISOString() }, fetchPolicy: 'network-only' })
      .subscribe({
        next: (r) => this.group.set(r.data?.group ?? null),
        error: (e) => this.message.error(gqlErrorMessage(e)),
      });
  }

  /** Společné zpracování mutací: zpráva, znovunačtení, chyba. */
  private run(op: Observable<unknown>, success: string, after?: () => void): void {
    this.saving.set(true);
    op.subscribe({
      next: () => {
        this.saving.set(false);
        this.message.success(success);
        after?.();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }

  // ---- členové ----

  sendInvite(): void {
    if (!this.invite.email.trim()) {
      return;
    }
    this.run(
      this.memberInviteGQL.mutate({ variables: { groupId: this.groupId, input: { ...this.invite, email: this.invite.email.trim() } } }),
      `Pozvánka odeslána na ${this.invite.email.trim()}.`,
      () => (this.invite.email = ''),
    );
  }

  updateMember(id: string, input: { memberType?: MemberType; position?: Position; organizer?: boolean }): void {
    this.run(this.memberUpdateGQL.mutate({ variables: { id, input } }), 'Uloženo.');
  }

  removeMember(id: string): void {
    this.run(this.memberRemoveGQL.mutate({ variables: { id } }), 'Člen odebrán.');
  }

  leaveGroup(): void {
    const myId = this.group()?.myMembership?.id;
    if (myId) {
      this.run(this.memberRemoveGQL.mutate({ variables: { id: myId } }), 'Opustili jste skupinu.', () => this.router.navigate(['/']));
    }
  }

  // ---- týmy ----

  saveTeam(team: { id: string | null; name: string; color: string | null; sortOrder?: number }): void {
    if (!team.name.trim()) {
      return;
    }
    this.run(
      this.teamSaveGQL.mutate({ variables: { groupId: this.groupId, input: { id: team.id, name: team.name.trim(), color: team.color, sortOrder: team.sortOrder } } }),
      'Tým uložen.',
      () => (this.newTeamName = ''),
    );
  }

  deleteTeam(id: string): void {
    this.run(this.teamDeleteGQL.mutate({ variables: { id } }), 'Tým smazán.');
  }

  // ---- místa ----

  editVenue(v?: Group['venues'][number]): void {
    this.venueForm = v
      ? { id: v.id, name: v.name, address: v.address ?? '', mapUrl: v.mapUrl ?? '', latitude: v.latitude ?? null, longitude: v.longitude ?? null }
      : { id: null, name: '', address: '', mapUrl: '', latitude: null, longitude: null };
  }

  saveVenue(): void {
    const f = this.venueForm;
    if (!f || !f.name.trim()) {
      return;
    }
    this.run(
      this.venueSaveGQL.mutate({
        variables: {
          groupId: this.groupId,
          input: { id: f.id, name: f.name.trim(), address: f.address || null, mapUrl: f.mapUrl || null, latitude: f.latitude, longitude: f.longitude },
        },
      }),
      'Místo uloženo.',
      () => (this.venueForm = null),
    );
  }

  deleteVenue(id: string): void {
    this.run(this.venueDeleteGQL.mutate({ variables: { id } }), 'Místo smazáno.');
  }

  // ---- akce ----

  createEvent(input: EventInput): void {
    this.saving.set(true);
    this.eventCreateGQL.mutate({ variables: { groupId: this.groupId, input } }).subscribe({
      next: (r) => {
        this.saving.set(false);
        this.eventModalVisible.set(false);
        this.router.navigate(['/akce', r.data?.eventCreate.id]);
      },
      error: (e) => {
        this.saving.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }
}
