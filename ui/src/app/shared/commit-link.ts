import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { commitUrl } from '../core/format';

/** A short commit id that links to GitHub; without an id it shows the empty label in muted text. */
@Component({
  selector: 'mb-commit-link',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (sha(); as id) {
      <a [href]="url()">{{ id.slice(0, 7) }}</a>
    } @else {
      <span class="empty">{{ emptyLabel() }}</span>
    }
  `,
  styles: `.empty { color: var(--color-muted); }`,
})
export class CommitLink {
  readonly repo = input.required<string>();
  readonly sha = input<string | null>(null);
  readonly emptyLabel = input('—');
  protected readonly url = computed(() => commitUrl(this.repo(), this.sha() ?? ''));
}
