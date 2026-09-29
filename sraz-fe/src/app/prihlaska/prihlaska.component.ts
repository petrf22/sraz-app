import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { finalize } from 'rxjs';
import {
  errorMessage,
  PublicApiService,
  PublicInvitation,
  PublicTeam,
  RegistrationStatus,
} from '../services/public-api.service';

/** Volba předvybraná tlačítkem v e-mailu – potvrzuje se až tady (e-mailové skenery odkazy samy otevírají). */
interface PendingChoice {
  status: RegistrationStatus;
  team: PublicTeam | null;
}

/**
 * Přihláška na jeden termín přes osobní odkaz z e-mailu. Nepřihlašuje do aplikace.
 */
@Component({
  selector: 'app-prihlaska',
  imports: [DatePipe, NzAlertModule, NzButtonModule, NzCardModule, NzGridModule, NzIconModule, NzSpinModule, NzTagModule],
  templateUrl: './prihlaska.component.html',
  styleUrl: './prihlaska.component.scss',
})
export class PrihlaskaComponent {
  private route = inject(ActivatedRoute);
  private api = inject(PublicApiService);
  private token = this.route.snapshot.paramMap.get('token') ?? '';

  invitation = signal<PublicInvitation | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  saved = signal<string | null>(null);
  pending = signal<PendingChoice | null>(null);

  /** Soupiska rozdělená podle týmů + brankáři + fronta. */
  columns = computed(() => {
    const inv = this.invitation();
    if (!inv) {
      return [];
    }
    const inPlayers = inv.roster.filter((r) => r.status === 'IN' && r.position === 'PLAYER');
    const cols = inv.teams.map((t) => ({
      title: t.name,
      color: t.color,
      names: inPlayers.filter((r) => r.teamId === t.id).map((r) => r.name),
    }));
    if (inv.teams.length === 0) {
      cols.push({ title: 'Hráči', color: null, names: inPlayers.map((r) => r.name) });
    }
    cols.push({
      title: 'Brankáři',
      color: null,
      names: inv.roster.filter((r) => r.status === 'IN' && r.position === 'GOALIE').map((r) => r.name),
    });
    return cols;
  });

  waitlist = computed(() =>
    (this.invitation()?.roster ?? []).filter((r) => r.status === 'WAITLIST').map((r) => r.name),
  );

  myTeamName = computed(() => {
    const inv = this.invitation();
    return inv?.teams.find((t) => t.id === inv.myTeamId)?.name ?? null;
  });

  constructor() {
    this.api
      .invitation(this.token)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (inv) => {
          this.invitation.set(inv);
          this.preselectFromEmail(inv);
        },
        error: (e) => this.error.set(errorMessage(e, 'Pozvánku se nepodařilo načíst.')),
      });
  }

  private preselectFromEmail(inv: PublicInvitation): void {
    const params = this.route.snapshot.queryParamMap;
    const status = params.get('volba');
    if (!inv.signupOpen || (status !== 'IN' && status !== 'OUT')) {
      return;
    }
    const teamId = Number(params.get('tym'));
    this.pending.set({ status, team: inv.teams.find((t) => t.id === teamId) ?? null });
  }

  choose(status: RegistrationStatus, team: PublicTeam | null = null): void {
    this.pending.set(null);
    this.saving.set(true);
    this.error.set(null);
    this.api
      .respond(this.token, status, team?.id ?? null)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (inv) => {
          this.invitation.set(inv);
          this.saved.set(
            inv.myStatus === 'IN'
              ? 'Jste přihlášen(a).'
              : inv.myStatus === 'WAITLIST'
                ? 'Kapacita je plná – jste ve frontě. Pokud se uvolní místo, přijde vám e-mail.'
                : 'Jste odhlášen(a).',
          );
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
  }

  cancelPending(): void {
    this.pending.set(null);
  }
}
