import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzGridModule } from 'ng-zorro-antd/grid';

@Component({
  selector: 'app-uvod',
  imports: [RouterLink, NzButtonModule, NzGridModule],
  templateUrl: './uvod.component.html',
  styleUrl: './uvod.component.scss'
})
export class UvodComponent {
  protected auth = inject(AuthService);
}
