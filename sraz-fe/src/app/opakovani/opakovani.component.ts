import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTimePickerModule } from 'ng-zorro-antd/time-picker';
import { Observable } from 'rxjs';
import { DayOfWeek, PeriodInput, Recurrence } from '../graphql/graphql-types';
import {
  GroupSeriesGQL,
  GroupSeriesQuery,
  PeriodDeleteGQL,
  PeriodPreviewGQL,
  PeriodSaveGQL,
  SeriesCreateGQL,
  SeriesDeleteGQL,
  SeriesRenameGQL,
} from '../graphql/series.generated';
import { gqlErrorMessage } from '../shared/labels';

type Series = GroupSeriesQuery['group']['series'][number];
type Period = Series['periods'][number];

interface SyncResult {
  created: number;
  updated: number;
  removed: number;
  kept: number;
}

/** Formulář období – čas a data jako Date kvůli ng-zorro pickerům. */
interface PeriodForm {
  seriesId: string;
  id: string | null;
  range: Date[];
  recurrence: Recurrence;
  intervalCount: number;
  days: DayOfWeek[];
  monthWeeks: number[];
  startTime: Date | null;
  durationMinutes: number;
  venueId: string | null;
  maxPlayersPerTeam: number;
  maxGoalies: number;
  deadlineHoursBefore: number;
  inviteRegularsHoursBefore: number;
  inviteSubstitutesHoursBefore: number;
  reminderHoursBefore: number | null;
  note: string;
}

export const DAYS: { value: DayOfWeek; label: string; short: string }[] = [
  { value: 'MONDAY', label: 'pondělí', short: 'po' },
  { value: 'TUESDAY', label: 'úterý', short: 'út' },
  { value: 'WEDNESDAY', label: 'středa', short: 'st' },
  { value: 'THURSDAY', label: 'čtvrtek', short: 'čt' },
  { value: 'FRIDAY', label: 'pátek', short: 'pá' },
  { value: 'SATURDAY', label: 'sobota', short: 'so' },
  { value: 'SUNDAY', label: 'neděle', short: 'ne' },
];

export const MONTH_WEEKS: { value: number; label: string }[] = [
  { value: 1, label: '1.' },
  { value: 2, label: '2.' },
  { value: 3, label: '3.' },
  { value: 4, label: '4.' },
  { value: 5, label: '5.' },
  { value: -1, label: 'poslední' },
];

/** Datum jako yyyy-MM-dd v místním čase (ne toISOString – posunul by den přes UTC). */
export function isoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Lidsky čitelné pravidlo, např. „každý pátek" nebo „1. a 3. pátek v měsíci". */
export function describeRule(p: Pick<Period, 'recurrence' | 'intervalCount' | 'daysOfWeek' | 'monthWeeks'>): string {
  const days = DAYS.filter((d) => p.daysOfWeek.includes(d.value)).map((d) => d.label).join(' a ');
  if (p.recurrence === 'WEEKLY') {
    return p.intervalCount > 1 ? `každý ${p.intervalCount}. týden: ${days}` : `každý týden: ${days}`;
  }
  const weeks = MONTH_WEEKS.filter((w) => p.monthWeeks.includes(w.value)).map((w) => w.label).join(', ');
  return `${weeks} ${days} v ${p.intervalCount > 1 ? `každém ${p.intervalCount}. měsíci` : 'měsíci'}`;
}

export function describeSync(r: SyncResult): string {
  return (
    `Termíny: vytvořeno ${r.created}, upraveno ${r.updated}, smazáno ${r.removed}` +
    (r.kept ? `, beze změny ${r.kept} (s přihláškami, zrušené nebo ručně upravené)` : '') +
    '.'
  );
}

/**
 * Opakované akce skupiny: série a jejich období (pravidlo, čas, kapacita). Uložení období
 * vygeneruje termíny celé sezóny; náhled ukáže termíny ještě před uložením.
 */
@Component({
  selector: 'app-opakovani',
  imports: [DatePipe, FormsModule, NzButtonModule, NzCardModule, NzCheckboxModule, NzDatePickerModule, NzFormModule,
    NzInputModule, NzInputNumberModule, NzModalModule, NzPopconfirmModule, NzRadioModule, NzSelectModule, NzTableModule,
    NzTagModule, NzTimePickerModule],
  templateUrl: './opakovani.component.html',
})
export class OpakovaniComponent implements OnInit {
  groupId = input.required<string>();
  /** Termíny se změnily – rodič si přenačte seznam akcí. */
  changed = output<void>();

  private seriesGQL = inject(GroupSeriesGQL);
  private previewGQL = inject(PeriodPreviewGQL);
  private seriesCreateGQL = inject(SeriesCreateGQL);
  private seriesRenameGQL = inject(SeriesRenameGQL);
  private seriesDeleteGQL = inject(SeriesDeleteGQL);
  private periodSaveGQL = inject(PeriodSaveGQL);
  private periodDeleteGQL = inject(PeriodDeleteGQL);
  private message = inject(NzMessageService);

  readonly days = DAYS;
  readonly monthWeeks = MONTH_WEEKS;
  readonly describeRule = describeRule;

  data = signal<GroupSeriesQuery['group'] | null>(null);
  organizer = computed(() => this.data()?.amOrganizer ?? false);
  saving = signal(false);
  newSeriesName = '';

  form: PeriodForm | null = null;
  preview = signal<{ date: Date; past: boolean }[] | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.seriesGQL.fetch({ variables: { id: this.groupId() }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => this.data.set(r.data?.group ?? null),
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  private run<T>(op: Observable<T>, success: (result: T) => string, after?: () => void): void {
    this.saving.set(true);
    op.subscribe({
      next: (result) => {
        this.saving.set(false);
        this.message.success(success(result), { nzDuration: 6000 });
        after?.();
        this.load();
        this.changed.emit();
      },
      error: (e) => {
        this.saving.set(false);
        this.message.error(gqlErrorMessage(e));
      },
    });
  }

  // ---- série ----

  createSeries(): void {
    const name = this.newSeriesName.trim();
    if (name) {
      this.run(this.seriesCreateGQL.mutate({ variables: { groupId: this.groupId(), name } }), () => 'Série založena – přidej jí období.',
        () => (this.newSeriesName = ''));
    }
  }

  renameSeries(s: Series, name: string): void {
    if (name.trim() && name.trim() !== s.name) {
      this.run(this.seriesRenameGQL.mutate({ variables: { id: s.id, name: name.trim() } }), () => 'Série přejmenována.');
    }
  }

  deleteSeries(s: Series): void {
    this.run(this.seriesDeleteGQL.mutate({ variables: { id: s.id } }),
      (r) => 'Série smazána. ' + describeSync(r.data!.seriesDelete));
  }

  // ---- období ----

  newPeriod(s: Series): void {
    const now = new Date();
    // výchozí sezóna září–březen (nejbližší, která ještě neskončila)
    const year = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
    this.form = {
      seriesId: s.id,
      id: null,
      range: [new Date(year, 8, 1), new Date(year + 1, 2, 31)],
      recurrence: 'WEEKLY',
      intervalCount: 1,
      days: ['FRIDAY'],
      monthWeeks: [1],
      startTime: new Date(2000, 0, 1, 20, 0),
      durationMinutes: 60,
      venueId: this.data()?.venues[0]?.id ?? null,
      maxPlayersPerTeam: 10,
      maxGoalies: 2,
      deadlineHoursBefore: 24,
      inviteRegularsHoursBefore: 96,
      inviteSubstitutesHoursBefore: 48,
      reminderHoursBefore: 3,
      note: '',
    };
    this.preview.set(null);
  }

  editPeriod(s: Series, p: Period): void {
    const [h, m] = p.startTime.split(':').map(Number);
    this.form = {
      seriesId: s.id,
      id: p.id,
      range: [new Date(p.validFrom + 'T00:00'), new Date(p.validTo + 'T00:00')],
      recurrence: p.recurrence,
      intervalCount: p.intervalCount,
      days: [...p.daysOfWeek],
      monthWeeks: p.monthWeeks.length ? [...p.monthWeeks] : [1],
      startTime: new Date(2000, 0, 1, h, m),
      durationMinutes: p.durationMinutes,
      venueId: p.venue?.id ?? null,
      maxPlayersPerTeam: p.maxPlayersPerTeam,
      maxGoalies: p.maxGoalies,
      deadlineHoursBefore: p.deadlineHoursBefore,
      inviteRegularsHoursBefore: p.inviteRegularsHoursBefore,
      inviteSubstitutesHoursBefore: p.inviteSubstitutesHoursBefore,
      reminderHoursBefore: p.reminderHoursBefore ?? null,
      note: p.note ?? '',
    };
    this.preview.set(null);
  }

  toggleDay(day: DayOfWeek, on: boolean): void {
    const f = this.form!;
    f.days = on ? [...f.days, day] : f.days.filter((d) => d !== day);
    this.preview.set(null);
  }

  toggleWeek(week: number, on: boolean): void {
    const f = this.form!;
    f.monthWeeks = on ? [...f.monthWeeks, week] : f.monthWeeks.filter((w) => w !== week);
    this.preview.set(null);
  }

  private input(): PeriodInput | null {
    const f = this.form;
    if (!f || f.range?.length !== 2 || !f.startTime || f.days.length === 0) {
      this.message.warning('Vyplň období, čas začátku a alespoň jeden den.');
      return null;
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return {
      validFrom: isoDate(f.range[0]),
      validTo: isoDate(f.range[1]),
      recurrence: f.recurrence,
      intervalCount: f.intervalCount,
      daysOfWeek: f.days,
      monthWeeks: f.recurrence === 'MONTHLY' ? f.monthWeeks : [],
      startTime: `${pad(f.startTime.getHours())}:${pad(f.startTime.getMinutes())}`,
      durationMinutes: f.durationMinutes,
      venueId: f.venueId,
      maxPlayersPerTeam: f.maxPlayersPerTeam,
      maxGoalies: f.maxGoalies,
      deadlineHoursBefore: f.deadlineHoursBefore,
      inviteRegularsHoursBefore: f.inviteRegularsHoursBefore,
      inviteSubstitutesHoursBefore: f.inviteSubstitutesHoursBefore,
      reminderHoursBefore: f.reminderHoursBefore,
      note: f.note.trim() || null,
    };
  }

  showPreview(): void {
    const input = this.input();
    if (!input) {
      return;
    }
    this.previewGQL.fetch({ variables: { input }, fetchPolicy: 'network-only' }).subscribe({
      next: (r) => {
        const now = Date.now();
        this.preview.set((r.data?.periodPreview ?? []).map((s) => ({ date: new Date(s), past: new Date(s).getTime() <= now })));
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  savePeriod(): void {
    const input = this.input();
    const f = this.form;
    if (input && f) {
      this.run(this.periodSaveGQL.mutate({ variables: { seriesId: f.seriesId, id: f.id, input } }),
        (r) => 'Období uloženo. ' + describeSync(r.data!.periodSave), () => (this.form = null));
    }
  }

  deletePeriod(p: Period): void {
    this.run(this.periodDeleteGQL.mutate({ variables: { id: p.id } }),
      (r) => 'Období smazáno. ' + describeSync(r.data!.periodDelete));
  }
}
