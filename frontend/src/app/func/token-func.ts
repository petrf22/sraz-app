import { HttpEvent, HttpEventType, HttpHandlerFn, HttpRequest } from "@angular/common/http";
import { Observable, tap } from "rxjs";
import { UserService } from "../services/user-service";
import { inject } from "@angular/core";

export function tokenInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  const userService = inject(UserService);
  const token = userService.tokenSig();

  if (token) {
    req = req.clone({
      setHeaders: {
        'Authorization': `Bearer ${token.accessToken}`,
      },
    });
  }

  return next(req).pipe(
    tap((event) => {
      if (event.type === HttpEventType.Response) {
        console.log(req.url, 'returned a response with status', event.status);
      }
    }),
  );
}