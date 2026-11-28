import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Subscription, catchError, forkJoin, of, switchMap, timer } from 'rxjs';
import { DashboardApi } from './dashboard-api';
import { Deployment, ServiceStatus } from './models';
import { Session } from './session';

export const REFRESH_SECONDS = 15;
export const LIMIT = 50;

/**
 * Polls the status and the deployments every 15 s while the tab is visible. A failed poll keeps the last
 * data and marks it stale; an ended session stops the polling.
 */
@Injectable()
export class DashboardStore {
  private readonly api = inject(DashboardApi);
  private readonly session = inject(Session);
  private polling: Subscription | null = null;
  private ticking: ReturnType<typeof setInterval> | null = null;

  readonly status = signal<ServiceStatus | null>(null);
  readonly deployments = signal<Deployment[] | null>(null);
  readonly lastUpdated = signal<number | null>(null);
  readonly failing = signal(false);
  readonly now = signal(Date.now());
  readonly loading = computed(() => this.deployments() === null && !this.failing());
  readonly latest = computed(() => this.deployments()?.[0] ?? null);

  constructor() {
    const onVisibility = () => (document.hidden ? this.pause() : this.start());
    document.addEventListener('visibilitychange', onVisibility);
    inject(DestroyRef).onDestroy(() => {
      document.removeEventListener('visibilitychange', onVisibility);
      this.pause();
    });
  }

  /** Starts polling at once, unless it already runs or the session has ended. */
  start(): void {
    if (this.polling || this.session.expired()) {
      return;
    }
    this.ticking = setInterval(() => this.now.set(Date.now()), 1000);
    this.polling = timer(0, REFRESH_SECONDS * 1000)
      .pipe(
        switchMap(() =>
          forkJoin({ status: this.api.status(), deployments: this.api.deployments(LIMIT) }).pipe(
            catchError(() => of(null)),
          ),
        ),
      )
      .subscribe((answer) => {
        if (this.session.expired()) {
          this.pause();
          return;
        }
        if (answer === null) {
          this.failing.set(true);
          return;
        }
        this.status.set(answer.status);
        this.deployments.set(answer.deployments);
        this.lastUpdated.set(Date.now());
        this.failing.set(false);
      });
  }

  pause(): void {
    this.polling?.unsubscribe();
    this.polling = null;
    if (this.ticking) {
      clearInterval(this.ticking);
      this.ticking = null;
    }
  }
}
