import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardApi } from '../core/dashboard-api';
import { DashboardStore, REFRESH_SECONDS } from '../core/dashboard-store';
import { Session } from '../core/session';
import { AppShell } from '../dashboard/app-shell';
import { DeploymentTable } from '../dashboard/deployment-table';
import { EmptyState } from '../dashboard/empty-state';
import { ErrorNotice } from '../dashboard/error-notice';
import { LastDeployment } from '../dashboard/last-deployment';
import { LoadingSkeleton } from '../dashboard/loading-skeleton';
import { RefreshIndicator } from '../dashboard/refresh-indicator';
import { StatusCard } from '../dashboard/status-card';

/** The one dashboard page: status, last deployment and history, read top-down. */
@Component({
  selector: 'mb-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [DashboardStore],
  imports: [
    AppShell,
    DeploymentTable,
    EmptyState,
    ErrorNotice,
    LastDeployment,
    LoadingSkeleton,
    RefreshIndicator,
    StatusCard,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPage implements OnInit {
  protected readonly store = inject(DashboardStore);
  protected readonly session = inject(Session);
  private readonly api = inject(DashboardApi);
  private readonly router = inject(Router);
  protected readonly refresh = REFRESH_SECONDS;

  /** The newest earlier run that deployed: what the site still shows after a failure. */
  protected readonly previousDeployed = computed(
    () =>
      this.store
        .deployments()
        ?.slice(1)
        .find((d) => d.outcome === 'deployed') ?? null,
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
