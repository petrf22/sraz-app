import { Component, inject, input, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzTableModule } from 'ng-zorro-antd/table';
import { Observable } from 'rxjs';
import { BankEntryAddGQL, BankEntryDeleteGQL, GroupBankGQL, GroupBankQuery } from '../graphql/money.generated';
import { GroupUpdateGQL } from '../graphql/group.generated';
import {
  BankTransactionAssignGQL,
  BankTransactionIgnoreGQL,
  FioSyncGQL,
  GroupFioGQL,
  GroupFioQuery,
  GroupSetFioTokenGQL,
} from '../graphql/fio.generated';
import { gqlErrorMessage } from '../shared/labels';

/**
 * Bank skupiny: zůstatek, pohyby z vyúčtování akcí a ruční pohyby; organizátor nastavuje i účet
 * skupiny (IBAN) pro QR platby, pokuty za pozdní odhlášení / neúčast a napojení na Fio API
 * (automatické párování plateb podle VS, nespárované pohyby páruje ručně).
 */
@Component({
  selector: 'app-bank',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, NzAlertModule, NzButtonModule, NzCardModule, NzCheckboxModule, NzInputModule,
    NzInputNumberModule, NzPopconfirmModule, NzSelectModule, NzStatisticModule, NzTableModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './bank.component.html',
})
export class BankComponent implements OnInit {
  groupId = input.required<string>();

  private bankGQL = inject(GroupBankGQL);
  private addGQL = inject(BankEntryAddGQL);
  private deleteGQL = inject(BankEntryDeleteGQL);
  private groupUpdateGQL = inject(GroupUpdateGQL);
  private fioGQL = inject(GroupFioGQL);
  private setTokenGQL = inject(GroupSetFioTokenGQL);
  private syncGQL = inject(FioSyncGQL);
  private assignGQL = inject(BankTransactionAssignGQL);
  private ignoreGQL = inject(BankTransactionIgnoreGQL);
  private message = inject(NzMessageService);

  data = signal<GroupBankQuery['group'] | null>(null);
  saving = signal(false);
  amount: number | null = null;
  description = '';
  iban = '';
  finesEnabled = false;
  fio = signal<GroupFioQuery['group'] | null>(null);
  fioToken = '';
  /** Vybraná platba ke spárování pro každý nespárovaný pohyb. */
  assignTo: Record<string, string | null> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.bankGQL.fetch({ variables: { id: this.groupId() }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => {
        this.data.set(r.data?.group ?? null);
        this.iban = r.data?.group.iban ?? '';
        this.finesEnabled = r.data?.group.finesEnabled ?? false;
        if (r.data?.group.amOrganizer) {
          this.loadFio();
        }
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  private loadFio(): void {
    this.fioGQL.fetch({ variables: { id: this.groupId() }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => this.fio.set(r.data?.group ?? null),
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  private run(op: Observable<unknown>, success: string | ((result: unknown) => string), after?: () => void): void {
    this.saving.set(true);
    op.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.message.success(typeof success === 'string' ? success : success(result));
        after?.();
        this.load();
      },
      error: (e) => {
        this.saving.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }

  add(sign: 1 | -1): void {
    if (!this.amount || !this.description.trim()) {
      this.message.warning('Vyplň částku a popis.');
      return;
    }
    this.run(
      this.addGQL.mutate({ variables: { groupId: this.groupId(), amount: sign * Math.abs(this.amount), description: this.description.trim() } }),
      'Pohyb zapsán.',
      () => {
        this.amount = null;
        this.description = '';
      },
    );
  }

  remove(id: string): void {
    this.run(this.deleteGQL.mutate({ variables: { id } }), 'Pohyb smazán.');
  }

  saveSettings(): void {
    const g = this.data();
    if (g) {
      this.run(
        this.groupUpdateGQL.mutate({
          variables: { id: g.id, input: { name: g.name, description: g.description, iban: this.iban, finesEnabled: this.finesEnabled } },
        }),
        'Nastavení plateb uloženo.',
      );
    }
  }

  connectFio(): void {
    if (!this.fioToken.trim()) {
      this.message.warning('Vlož token z internetbankingu Fio.');
      return;
    }
    this.run(
      this.setTokenGQL.mutate({ variables: { groupId: this.groupId(), token: this.fioToken.trim() } }),
      (r) => 'Fio připojeno. ' + syncSummary((r as { data?: { groupSetFioToken: SyncSummary | null } }).data?.groupSetFioToken),
      () => (this.fioToken = ''),
    );
  }

  disconnectFio(): void {
    this.run(this.setTokenGQL.mutate({ variables: { groupId: this.groupId(), token: null } }), 'Fio odpojeno.');
  }

  syncFio(): void {
    this.run(
      this.syncGQL.mutate({ variables: { groupId: this.groupId() } }),
      (r) => syncSummary((r as { data?: { fioSync: SyncSummary } }).data?.fioSync),
    );
  }

  assign(txId: string): void {
    const chargeId = this.assignTo[txId];
    if (!chargeId) {
      this.message.warning('Vyber platbu, ke které pohyb patří.');
      return;
    }
    this.run(this.assignGQL.mutate({ variables: { id: txId, chargeId } }), 'Spárováno – platba je zaplacená.');
  }

  ignore(txId: string): void {
    this.run(this.ignoreGQL.mutate({ variables: { id: txId } }), 'Pohyb odložen.');
  }
}

interface SyncSummary {
  fetched: number;
  created: number;
  matched: number;
  unmatched: number;
}

function syncSummary(r: SyncSummary | null | undefined): string {
  return r ? `Nových pohybů ${r.created}, spárováno ${r.matched}, ke kontrole ${r.unmatched}.` : '';
}
