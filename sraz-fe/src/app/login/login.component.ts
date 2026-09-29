import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzInputModule } from 'ng-zorro-antd/input';
import { first, Observable, shareReplay, switchMap } from 'rxjs';
import { AuthService, authErrorMessage, OtpRequestResponse } from '../services/auth.service';

/**
 * Přihlášení bez hesla: 1) e-mail → 2) šestimístný kód z e-mailu.
 * Nový účet vznikne až po zadání kódu a se souhlasem se zpracováním údajů.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule, NzAlertModule, NzButtonModule, NzCardModule, NzCheckboxModule, NzFormModule, NzGridModule, NzInputModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  step = signal<'email' | 'code'>('email');
  email = signal('');
  code = signal('');
  consentGiven = signal(false);
  sendingCode = signal(false);
  verifying = signal(false);
  errorMessage = signal<string | null>(null);
  resendCooldown = signal(0);

  private otpRequest$: Observable<OtpRequestResponse> | null = null;
  private resendTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.clearResendTimer());
  }

  /** Na krok s kódem přepne hned – odeslání e-mailu může chvíli trvat. */
  requestCode(): void {
    if (!this.email().trim() || this.sendingCode()) {
      return;
    }
    this.sendingCode.set(true);
    this.errorMessage.set(null);
    this.step.set('code');

    const request$ = this.auth.requestOtp(this.email().trim()).pipe(shareReplay(1));
    this.otpRequest$ = request$;
    request$.subscribe({
      next: (response) => {
        this.sendingCode.set(false);
        this.startResendCooldown(response.resendAfterSec);
      },
      error: (err) => {
        this.sendingCode.set(false);
        this.otpRequest$ = null;
        this.clearResendTimer();
        this.resendCooldown.set(0);
        this.step.set('email');
        this.errorMessage.set(authErrorMessage(err));
      },
    });
  }

  verifyCode(): void {
    const request$ = this.otpRequest$;
    if (!request$ || !this.code().trim() || this.verifying()) {
      return;
    }
    this.verifying.set(true);
    this.errorMessage.set(null);
    request$
      .pipe(
        first(),
        switchMap((r) => this.auth.verifyOtp(r.challengeUid, this.code().trim(), this.email().trim(), this.consentGiven())),
      )
      .subscribe({
        next: () => {
          this.verifying.set(false);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/', { replaceUrl: true });
        },
        error: (err) => {
          this.verifying.set(false);
          this.errorMessage.set(authErrorMessage(err));
        },
      });
  }

  backToEmail(): void {
    this.step.set('email');
    this.code.set('');
    this.errorMessage.set(null);
    this.otpRequest$ = null;
    this.clearResendTimer();
    this.resendCooldown.set(0);
  }

  private startResendCooldown(seconds: number): void {
    this.clearResendTimer();
    this.resendCooldown.set(seconds);
    this.resendTimer = setInterval(() => {
      const left = this.resendCooldown() - 1;
      this.resendCooldown.set(Math.max(0, left));
      if (left <= 0) {
        this.clearResendTimer();
      }
    }, 1000);
  }

  private clearResendTimer(): void {
    if (this.resendTimer) {
      clearInterval(this.resendTimer);
      this.resendTimer = null;
    }
  }
}
