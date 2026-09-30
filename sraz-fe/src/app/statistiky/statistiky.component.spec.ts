import { toCsv } from './statistiky.component';

describe('toCsv', () => {
  it('uses semicolons, Czech decimal comma and quoted text', () => {
    expect(toCsv(['Hráč', 'Kč'], [['Adam "Ája" Novák', 260.5]])).toBe('"Hráč";"Kč"\r\n"Adam ""Ája"" Novák";260,5');
  });
});
