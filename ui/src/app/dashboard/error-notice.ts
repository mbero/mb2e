import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The API cannot be reached, or the session has ended; the session notice leads back to the sign-in. */
@Component({
  selector: 'mb-error-notice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './error-notice.html',
  styleUrl: './error-notice.css',
})
export class ErrorNotice {
  readonly kind = input.required<'api' | 'session'>();
  readonly hasData = input(false);
}
