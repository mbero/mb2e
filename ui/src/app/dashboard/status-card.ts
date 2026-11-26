import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { hostOf, uptime } from '../core/format';
import { ServiceStatus } from '../core/models';

/** The service: worker state, uptime, the two repositories and the published site. */
@Component({
  selector: 'mb-status-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './status-card.html',
  styleUrl: './status-card.css',
})
export class StatusCard {
  readonly status = input.required<ServiceStatus>();
  protected readonly up = computed(() => uptime(this.status().uptimeSeconds));
  protected readonly host = computed(() => hostOf(this.status().siteUrl ?? ''));
  protected readonly stateLabel = computed(() =>
    this.status().state === 'waiting'
      ? 'Waiting · 1 queued'
      : this.status().state === 'running'
        ? 'Running'
        : 'Idle',
  );
}
