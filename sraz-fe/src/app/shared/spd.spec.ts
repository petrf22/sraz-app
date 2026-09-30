import { spdPayment } from './spd';

describe('spdPayment', () => {
  it('builds a Czech QR payment string', () => {
    expect(spdPayment('CZ65 0800 0000 1920 0014 5399', 280, 42, 'Večerní hokej 2. 10.')).toBe(
      'SPD*1.0*ACC:CZ6508000000192000145399*AM:280.00*CC:CZK*X-VS:42*MSG:VECERNI HOKEJ 2. 10.',
    );
  });

  it('keeps the message short and without field separators', () => {
    const spd = spdPayment('CZ6508000000192000145399', 140.5, '7', '*'.repeat(80));
    expect(spd).toContain('AM:140.50');
    expect(spd.split('*MSG:')[1]).toBe(' '.repeat(60));
  });
});
