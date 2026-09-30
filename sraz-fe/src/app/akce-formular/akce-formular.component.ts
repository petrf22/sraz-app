import { Component, effect, input, output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { EventInput } from '../graphql/graphql-types';

/** Výchozí hodnoty odpovídají backendu (EventService.DEFAULT_*). */
export interface EventFormValue {
  name: string;
  startsAt: Date | null;
  durationMinutes: number;
  venueId: string | null;
  maxPlayersPerTeam: number;
  maxGoalies: number;
  signupDeadline: Date | null;
  inviteRegularsHoursBefore: number;
  inviteSubstitutesHoursBefore: number;
  /** null = bez připomínky */
  reminderHoursBefore: number | null;
  /** Kč za hodinu, null = zdarma */
  pricePerHour: number | null;
  regularFee: number | null;
  note: string;
}

export function emptyEventForm(): EventFormValue {
  return {
    name: '',
    startsAt: null,
    durationMinutes: 60,
    venueId: null,
    maxPlayersPerTeam: 10,
    maxGoalies: 2,
    signupDeadline: null,
    inviteRegularsHoursBefore: 96,
    inviteSubstitutesHoursBefore: 48,
    reminderHoursBefore: 3,
    pricePerHour: null,
    regularFee: null,
    note: '',
  };
}

/**
 * Formulář termínu akce – pro založení i úpravu.
 */
@Component({
  selector: 'app-akce-formular',
  imports: [FormsModule, NzButtonModule, NzDatePickerModule, NzFormModule, NzInputModule, NzInputNumberModule, NzSelectModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './akce-formular.component.html',
})
export class AkceFormularComponent {
  venues = input<{ id: string; name: string }[]>([]);
  initial = input<EventFormValue>(emptyEventForm());
  saving = input(false);
  submitLabel = input('Uložit');
  submitted = output<EventInput>();

  value: EventFormValue = emptyEventForm();

  constructor() {
    effect(() => (this.value = { ...this.initial() }));
  }

  submit(): void {
    const v = this.value;
    if (!v.name.trim() || !v.startsAt) {
      return;
    }
    this.submitted.emit({
      name: v.name.trim(),
      startsAt: v.startsAt.toISOString(),
      durationMinutes: v.durationMinutes,
      venueId: v.venueId,
      maxPlayersPerTeam: v.maxPlayersPerTeam,
      maxGoalies: v.maxGoalies,
      signupDeadline: v.signupDeadline ? v.signupDeadline.toISOString() : null,
      inviteRegularsHoursBefore: v.inviteRegularsHoursBefore,
      inviteSubstitutesHoursBefore: v.inviteSubstitutesHoursBefore,
      reminderHoursBefore: v.reminderHoursBefore,
      pricePerHour: v.pricePerHour,
      regularFee: v.regularFee,
      note: v.note.trim() || null,
    });
  }
}
