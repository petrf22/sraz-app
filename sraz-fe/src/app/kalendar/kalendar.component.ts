import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzCalendarModule } from 'ng-zorro-antd/calendar';
import { NzMessageService } from 'ng-zorro-antd/message';
import { GroupCalendarGQL, GroupCalendarQuery } from '../graphql/series.generated';
import { gqlErrorMessage, REGISTRATION_STATUS } from '../shared/labels';

type CalendarEvent = GroupCalendarQuery['group']['events'][number];

/** Klíč dne v místním čase (yyyy-mm-dd). */
function dayKey(d: Date): string {
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
}

/**
 * Měsíční kalendář akcí skupiny – načítá termíny zobrazeného měsíce (s přesahem sousedních týdnů).
 */
@Component({
  selector: 'app-kalendar',
  imports: [DatePipe, FormsModule, RouterLink, NzBadgeModule, NzCalendarModule],
  templateUrl: './kalendar.component.html',
  styleUrl: './kalendar.component.scss',
})
export class KalendarComponent implements OnInit {
  groupId = input.required<string>();

  private calendarGQL = inject(GroupCalendarGQL);
  private message = inject(NzMessageService);

  readonly regStatus = REGISTRATION_STATUS;
  selected = new Date();
  private byDay = signal(new Map<string, CalendarEvent[]>());
  /** Načtený měsíc (rok-měsíc) – ngModel mění `selected` dřív, než přijde nzSelectChange. */
  private loadedMonth = '';

  ngOnInit(): void {
    this.load(this.selected);
  }

  eventsOn(date: Date): CalendarEvent[] {
    return this.byDay().get(dayKey(date)) ?? [];
  }

  badge(e: CalendarEvent): 'success' | 'warning' | 'error' | 'default' | 'processing' {
    if (e.status === 'CANCELLED') {
      return 'error';
    }
    switch (e.myRegistration?.status) {
      case 'IN':
        return 'success';
      case 'WAITLIST':
        return 'warning';
      case 'OUT':
        return 'default';
      default:
        return 'processing';
    }
  }

  onPanelChange(change: { date: Date }): void {
    this.load(change.date);
  }

  onSelect(date: Date): void {
    this.load(date);
  }

  private load(month: Date): void {
    const key = `${month.getFullYear()}-${month.getMonth()}`;
    if (key === this.loadedMonth) {
      return;
    }
    this.loadedMonth = key;
    const from = new Date(month.getFullYear(), month.getMonth(), 1 - 7);
    const to = new Date(month.getFullYear(), month.getMonth() + 1, 7);
    this.calendarGQL
      .fetch({ variables: { id: this.groupId(), from: from.toISOString(), to: to.toISOString() }, fetchPolicy: 'network-only' })
      .subscribe({
        next: (r) => {
          const map = new Map<string, CalendarEvent[]>();
          for (const e of r.data?.group.events ?? []) {
            const key = dayKey(new Date(e.startsAt));
            map.set(key, [...(map.get(key) ?? []), e]);
          }
          this.byDay.set(map);
        },
        error: (e) => this.message.error(gqlErrorMessage(e)),
      });
  }
}
