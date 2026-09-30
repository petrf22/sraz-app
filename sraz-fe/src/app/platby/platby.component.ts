import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { toDataURL } from 'qrcode';
import { MyChargesGQL, MyChargesQuery } from '../graphql/money.generated';
import { CHARGE_REASON, gqlErrorMessage } from '../shared/labels';
import { spdPayment } from '../shared/spd';

type Charge = MyChargesQuery['myCharges'][number];

/**
 * Moje platby za akce – nezaplacené s QR platbou (IBAN skupiny + variabilní symbol = číslo platby).
 */
@Component({
  selector: 'app-platby',
  imports: [DatePipe, DecimalPipe, RouterLink, NzCardModule, NzEmptyModule, NzGridModule, NzTagModule],
  templateUrl: './platby.component.html',
})
export class PlatbyComponent {
  private chargesGQL = inject(MyChargesGQL);
  private message = inject(NzMessageService);
  readonly chargeReason = CHARGE_REASON;

  charges = signal<Charge[] | null>(null);
  qr = signal(new Map<string, string>());
  unpaid = computed(() => (this.charges() ?? []).filter((c) => !c.paidAt));
  paid = computed(() => (this.charges() ?? []).filter((c) => c.paidAt));
  toPay = computed(() => this.unpaid().reduce((sum, c) => sum + c.amount, 0));

  constructor() {
    this.chargesGQL.fetch({ fetchPolicy: 'network-only' }).subscribe({
      next: (r) => {
        const charges = r.data?.myCharges ?? [];
        this.charges.set(charges);
        this.renderQr(charges.filter((c) => !c.paidAt && c.iban));
      },
      error: (e) => this.message.error(gqlErrorMessage(e)),
    });
  }

  spd(c: Charge): string {
    const date = new Date(c.event.startsAt);
    return spdPayment(c.iban!, c.amount, c.id, `${c.event.name} ${date.getDate()}. ${date.getMonth() + 1}.`);
  }

  private renderQr(charges: Charge[]): void {
    for (const c of charges) {
      toDataURL(this.spd(c), { errorCorrectionLevel: 'M', margin: 1, width: 180 }).then((url) =>
        this.qr.update((m) => new Map(m).set(c.id, url)),
      );
    }
  }
}
