import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardApi } from '../core/dashboard-api';
import { DashboardStore, REFRESH_SECONDS } from '../core/dashboard-store';
import { Session } from '../core/session';
import { AppShell } from '../dashboard/app-shell';
import { DeploymentTable } from '../dashboard/deployment-table';
import { LastDeployment } from '../dashboard/last-deployment';
import { RefreshIndicator } from '../dashboard/refresh-indicator';
import { EmptyState, ErrorNotice, LoadingSkeleton } from '../dashboard/states';
import { StatusCard } from '../dashboard/status-card';

/** The one dashboard page: status, last deployment and history, read top-down. */
@Component({
  selector: 'mb-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DashboardStore],
  imports: [AppShell, DeploymentTable, EmptyState, ErrorNotice, LastDeployment, LoadingSkeleton, RefreshIndicator, StatusCard],
  template: `
    <mb-app-shell [login]="session.login()" [siteUrl]="store.status()?.siteUrl ?? null"
                  [sourceRepo]="store.status()?.sourceRepo ?? null" [targetRepo]="store.status()?.targetRepo ?? null"
                  (signOut)="signOut()">
      <div class="title-row">
        <div><div class="label dark">Dashboard</div><h1>Deployments</h1></div>
        @if (store.lastUpdated(); as updated) {
          <mb-refresh-indicator [lastUpdated]="updated" [now]="store.now()" [intervalSec]="refresh"
                                [stale]="store.failing()" />
        }
      </div>
      @if (session.expired()) {
        <mb-error-notice kind="session" />
      } @else if (store.failing()) {
        <mb-error-notice kind="api" [hasData]="store.deployments() !== null" />
      }
      <div [class.stale]="store.failing() || session.expired()" [attr.aria-busy]="store.loading()">
        @if (store.loading()) {
          <mb-loading-skeleton />
        }
        @if (store.status(); as status) {
          <mb-status-card [status]="status" />
          @if (store.latest(); as latest) {
            <mb-last-deployment [deployment]="latest" [previous]="previousDeployed()" [sourceRepo]="status.sourceRepo"
                                [targetRepo]="status.targetRepo" [maxAttempts]="status.maxAttempts" [now]="store.now()" />
            <mb-deployment-table [deployments]="store.deployments() ?? []" [sourceRepo]="status.sourceRepo"
                                 [targetRepo]="status.targetRepo" [maxAttempts]="status.maxAttempts" [now]="store.now()" />
          } @else {
            <mb-empty-state [sourceRepo]="status.sourceRepo" />
          }
        }
      </div>
    </mb-app-shell>
  `,
  styles: `
    .title-row { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 12px 24px; }
    .label.dark { color: var(--color-text); }
    h1 { margin: 16px 0 0; font-size: 44px; line-height: 1.12; font-weight: 700; letter-spacing: -.02em; text-transform: uppercase; }
    .stale { opacity: .55; }
    @media (max-width: 900px) { h1 { font-size: 32px; } }
  `,
})
export class DashboardPage implements OnInit {
  protected readonly store = inject(DashboardStore);
  protected readonly session = inject(Session);
  private readonly api = inject(DashboardApi);
  private readonly router = inject(Router);
  protected readonly refresh = REFRESH_SECONDS;

  /** The newest earlier run that deployed: what the site still shows after a failure. */
  protected readonly previousDeployed = computed(
    () => this.store.deployments()?.slice(1).find((d) => d.outcome === 'deployed') ?? null,
  );

  ngOnInit(): void {
    this.store.start();
  }

  protected signOut(): void {
    this.store.pause();
    this.api.logout().subscribe({
      complete: () => void this.router.navigate(['/sign-in']),
      error: () => void this.router.navigate(['/sign-in']),
    });
  }
}
