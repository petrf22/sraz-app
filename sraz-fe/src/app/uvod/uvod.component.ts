import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PrehledComponent } from '../prehled/prehled.component';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzGridModule } from 'ng-zorro-antd/grid';

@Component({
  selector: 'app-uvod',
  imports: [RouterLink, NzButtonModule, NzGridModule, PrehledComponent],
  templateUrl: './uvod.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './uvod.component.scss'
})
export class UvodComponent {
  protected auth = inject(AuthService);
}
