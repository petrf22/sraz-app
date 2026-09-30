import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DecimalPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzTableModule } from 'ng-zorro-antd/table';
import { GroupStatsGQL, GroupStatsQuery } from '../graphql/stats.generated';
import { isoDate } from '../opakovani/opakovani.component';
import { gqlErrorMessage, MEMBER_TYPE, POSITION } from '../shared/labels';

type Row = GroupStatsQuery['group']['stats'][number];

/** Hodnoty oddělené středníkem (český Excel), s uvozovkami kolem textu. */
export function toCsv(header: string[], rows: (string | number)[][]): string {
  const cell = (v: string | number) => (typeof v === 'number' ? String(v).replace('.', ',') : `"${v.replace(/"/g, '""')}"`);
  return [header, ...rows].map((r) => r.map(cell).join(';')).join('\r\n');
}

/**
 * Statistiky hráčů skupiny za období (výchozí = aktuální sezóna září–srpen), řazení a export CSV.
 */
@Component({
  selector: 'app-statistiky',
  imports: [DecimalPipe, PercentPipe, FormsModule, NzButtonModule, NzDatePickerModule, NzTableModule],
  templateUrl: './statistiky.component.html',
})
export class StatistikyComponent implements OnInit {
  groupId = input.required<string>();

  private statsGQL = inject(GroupStatsGQL);
  private message = inject(NzMessageService);

  readonly memberType = MEMBER_TYPE;
  readonly position = POSITION;
  rows = signal<Row[]>([]);
  groupName = '';
  range: Date[] = [];

  readonly sortAttended = (a: Row, b: Row) => a.attended - b.attended;
  readonly sortRate = (a: Row, b: Row) => a.attendanceRate - b.attendanceRate;
  readonly sortGoals = (a: Row, b: Row) => a.goals - b.goals;
  readonly sortAssists = (a: Row, b: Row) => a.assists - b.assists;
  readonly sortNoShow = (a: Row, b: Row) => a.noShow - b.noShow;
  readonly sortLateCancels = (a: Row, b: Row) => a.lateCancels - b.lateCancels;
  readonly sortDebt = (a: Row, b: Row) => a.charged - a.paid - (b.charged - b.paid);
  readonly sortName = (a: Row, b: Row) => a.user.publicName.localeCompare(b.user.publicName, 'cs');

  ngOnInit(): void {
    const now = new Date();
    const year = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1;
    this.range = [new Date(year, 8, 1), new Date(year + 1, 7, 31)];
    this.load();
  }

  load(): void {
    const [from, to] = this.range ?? [];
    this.statsGQL
      .fetch({
        variables: { id: this.groupId(), from: from ? isoDate(from) : null, to: to ? isoDate(to) : null },
        fetchPolicy: 'network-only',
      })
      .subscribe({
        next: (r) => {
          this.rows.set(r.data?.group.stats ?? []);
          this.groupName = r.data?.group.name ?? '';
        },
        error: (e) => this.message.error(gqlErrorMessage(e)),
      });
  }

  exportCsv(): void {
    const header = ['Hráč', 'Typ', 'Pozice', 'Termínů', 'Účast', 'Účast %', 'Nepřišel', 'Pozdě odhlášen', 'Omluven', 'Bez odpovědi',
      'Góly', 'Asistence', 'Vyúčtováno Kč', 'Zaplaceno Kč'];
    const data = this.rows().map((r) => [r.user.publicName, this.memberType[r.memberType], this.position[r.position],
      r.events, r.attended, Math.round(r.attendanceRate * 100), r.noShow, r.lateCancels, r.declined, r.noAnswer, r.goals, r.assists,
      r.charged, r.paid]);
    // BOM, ať Excel pozná UTF-8 (diakritika)
    const blob = new Blob(['﻿' + toCsv(header, data)], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `statistiky-${this.groupName || 'skupina'}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }
}
