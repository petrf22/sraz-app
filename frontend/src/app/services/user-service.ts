import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { Token } from '../models/token';
import { TextContent } from '../models/text-content';
import { NzUploadFile } from 'ng-zorro-antd/upload';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private token = signal<Token | null>(null);

  mailToken(email: string): Observable<void> {
    console.log('UserService :: mailToken :: email:', email);

    const headers = new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded')
    const params = new HttpParams().set('email', email);

    return this.http.post<void>('/api/auth/mail-token', params, { headers });
  }

  verifyToken(emailToken: string) {
    console.log('UserService :: verifyToken :: emailToken:', emailToken);

    return this.http.get<Token>(`/api/auth/verify/${emailToken}`)
      .pipe(
        tap(token => {
          console.log('UserService :: verifyToken :: token:', token);
          this.token.set(token);
        })
      );
  }

  get tokenSig(): Signal<Token | null> {
    return this.token.asReadonly();
  }

  updateTextPrani(text: string): Observable<TextContent> {
    console.log('UserService :: updateTextPrani :: text:', text);

    return this.http.post<TextContent>('/api/text', { content: text });
  }

  getTextContent(): Observable<TextContent> {
    console.log('UserService :: getTextContent');

    return this.http.get<TextContent>('/api/text');
  }

  uploadFotoPrani(file: File) {
    console.log('UserService :: updateFotoPrani :: file:', file.name);

    const formData = new FormData();

    formData.append('file', file);

    return this.http.post('/api/photo', formData);
  }

  deleteFotoPrani() {
    console.log('UserService :: deleteFotoPrani');

    return this.http.delete('/api/photo');
  }


  /**
   * Získá fotografii jako Data URL (base64)
   */
  getFotoPrani(): Observable<NzUploadFile> {
    return this.http.get('/api/photo', {
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      map(response => {
        const headers = response.headers;
        const arrayBuffer = response.body as ArrayBuffer;

        // Získání metadat z hlaviček
        const contentType = headers.get('Content-Type') || 'image/jpeg';
        const contentLength = parseInt(headers.get('Content-Length') || '0', 10);
        const fileName = headers.get('X-Photo-Filename') ?? 'unknown.jpg';
        const uid = headers.get('X-Photo-Id');
        const photoId = parseInt(uid || '0', 10);
        const photocreatedAt = new Date(headers.get('X-Photo-Uploaded-At') ?? '');

        // Konverze ArrayBuffer na Base64
        const base64 = this.arrayBufferToBase64(arrayBuffer);

        // Vytvoření Data URL
        const dataUrl = `data:${contentType};base64,${base64}`;

        console.log(`UserService :: getFotoPrani :: Načtena fotografie: ${fileName}, typ: ${contentType}, velikost: ${contentLength}, photoId: ${photoId}, photocreatedAt: ${photocreatedAt}`);

        return {
          uid: uid || '',
          name: fileName,
          fileSize: contentLength,
          type: contentType,
          status: 'done',
          response: {status: "success", createdAt: photocreatedAt},
          linkProps: {download: "image"},
          url: dataUrl
        } as NzUploadFile;

        // return {
        //   dataUrl: dataUrl,
        //   fileName: fileName,
        //   contentType: contentType,
        //   fileSize: contentLength,
        //   base64Data: base64,
        //   photoId: photoId,
        //   createdAt: photocreatedAt
        // };
      })/*,
      catchError(error => {
        console.error('Chyba při načítání fotografie:', error);
        if (error.status === 404) {
          return throwError(() => new Error('Fotografie nebyla nalezena'));
        }
        return throwError(() => error);
      })*/
    );
  }

  /**
 * Pomocná metoda pro konverzi ArrayBuffer na Base64 string
 */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    const len = bytes.byteLength;

    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }

    return btoa(binary);
  }

  getFotoInfo() {
    console.log('UserService :: getFotoInfo');

    return this.http.get('/api/photo/info');
  }

  logout(smazatUcet: boolean) {
    console.log('UserService :: logout');
    this.token.set(null);

    const formData = new FormData();

    formData.append('deleteAccount', smazatUcet.toString());

    return this.http.post('/api/auth/logout', formData);
  }

  refreshToken(): Observable<Token> {
    console.log('UserService :: refreshToken');

    return this.http.post<Token>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(
        tap(token => {
          console.log('UserService :: verifyToken :: token:', token);
          this.token.set(token);
        })
      );

  }

}
