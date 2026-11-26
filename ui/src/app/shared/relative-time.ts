import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { absolute, relative } from '../core/format';

/** An instant as local date and time, with the relative time under it or after it. */
@Component({
  selector: 'mb-relative-time',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<time [attr.datetime]="value()">{{ local() }}</time
    >@if (inline()) {<span> · {{ ago() }}</span>} @else {<div class="ago">{{ ago() }}</div>}`,
  styles: `
    time { color: var(--color-text); white-space: nowrap; }
    .ago { color: var(--color-muted); font-size: 12px; margin-top: 2px; }
  `,
})
export class RelativeTime {
  readonly value = input.required<string>();
  readonly now = input.required<number>();
  readonly inline = input(false);
  protected readonly local = computed(() => absolute(this.value()));
  protected readonly ago = computed(() => relative(this.value(), this.now()));
}
