import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { relative } from '../core/format';

/** A quiet note of the last successful update; no live region, so a screen reader is not interrupted. */
@Component({
  selector: 'mb-refresh-indicator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './refresh-indicator.html',
  styleUrl: './refresh-indicator.css',
})
export class RefreshIndicator {
  readonly lastUpdated = input.required<number>();
  readonly now = input.required<number>();
  readonly intervalSec = input.required<number>();
  readonly stale = input(false);
  protected readonly ago = computed(() =>
    relative(new Date(this.lastUpdated()).toISOString(), this.now()),
  );
}
