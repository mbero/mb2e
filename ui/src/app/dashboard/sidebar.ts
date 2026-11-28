import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** The navy brand panel: logo, navigation, the pipeline, and the signed-in login with "Sign out". */
@Component({
  selector: 'mb-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  readonly login = input<string | null>(null);
  readonly siteUrl = input<string | null>(null);
  readonly sourceRepo = input<string | null>(null);
  readonly targetRepo = input<string | null>(null);
  readonly signOut = output<void>();
}
