import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Session } from './session';

/** A 401 from the API marks the session as expired; a 403 other than a CSRF refusal opens the denied page. */
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(Session);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && request.url.startsWith('/api/')) {
        if (error.status === 401) {
          session.expire();
        } else if (error.status === 403 && request.method === 'GET') {
          void router.navigate(['/denied']);
        }
      }
      return throwError(() => error);
    }),
  );
};
