import { Component, inject, signal, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { UserService } from '../services/user-service';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzImageModule } from 'ng-zorro-antd/image';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-mail',
  imports: [FormsModule, NzFormModule, NzInputModule, NzInputModule, NzImageModule, NzAlertModule, NzButtonModule,
    NzResultModule, NzIconModule],
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.css']
})
export class MailComponent {
  private userService = inject(UserService);

  @ViewChild('form') formRef!: NgForm;

  sendingEmail = false;
  email = '';
  sentInfo = signal<{ message: string, error: boolean, sent: boolean }>({ message: '', error: false, sent: false });

  submitForm(): void {
    console.log('MailComponent :: submit :: email:', this.email);

    this.sendingEmail = true;

    this.userService.mailToken(this.email)
      .pipe(
        finalize(() => this.sendingEmail = false)
      )
      .subscribe({
        next: () => {
          console.log('MailComponent :: Mail token sent successfully.');

          this.formRef.form.markAsPristine();
          this.formRef.form.markAsUntouched();
          this.sentInfo.set({ message: 'E-mail byl úspěšně odeslán.', error: false, sent: true });
        },
        error: (error) => {
          console.error('MailComponent :: Error sending mail token:', error);
          const message = error?.error ?? 'Chyba při odesílání e-mailu.';
          this.sentInfo.set({ message, error: true, sent: true });
        }
      });
  }
}
