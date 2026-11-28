import { ChangeDetectionStrategy, Component } from '@angular/core';

/** A centred card with one action: the GitHub login of the server. */
@Component({
  selector: 'mb-sign-in-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sign-in-page.html',
  styleUrl: './sign-in-page.css',
})
export class SignInPage {}
