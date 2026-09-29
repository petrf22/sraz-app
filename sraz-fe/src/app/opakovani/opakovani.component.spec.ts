import { describeRule, describeSync, isoDate } from './opakovani.component';

describe('opakovani helpers', () => {
  it('formats date in local time without shifting through UTC', () => {
    expect(isoDate(new Date(2026, 8, 1, 0, 0))).toBe('2026-09-01');
    expect(isoDate(new Date(2027, 2, 31, 23, 59))).toBe('2027-03-31');
  });

  it('describes weekly and monthly rules in Czech', () => {
    expect(describeRule({ recurrence: 'WEEKLY', intervalCount: 1, daysOfWeek: ['FRIDAY'], monthWeeks: [] })).toBe(
      'každý týden: pátek',
    );
    expect(
      describeRule({ recurrence: 'WEEKLY', intervalCount: 2, daysOfWeek: ['THURSDAY', 'MONDAY'], monthWeeks: [] }),
    ).toBe('každý 2. týden: pondělí a čtvrtek');
    expect(describeRule({ recurrence: 'MONTHLY', intervalCount: 1, daysOfWeek: ['FRIDAY'], monthWeeks: [1, 3] })).toBe(
      '1., 3. pátek v měsíci',
    );
    expect(describeRule({ recurrence: 'MONTHLY', intervalCount: 1, daysOfWeek: ['FRIDAY'], monthWeeks: [-1] })).toBe(
      'poslední pátek v měsíci',
    );
  });

  it('explains what happened to the events', () => {
    expect(describeSync({ created: 30, updated: 0, removed: 0, kept: 0 })).toBe(
      'Termíny: vytvořeno 30, upraveno 0, smazáno 0.',
    );
    expect(describeSync({ created: 0, updated: 5, removed: 1, kept: 2 })).toContain('beze změny 2');
  });
});
