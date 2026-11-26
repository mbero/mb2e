import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { absolute, attempts } from '../core/format';
import { Deployment } from '../core/models';
import { CommitLink } from '../shared/commit-link';
import { OutcomeBadge } from '../shared/outcome-badge';
import { RelativeTime } from '../shared/relative-time';

/**
 * The recent deployments: a table from 900 px up, a stacked list below. A failed or refused run opens its
 * cause; the open rows are kept by finish time, so a refresh keeps them open.
 */
@Component({
  selector: 'mb-deployment-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommitLink, OutcomeBadge, RelativeTime],
  template: `
    <section class="section" aria-labelledby="recent-heading">
      <div class="heading">
        <h2 id="recent-heading" class="label">Recent deployments</h2>
        <span class="caption">{{ deployments().length }} shown · newest first · times in your time zone</span>
      </div>
      <div class="wide">
        <table>
          <thead><tr>
            <th class="toggle"><span class="visually-hidden">Details</span></th>
            <th>Finished</th><th>Outcome</th><th>Trigger</th><th>Attempts</th><th>Source</th><th>Target</th>
          </tr></thead>
          @for (d of deployments(); track d.finishedAt) {
            <tbody [class.failed]="d.outcome === 'failed'">
              <tr>
                <td class="toggle">
                  @if (d.cause) {
                    <button type="button" class="expand" [attr.aria-expanded]="isOpen(d)"
                            [attr.aria-controls]="'cause-' + $index"
                            [attr.aria-label]="(isOpen(d) ? 'Hide cause for ' : 'Show cause for ') + local(d)"
                            (click)="toggle(d)">
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor"
                           stroke-width="1.8" aria-hidden="true" [class.open]="isOpen(d)">
                        <path d="m4.5 2.5 3.5 3.5-3.5 3.5" /></svg>
                    </button>
                  }
                </td>
                <td><mb-relative-time [value]="d.finishedAt" [now]="now()" /></td>
                <td><mb-outcome-badge [outcome]="d.outcome" /></td>
                <td>{{ d.trigger }}</td>
                <td [class.retried]="d.attempts > 1">{{ attemptText(d) }}</td>
                <td><mb-commit-link [repo]="sourceRepo()" [sha]="d.sourceCommit" /></td>
                <td><mb-commit-link [repo]="targetRepo()" [sha]="d.targetCommit" emptyLabel="— no change" /></td>
              </tr>
              @if (isOpen(d)) {
                <tr [id]="'cause-' + $index" class="cause-row">
                  <td colspan="7"><div class="label">Cause</div><pre class="cause">{{ d.cause }}</pre></td>
                </tr>
              }
            </tbody>
          }
        </table>
      </div>
      <ul class="narrow">
        @for (d of deployments(); track d.finishedAt) {
          <li [class.failed]="d.outcome === 'failed'">
            <div class="row"><mb-outcome-badge [outcome]="d.outcome" />
              <mb-relative-time [value]="d.finishedAt" [now]="now()" /></div>
            <dl>
              <div><dt>Trigger</dt><dd>{{ d.trigger }}</dd></div>
              <div><dt>Attempts</dt><dd [class.retried]="d.attempts > 1">{{ attemptText(d) }}</dd></div>
              <div><dt>Source</dt><dd><mb-commit-link [repo]="sourceRepo()" [sha]="d.sourceCommit" /></dd></div>
              <div><dt>Target</dt><dd>
                <mb-commit-link [repo]="targetRepo()" [sha]="d.targetCommit" emptyLabel="— no change" /></dd></div>
            </dl>
            @if (d.cause) {
              <button type="button" class="show" [attr.aria-expanded]="isOpen(d)" (click)="toggle(d)">
                {{ isOpen(d) ? 'Hide cause' : 'Show cause' }}</button>
              @if (isOpen(d)) { <pre class="cause">{{ d.cause }}</pre> }
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .section { margin-top: 48px; }
    .heading { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; margin: 14px 0 12px; }
    .heading h2 { margin: 0; }
    .caption { font-size: 12px; color: var(--color-muted); }
    .wide { border: 1px solid var(--color-line); border-radius: 4px; overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th { background: var(--color-panel); border-bottom: 2px solid var(--color-text); font: 700 12px var(--font-mono);
         text-transform: uppercase; letter-spacing: .06em; text-align: left; padding: 10px 14px; white-space: nowrap; }
    td { padding: 12px 14px; border-bottom: 1px solid var(--color-line); vertical-align: top; font: 400 13px var(--font-mono); }
    th.toggle, td.toggle { width: 44px; padding-left: 10px; }
    tbody tr:first-child:hover td { background: var(--color-hover); }
    tbody.failed td:first-child { border-left: 3px solid var(--color-fg); }
    .retried { font-weight: 700; }
    .expand { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px;
              border: 1px solid var(--color-line); border-radius: 4px; background: var(--color-bg);
              color: var(--color-text); cursor: pointer; padding: 0; }
    .expand:hover { border-color: var(--color-fg); }
    .expand svg { transition: transform 120ms ease; }
    .expand svg.open { transform: rotate(90deg); }
    .cause-row td { background: var(--color-panel); padding: 4px 14px 16px 58px; }
    .cause-row .label { margin: 10px 0 6px; }
    .cause-row pre { margin: 0; }
    .narrow { display: none; list-style: none; margin: 0; padding: 0; }
    @media (max-width: 900px) {
      .wide { display: none; }
      .narrow { display: block; }
      .narrow li { border: 1px solid var(--color-line); border-radius: 4px; padding: 14px 16px; margin-bottom: 10px; }
      .narrow li.failed { border-left: 3px solid var(--color-fg); }
      .row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; font-size: 13px; }
      dl { margin: 12px 0 0; display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; font-size: 13px; }
      dt { font-size: 11px; letter-spacing: .06em; text-transform: uppercase; color: var(--color-muted); }
      dd { margin: 2px 0 0; }
      .show { margin-top: 12px; min-height: 44px; width: 100%; border: 1px solid var(--color-text); border-radius: 4px;
              background: var(--color-bg); font: 600 13px var(--font-mono); cursor: pointer; color: var(--color-text); }
    }
  `,
})
export class DeploymentTable {
  readonly deployments = input.required<Deployment[]>();
  readonly sourceRepo = input.required<string>();
  readonly targetRepo = input.required<string>();
  readonly maxAttempts = input.required<number>();
  readonly now = input.required<number>();

  private readonly open = signal<ReadonlySet<string>>(new Set());

  protected isOpen(d: Deployment): boolean {
    return d.cause !== null && this.open().has(d.finishedAt);
  }

  protected toggle(d: Deployment): void {
    const next = new Set(this.open());
    if (!next.delete(d.finishedAt)) {
      next.add(d.finishedAt);
    }
    this.open.set(next);
  }

  protected local(d: Deployment): string {
    return absolute(d.finishedAt);
  }

  protected attemptText(d: Deployment): string {
    return attempts(d.attempts, this.maxAttempts());
  }
}
