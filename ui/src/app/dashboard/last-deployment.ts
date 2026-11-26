import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { attempts, clock } from '../core/format';
import { Deployment } from '../core/models';
import { CommitLink } from '../shared/commit-link';
import { OutcomeBadge } from '../shared/outcome-badge';
import { RelativeTime } from '../shared/relative-time';

const HEADLINES = {
  deployed: 'Your last push is live',
  up_to_date: 'Nothing to publish',
  refused: 'The build stopped on purpose',
  failed: 'Your last push did not deploy',
} as const;

/** Answers "did my last push deploy?" in one block: a headline, one plain sentence and the cause. */
@Component({
  selector: 'mb-last-deployment',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommitLink, OutcomeBadge, RelativeTime],
  templateUrl: './last-deployment.html',
  styleUrl: './last-deployment.css',
})
export class LastDeployment {
  readonly deployment = input.required<Deployment>();
  /** The newest earlier run that deployed, so a failure can say what the site still shows. */
  readonly previous = input<Deployment | null>(null);
  readonly sourceRepo = input.required<string>();
  readonly targetRepo = input.required<string>();
  readonly maxAttempts = input.required<number>();
  readonly now = input.required<number>();

  protected readonly d = this.deployment;
  protected readonly headline = computed(() => HEADLINES[this.d().outcome]);
  protected readonly attemptText = computed(() => attempts(this.d().attempts, this.maxAttempts()));
  protected readonly previousTime = computed(() => {
    const p = this.previous();
    return p ? clock(p.finishedAt) : '';
  });
}
