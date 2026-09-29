import { Component, inject } from '@angular/core';
import { MailComponent } from "../mail/mail.component";
import { UserService } from '../services/user-service';
import { NzGridModule } from 'ng-zorro-antd/grid';

@Component({
  selector: 'app-uvod',
  imports: [MailComponent, NzGridModule],
  templateUrl: './uvod.component.html',
  styleUrl: './uvod.component.scss'
})
export class UvodComponent {
  protected userService = inject(UserService);
}
