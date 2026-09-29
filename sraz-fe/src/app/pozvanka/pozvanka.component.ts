import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { finalize } from 'rxjs';
import { errorMessage, PublicApiService, PublicGroupInvite } from '../services/public-api.service';

/**
 * Pozvánka do skupiny otevřená odkazem z e-mailu – souhlas s členstvím.
 */
@Component({
  selector: 'app-pozvanka',
  imports: [RouterLink, NzAlertModule, NzButtonModule, NzGridModule, NzResultModule, NzSpinModule],
  templateUrl: './pozvanka.component.html',
})
export class PozvankaComponent {
  private route = inject(ActivatedRoute);
  private api = inject(PublicApiService);
  private token = this.route.snapshot.paramMap.get('token') ?? '';

  invite = signal<PublicGroupInvite | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  answered = signal<'accepted' | 'declined' | null>(null);

  constructor() {
    this.api
      .groupInvite(this.token)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (invite) => this.invite.set(invite),
        error: (e) => this.error.set(errorMessage(e, 'Pozvánku se nepodařilo načíst.')),
      });
  }

  respond(accept: boolean): void {
    this.saving.set(true);
    this.error.set(null);
    this.api
      .respondGroupInvite(this.token, accept)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.answered.set(accept ? 'accepted' : 'declined'),
        error: (e) => this.error.set(errorMessage(e)),
      });
  }
}
