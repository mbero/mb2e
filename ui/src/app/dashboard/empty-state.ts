import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The service has started and has no deployment yet. */
@Component({
  selector: 'mb-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.css',
})
export class EmptyState {
  readonly sourceRepo = input.required<string>();
}
