import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { absolute, relative } from '../core/format';

/** An instant as local date and time, with the relative time under it or after it. */
@Component({
  selector: 'mb-relative-time',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './relative-time.html',
  styleUrl: './relative-time.css',
})
export class RelativeTime {
  readonly value = input.required<string>();
  readonly now = input.required<number>();
  readonly inline = input(false);
  protected readonly local = computed(() => absolute(this.value()));
  protected readonly ago = computed(() => relative(this.value(), this.now()));
}
