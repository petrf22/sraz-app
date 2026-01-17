import { DatePipe, JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzImageModule } from 'ng-zorro-antd/image';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { UserService } from '../services/user-service';
import { Router } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';

@Component({
  selector: 'app-logout-component',
  imports: [FormsModule, NzFormModule, NzInputModule, NzInputModule, NzImageModule, NzAlertModule, NzButtonModule,
    NzResultModule, NzUploadModule, NzIconModule, NzDividerModule, NzGridModule, NzCheckboxModule, NzDividerModule,
    NzModalModule],
  templateUrl: './logout.component.html',
  styleUrl: './logout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogoutComponent {
  readonly smazatUcetInfo = "Smazání účtu je nevratná akce, která smaže všechny vaše údaje z této aplikace.";

  private userService = inject(UserService);
  private router = inject(Router);
  private messageService = inject(NzMessageService);
  private modalService = inject(NzModalService);

  smazatUcet = false

  submitForm() {
    this.modalService.confirm({
      nzTitle: this.smazatUcet ? 'Potvrzení smazání účtu' : 'Potvrzení odhlášení',
      nzContent: this.smazatUcet ? `<b style="color: red;">${this.smazatUcetInfo}</b>` : 'Pro nové přihlášení budete potřebovat ověření přes e-mail',
      nzOkText: 'Ano',
      nzOkType: 'primary',
      nzOkDanger: true,
      nzOnOk: () => this.logout(),
      nzCancelText: 'Ne',
      nzOnCancel: () => console.log('Cancel')
    });
  }

  logout(): void {
    this.userService.logout(this.smazatUcet).subscribe({
      next: (response) => {
        console.log('LogoutComponent :: Logout successful:', response);

        this.messageService.success(this.smazatUcet ? 'Smazání účtu proběhlo úspěšně.' : 'Odhlášení proběhlo úspěšně.');

        this.router.navigate(['/'], { replaceUrl: true });
      },
      error: (error) => {
        console.error('LogoutComponent :: Error during logout:', error);
        this.messageService.error('Chyba při odhlášení.');
      }
    });
  }
}
