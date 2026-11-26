import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { DashboardApi } from './dashboard-api';
import { Session } from './session';

/** Opens the dashboard only with a session; otherwise it shows the sign-in page. */
export const signedInGuard: CanActivateFn = () => {
  const api = inject(DashboardApi);
  const session = inject(Session);
  const router = inject(Router);
  return api.me().pipe(
    map((me) => {
      session.signedIn(me.login);
      return true;
    }),
    catchError(() => of(router.parseUrl('/sign-in'))),
  );
};
