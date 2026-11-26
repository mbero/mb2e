import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Outcome } from '../core/models';

/** The outcome as text, glyph, fill and border weight; colour alone never tells two outcomes apart. */
@Component({
  selector: 'mb-outcome-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"badge " + outcome() + " " + size()' },
  template: `
    <svg [attr.width]="size() === 'lg' ? 12 : 11" [attr.height]="size() === 'lg' ? 12 : 11" viewBox="0 0 12 12"
         fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
      @switch (outcome()) {
        @case ('deployed') { <path d="m2 6.5 2.5 2.5L10 3.5" /> }
        @case ('up_to_date') { <path d="M2.5 4.5h7M2.5 7.5h7" /> }
        @case ('refused') { <path d="M3 6h6" /> }
        @case ('failed') { <path d="M6 2v5" /><circle cx="6" cy="9.5" r=".6" fill="currentColor" /> }
      }
    </svg>
    {{ label() }}
  `,
  styles: `
    :host {
      display: inline-flex; align-items: center; gap: 6px; height: 24px; padding: 0 9px; border-radius: 4px;
      font: 600 12px var(--font-mono); letter-spacing: .02em; white-space: nowrap;
    }
    :host(.lg) { height: 30px; font-size: 13px; text-transform: uppercase; }
    :host(.deployed) { background: var(--color-code-bg); color: var(--color-code-fg); border: 1px solid var(--color-code-bg); }
    :host(.up_to_date) { background: var(--color-bg); color: var(--color-muted); border: 1px solid var(--color-badge-line); font-weight: 500; }
    :host(.refused) { background: var(--color-bg); color: var(--color-text); border: 1px dashed var(--color-text); }
    :host(.failed) { background: var(--color-code-bg); color: var(--color-code-fg); border: 2px solid var(--color-fg); font-weight: 700; }
  `,
})
export class OutcomeBadge {
  readonly outcome = input.required<Outcome>();
  readonly size = input<'sm' | 'lg'>('sm');
  protected readonly label = computed(() => (this.outcome() === 'up_to_date' ? 'up to date' : this.outcome()));
}
