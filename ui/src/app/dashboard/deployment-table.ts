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
  templateUrl: './deployment-table.html',
  styleUrl: './deployment-table.css',
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
