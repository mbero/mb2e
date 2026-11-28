import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The GitHub account is not the owner's; the page names it and offers to sign in with another one. */
@Component({
  selector: 'mb-access-denied-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './access-denied-page.html',
  styleUrl: './access-denied-page.css',
})
export class AccessDeniedPage {
  /** Bound from the "login" query parameter of the server redirect. */
  readonly login = input<string | null>(null);
}
