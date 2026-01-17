import { Component, computed, inject, OnInit, signal, ViewChild, WritableSignal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';

import { NzFormModule } from 'ng-zorro-antd/form';
import { NzImageModule } from 'ng-zorro-antd/image';
import { NzInputModule } from 'ng-zorro-antd/input';
import { UserService } from '../services/user-service';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzUploadChangeParam, NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { TextContent as TextContent } from '../models/text-content';
import { DatePipe } from '@angular/common';
import { PhotoInfo } from '../models/photo-info';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { max, isDate } from "date-fns";

const fileToDataURL = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = error => reject(error);
  });

@Component({
  selector: 'app-prani',
  imports: [FormsModule, NzFormModule, NzInputModule, NzInputModule, NzImageModule, NzAlertModule, NzButtonModule,
    NzResultModule, NzUploadModule, NzIconModule, NzDividerModule, NzGridModule, NzSpaceModule, DatePipe],
  templateUrl: './prani.component.html',
  styleUrls: ['./prani.component.css']
})
export class PraniComponent implements OnInit {
  private userService = inject(UserService);
  private messageService = inject(NzMessageService);

  readonly fallback = 'data:image/png;base64,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';
  readonly maxTextLength = 350;

  @ViewChild('form') formRef!: NgForm;

  // model = {
  //   fotoFile = signal<File | null>(null);
  //   textPrani = signal('');

  //   email: ''
  // };

  fotoInfo = signal<PhotoInfo | null>(null);
  previewUrl = signal('');
  textPrani = signal('');
  sentInfo = signal('');
  textContent = signal<TextContent | null>(null);
  fileList = signal<NzUploadFile[]>([]);
  aktualizace = computed(() => {
    const photoUpdatedAtText = this.fileList()[0]?.response?.updatedAt;
    const photoUpdatedAt = photoUpdatedAtText ? new Date(photoUpdatedAtText!) : null;
    const textUpdatedAtText = this.textContent()?.updatedAt;
    const textUpdatedAt = textUpdatedAtText ? new Date(textUpdatedAtText!) : null;

    if (isDate(photoUpdatedAt) && isDate(textUpdatedAt)) {
      return max([photoUpdatedAt!, textUpdatedAt!]);
    } else if (isDate(photoUpdatedAt)) {
      return photoUpdatedAt;
    } else if (isDate(textUpdatedAt)) {
      return textUpdatedAt;
    } else {
      return null;
    }
  });

  ngOnInit(): void {
    console.log('PraniFormComponent :: ngOnInit');
    this.userService.getFotoPrani().subscribe({
      next: (nzUploadFile) => {
        this.fileList.set([nzUploadFile]);
        this.previewUrl.set(nzUploadFile.url || '');
      },
      error: (error) => console.error('Chyba:', error)
    });
    this.userService.getTextContent().subscribe({
      next: (textContent) => {
        console.log('textContent:', textContent);
        this.textContent.set(textContent);
        this.textPrani.set(textContent.content);
      },
      error: (error) => console.error('Chyba:', error)
    });
  }

  submitForm(): void {
    console.log('PraniFormComponent :: submitForm :: textPrani:', this.textPrani);

    this.userService.updateTextPrani(this.textPrani()).subscribe({
      next: (textContent) => {
        console.log('PraniFormComponent :: submitForm sent successfully:', textContent);
        //this.formRef.reset();
        this.textContent.set(textContent);
        //this.textPrani.set(textContent.content);

        this.formRef.form.markAsPristine();
        this.formRef.form.markAsUntouched();

        this.messageService.success('Formulář byl úspěšně odeslán.');
      },
      error: (error) => {
        console.error('PraniFormComponent :: Error sending submitForm:', error);
        this.messageService.error('Chyba při odesílání formuláře.');
      }
    });
  }

  async handleChange(info: NzUploadChangeParam): Promise<void> {
    // console.log('handleChange :: info:', info);

    let fileList = [...info.fileList];

    // Limit - jeden soubor
    fileList = fileList.slice(-1);

    if (info.file.status !== 'uploading') {
      console.log('handleChange :: info:', info);
    }
    if (info.file.status === 'done') {
      this.messageService.success(`Fotografie ${info.file.name} byl úspěšně nahrán`);
      const imgUrl = await fileToDataURL(info.file.originFileObj!);
      this.previewUrl.set(imgUrl);
      // info.file.response
      // } else if (info.file.status === 'removed') {
      //   this.userService.deleteFotoPrani().subscribe({
      //     next: () => {
      //       this.previewUrl.set(null);
      //       this.messageService.success('Fotografie byla úspěšně odstraněna');
      //     },
      //     error: () => {
      //       this.messageService.error('Odstranění fotografie se nezdařilo');
      //     }
      //   });
    } else if (info.file.status === 'error') {
      const message = info.file.error?.error?.error || info.file.error?.statusText || 'Neočekávaná chyba';
      const errMessage = `Chyba při nahrávání fotografie ${info.file.name}: ${message}`;
      console.error(errMessage);
      this.messageService.error(errMessage);
    }

    this.fileList.set(fileList);
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
      this.messageService.error('Vkládat se mohou jen obrázky typu JPG a PNG');
      return false;
    } else if (file.size === undefined) {
      this.messageService.error('Nebylo možné zjistit velikost souboru');
      return false;
    } else if (file.size > 10 * 1024 * 1024) {
      this.messageService.error('Velikost souboru nesmí přesáhnout 10MB');
      return false;
    }
    return true;
  };


  removeFoto = (file: NzUploadFile): boolean | Observable<boolean> => {
    console.log('removeFoto :: file:', file);
    return this.userService.deleteFotoPrani().pipe(
      map(() => {
        const fileList = this.fileList().filter(f => f.uid !== file.uid)
        this.fileList.set(fileList);
        this.previewUrl.set('');
        return true;
      }),
      tap(() => this.messageService.success('Fotografie byla úspěšně odstraněna')),
      catchError(() => of(false))
    );

  };
}
