import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Sidebar } from './sidebar';

/**
 * The page frame: the sidebar beside the content from 900 px up; below, a top bar whose menu button opens
 * the sidebar as a drawer. Escape or the scrim closes it, and the focus goes back to the button.
 */
@Component({
  selector: 'mb-app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Sidebar],
  host: { '(document:keydown.escape)': 'close()' },
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
})
export class AppShell {
  readonly login = input<string | null>(null);
  readonly siteUrl = input<string | null>(null);
  readonly sourceRepo = input<string | null>(null);
  readonly targetRepo = input<string | null>(null);
  readonly signOut = output<void>();

  protected readonly open = signal(false);
  private readonly menu = viewChild.required<ElementRef<HTMLButtonElement>>('menu');
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  protected close(): void {
    if (this.open()) {
      this.open.set(false);
      this.menu().nativeElement.focus();
    }
  }

  protected focusContent(event: Event): void {
    event.preventDefault();
    this.content().nativeElement.focus();
  }
}
