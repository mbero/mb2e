import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { relative } from '../core/format';

/** A quiet note of the last successful update; no live region, so a screen reader is not interrupted. */
@Component({
  selector: 'mb-refresh-indicator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
      <path d="M13.5 8A5.5 5.5 0 1 1 11.8 4M13.5 2.5V5H11" /></svg>
    @if (stale()) {
      Last updated {{ ago() }}
    } @else {
      Updated {{ ago() }} · refreshes every {{ intervalSec() }} s
    }
  `,
  styles: `:host { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--color-muted); }`,
})
export class RefreshIndicator {
  readonly lastUpdated = input.required<number>();
  readonly now = input.required<number>();
  readonly intervalSec = input.required<number>();
  readonly stale = input(false);
  protected readonly ago = computed(() => relative(new Date(this.lastUpdated()).toISOString(), this.now()));
}
