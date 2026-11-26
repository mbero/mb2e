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
  template: `
    <section class="section" aria-labelledby="last-heading">
      <h2 id="last-heading" class="label">Last deployment</h2>
      <div class="box" [class.alarm]="d().outcome === 'failed'">
        <div class="head">
          <mb-outcome-badge [outcome]="d().outcome" size="lg" />
          <p class="headline">{{ headline() }}</p>
        </div>
        <p class="sentence">
          @switch (d().outcome) {
            @case ('deployed') {
              Commit <mb-commit-link [repo]="sourceRepo()" [sha]="d().sourceCommit" /> was built and pushed to
              <strong>{{ targetRepo() }}</strong> as <mb-commit-link [repo]="targetRepo()" [sha]="d().targetCommit" />.
              GitHub Pages usually publishes within a minute.
            }
            @case ('up_to_date') {
              Commit <mb-commit-link [repo]="sourceRepo()" [sha]="d().sourceCommit" /> built the same site as before,
              so there was nothing to push. This is normal.
            }
            @case ('refused') {
              Commit <mb-commit-link [repo]="sourceRepo()" [sha]="d().sourceCommit" /> was not published.
              The site stays as it is.
            }
            @case ('failed') {
              Commit <mb-commit-link [repo]="sourceRepo()" [sha]="d().sourceCommit" /> was built, but the push to
              <strong>{{ targetRepo() }}</strong> failed.
              @if (previous(); as p) {
                The site still shows the deployment from {{ previousTime() }}
                (<mb-commit-link [repo]="targetRepo()" [sha]="p.targetCommit" />).
              }
              The next push or poll will try again.
            }
          }
        </p>
        @if (d().cause; as cause) {
          <div class="label cause-label">Cause</div>
          <pre class="cause">{{ cause }}</pre>
        }
        <p class="meta">
          <span>Finished <mb-relative-time [value]="d().finishedAt" [now]="now()" [inline]="true" /></span>
          <span>Trigger <b>{{ d().trigger }}</b></span>
          <span>Attempts <b>{{ attemptText() }}</b></span>
        </p>
      </div>
    </section>
  `,
  styles: `
    .box { border: 1px solid var(--color-line); border-radius: 5px; padding: 24px 26px; }
    .box.alarm { border: 2px solid var(--color-fg); }
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; }
    .headline { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -.02em; text-transform: uppercase; }
    .sentence { margin: 14px 0 0; font: 400 17px/1.6 var(--font-sans); color: var(--color-lead); max-width: 720px; }
    .cause-label { margin-top: 18px; }
    .meta { margin: 16px 0 0; display: flex; flex-wrap: wrap; gap: 6px 22px; font-size: 13px; color: var(--color-muted); }
    .meta b { color: var(--color-text); font-weight: 600; }
    @media (max-width: 900px) { .box { padding: 18px; } .headline { font-size: 20px; } }
  `,
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
