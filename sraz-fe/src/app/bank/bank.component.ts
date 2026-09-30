import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzTableModule } from 'ng-zorro-antd/table';
import { Observable } from 'rxjs';
import { BankEntryAddGQL, BankEntryDeleteGQL, GroupBankGQL, GroupBankQuery } from '../graphql/money.generated';
import { GroupUpdateGQL } from '../graphql/group.generated';
import { gqlErrorMessage } from '../shared/labels';

/**
 * Bank skupiny: zůstatek, pohyby z vyúčtování akcí a ruční pohyby; organizátor zadává i účet
 * skupiny (IBAN) pro QR platby.
 */
@Component({
  selector: 'app-bank',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, NzButtonModule, NzCardModule, NzInputModule, NzInputNumberModule,
    NzPopconfirmModule, NzStatisticModule, NzTableModule],
  templateUrl: './bank.component.html',
})
export class BankComponent implements OnInit {
  groupId = input.required<string>();

  private bankGQL = inject(GroupBankGQL);
  private addGQL = inject(BankEntryAddGQL);
  private deleteGQL = inject(BankEntryDeleteGQL);
  private groupUpdateGQL = inject(GroupUpdateGQL);
  private message = inject(NzMessageService);

  data = signal<GroupBankQuery['group'] | null>(null);
  saving = signal(false);
  amount: number | null = null;
  description = '';
  iban = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.bankGQL.fetch({ variables: { id: this.groupId() }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => {
        this.data.set(r.data?.group ?? null);
        this.iban = r.data?.group.iban ?? '';
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

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

  saveIban(): void {
    const g = this.data();
    if (g) {
      this.run(
        this.groupUpdateGQL.mutate({ variables: { id: g.id, input: { name: g.name, description: g.description, iban: this.iban } } }),
        this.iban.trim() ? 'Účet uložen – platby půjdou zaplatit QR kódem.' : 'Účet odebrán.',
      );
    }
  }
}
