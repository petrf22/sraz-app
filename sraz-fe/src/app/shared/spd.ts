/**
 * Řetězec české QR platby (Short Payment Descriptor, SPD 1.0), který čtou bankovní aplikace.
 * https://qr-platba.cz/pro-vyvojare/specifikace-formatu/
 */
export function spdPayment(iban: string, amount: number, variableSymbol: string | number, message: string): string {
  const msg = message
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // bez diakritiky – ne všechny banky ji v MSG zobrazí
    .replace(/[*]/g, ' ') // hvězdička odděluje pole SPD
    .toUpperCase()
    .slice(0, 60);
  const vs = String(variableSymbol).replace(/\D/g, '').slice(0, 10);
  return `SPD*1.0*ACC:${iban.replace(/\s/g, '')}*AM:${amount.toFixed(2)}*CC:CZK*X-VS:${vs}*MSG:${msg}`;
}
