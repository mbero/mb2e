import { Routes } from '@angular/router';
import { signedInGuard } from './core/signed-in-guard';
import { AccessDeniedPage } from './pages/access-denied-page';
import { DashboardPage } from './pages/dashboard-page';
import { SignInPage } from './pages/sign-in-page';

export const routes: Routes = [
  { path: '', component: DashboardPage, canActivate: [signedInGuard], title: 'Deployments — mb2e' },
  { path: 'sign-in', component: SignInPage, title: 'Sign in — mb2e' },
  { path: 'denied', component: AccessDeniedPage, title: 'No access — mb2e' },
  { path: '**', redirectTo: '' },
];
