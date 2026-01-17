import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NzGridModule } from 'ng-zorro-antd/grid';

@Component({
  selector: 'app-onas',
  imports: [NzGridModule],
  template: `
    <div nz-row [nzGutter]="[16, 16]">
      <div nz-col nzXs="24" [nzMd]="{ span: 12, offset: 6 }">
        <h2>O nás ...</h2>
      </div>
      <div nz-col nzXs="24" [nzMd]="{ span: 12, offset: 6 }">
        Udělat dobrou věc, je správné.
      </div>
      <div nz-col nzXs="24" [nzMd]="{ span: 12, offset: 6 }">
        Kontakt: petr.franta@gmail.com
      </div>
    </div>
  `,
})
export class ONasComponent { }
