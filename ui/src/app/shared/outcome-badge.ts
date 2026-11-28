import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Outcome } from '../core/models';

/** The outcome as text, glyph, fill and border weight; colour alone never tells two outcomes apart. */
@Component({
  selector: 'mb-outcome-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"badge " + outcome() + " " + size()' },
  templateUrl: './outcome-badge.html',
  styleUrl: './outcome-badge.css',
})
export class OutcomeBadge {
  readonly outcome = input.required<Outcome>();
  readonly size = input<'sm' | 'lg'>('sm');
  protected readonly label = computed(() =>
    this.outcome() === 'up_to_date' ? 'up to date' : this.outcome(),
  );
}
