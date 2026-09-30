import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzListModule } from 'ng-zorro-antd/list';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { GroupCreateGQL, GroupInviteRespondGQL, MyDashboardGQL, MyDashboardQuery } from '../graphql/dashboard.generated';
import { gqlErrorMessage, MEMBER_TYPE, REGISTRATION_COLOR, REGISTRATION_STATUS } from '../shared/labels';

/**
 * Přehled přihlášeného uživatele: nadcházející akce, pozvánky do skupin a moje skupiny.
 */
@Component({
  selector: 'app-prehled',
  imports: [DatePipe, FormsModule, RouterLink, NzButtonModule, NzCardModule, NzEmptyModule, NzGridModule, NzInputModule,
    NzListModule, NzModalModule, NzTagModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './prehled.component.html',
})
export class PrehledComponent {
  private dashboardGQL = inject(MyDashboardGQL);
  private inviteRespondGQL = inject(GroupInviteRespondGQL);
  private groupCreateGQL = inject(GroupCreateGQL);
  private message = inject(NzMessageService);
  private router = inject(Router);

  readonly memberType = MEMBER_TYPE;
  readonly regStatus = REGISTRATION_STATUS;
  readonly regColor = REGISTRATION_COLOR;

  data = signal<MyDashboardQuery | null>(null);
  loading = signal(true);
  newGroupVisible = signal(false);
  newGroupName = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.dashboardGQL.fetch({ fetchPolicy: 'network-only' }).subscribe({
      next: (r) => {
        this.data.set(r.data ?? null);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }

  respondInvite(memberId: string, accept: boolean): void {
    this.inviteRespondGQL.mutate({ variables: { memberId, accept } }).subscribe({
      next: () => {
        this.message.success(accept ? 'Jste členem skupiny.' : 'Pozvánka odmítnuta.');
        this.load();
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  createGroup(): void {
    const name = this.newGroupName().trim();
    if (!name) {
      return;
    }
    this.groupCreateGQL.mutate({ variables: { input: { name } } }).subscribe({
      next: (r) => {
        this.newGroupVisible.set(false);
        this.newGroupName.set('');
        this.router.navigate(['/skupiny', r.data?.groupCreate.id]);
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }
}
