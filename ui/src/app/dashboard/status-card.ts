import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { hostOf, uptime } from '../core/format';
import { ServiceStatus } from '../core/models';

/** The service: worker state, uptime, the two repositories and the published site. */
@Component({
  selector: 'mb-status-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="section" aria-labelledby="service-heading">
      <h2 id="service-heading" class="label">Service</h2>
      <dl>
        <div><dt class="label">State</dt>
          <dd class="state">
            <svg width="10" height="10" viewBox="0 0 10 10" aria-hidden="true">
              <circle cx="5" cy="5" r="4" [attr.fill]="status().state === 'idle' ? 'none' : 'currentColor'"
                      stroke="currentColor" stroke-width="2" />
            </svg>
            {{ stateLabel() }}
          </dd></div>
        <div><dt class="label">Uptime</dt><dd class="strong">{{ up() }}</dd></div>
        <div><dt class="label">Source</dt>
          <dd><svg width="12" height="12" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6"
                   role="img" aria-label="Private"><rect x="3" y="7" width="10" height="7" rx="1" />
                <path d="M5 7V5a3 3 0 0 1 6 0v2" /></svg>
            <a [href]="'https://github.com/' + status().sourceRepo">{{ status().sourceRepo }}</a></dd></div>
        <div><dt class="label">Target</dt>
          <dd><a [href]="'https://github.com/' + status().targetRepo">{{ status().targetRepo }}</a></dd></div>
        <div><dt class="label">Site</dt>
          <dd>@if (status().siteUrl; as site) { <a [href]="site">{{ host() }} ↗</a> } @else { — }</dd></div>
      </dl>
    </section>
  `,
  styles: `
    .section { margin-top: 32px; }
    dl { margin: 0; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr));
         border: 1px solid var(--color-line); border-radius: 4px; }
    dl > div { padding: 14px 16px; border-right: 1px solid var(--color-line); min-width: 0; }
    dl > div:last-child { border-right: 0; }
    dd { margin: 6px 0 0; font-size: 14px; display: flex; align-items: center; gap: 6px; min-width: 0;
         overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    dd a { overflow: hidden; text-overflow: ellipsis; }
    .state { font-size: 15px; font-weight: 700; text-transform: uppercase; gap: 8px; }
    .strong { font-size: 15px; font-weight: 600; }
    @media (max-width: 900px) {
      dl { grid-template-columns: minmax(0, 1fr); }
      dl > div { border-right: 0; border-bottom: 1px solid var(--color-line); display: flex;
                 justify-content: space-between; gap: 12px; align-items: baseline; }
      dl > div:last-child { border-bottom: 0; }
      dd { margin: 0; }
    }
  `,
})
export class StatusCard {
  readonly status = input.required<ServiceStatus>();
  protected readonly up = computed(() => uptime(this.status().uptimeSeconds));
  protected readonly host = computed(() => hostOf(this.status().siteUrl ?? ''));
  protected readonly stateLabel = computed(() =>
    this.status().state === 'waiting' ? 'Waiting · 1 queued' : this.status().state === 'running' ? 'Running' : 'Idle',
  );
}
