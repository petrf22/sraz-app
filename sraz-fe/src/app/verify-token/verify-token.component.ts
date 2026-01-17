import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { UserService } from '../services/user-service';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzAlertModule } from 'ng-zorro-antd/alert';

@Component({
  selector: 'app-verify-token',
  imports: [FormsModule, NzFormModule, NzInputModule, NzInputModule, NzAlertModule, RouterLink],
  templateUrl: './verify-token.component.html',
  styleUrls: ['./verify-token.component.css']
})
export class VerifyTokenComponent {
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private messageService = inject(NzMessageService);
  errorVerify = false;

  constructor() {
    // Access route parameters
    this.activatedRoute.params.subscribe((params) => {
      const emailToken = params['emailToken'];

      console.log('VerifyTokenComponent :: activatedRoute :: emailToken:', emailToken);

      this.userService.verifyToken(emailToken).subscribe({
        next: (response) => {
          console.log('VerifyTokenComponent :: Verify token successfully:', response);
          this.messageService.success('E-mail byl úspěšně ověřen.');

          this.router.navigate(['/'], { replaceUrl: true });
        },
        error: (error) => {
          this.errorVerify = true;
          console.error('VerifyTokenComponent :: Error verifying token:', error);
          this.messageService.error('Chyba při ověřování tokenu z e-mailu.');
        }
      });
    });
  }
}
