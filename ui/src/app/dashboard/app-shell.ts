import { ChangeDetectionStrategy, Component, ElementRef, input, output, signal, viewChild } from '@angular/core';
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
  template: `
    <a class="skip" href="#content" (click)="focusContent($event)">Skip to content</a>
    <header class="topbar">
      <img src="mb2e-logo.jpg" alt="mb2e" width="96">
      <button #menu type="button" class="menu" aria-controls="sidebar" [attr.aria-expanded]="open()"
              [attr.aria-label]="open() ? 'Close menu' : 'Open menu'" (click)="open() ? close() : open.set(true)">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="square" aria-hidden="true"><path d="M3 6h18M3 12h18M3 18h18" /></svg>
      </button>
    </header>
    <div class="layout">
      <nav id="sidebar" aria-label="Main" class="sidebar" [class.is-open]="open()">
        <mb-sidebar [login]="login()" [siteUrl]="siteUrl()" [sourceRepo]="sourceRepo()" [targetRepo]="targetRepo()"
                    (signOut)="signOut.emit()" />
      </nav>
      @if (open()) { <div class="scrim" (click)="close()"></div> }
      <main #content id="content" tabindex="-1"><div class="inner"><ng-content /></div></main>
    </div>
  `,
  styles: `
    .skip { position: absolute; left: 12px; top: -48px; z-index: 100; padding: 8px 14px; background: var(--color-fg);
            color: var(--color-bg); font-size: 14px; text-decoration: none; }
    .skip:focus { top: 12px; color: var(--color-bg); }
    .layout { display: grid; grid-template-columns: var(--sidebar-w) minmax(0, 1fr); min-height: 100vh; }
    .sidebar { position: sticky; top: 0; height: 100vh; }
    main { min-width: 0; padding: 72px 40px 96px; }
    main:focus { outline: none; }
    .inner { max-width: var(--content-w-wide); margin: 0 auto; }
    .topbar, .scrim { display: none; }
    @media (max-width: 900px) {
      .layout { grid-template-columns: minmax(0, 1fr); }
      .topbar { position: sticky; top: 0; z-index: 20; display: flex; align-items: center; justify-content: space-between;
                height: 64px; padding: 0 12px 0 16px; background: var(--color-brand-bg); }
      .topbar img { display: block; height: auto; }
      .menu { display: flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0;
              border: 2px solid var(--color-brand-fg); background: transparent; color: var(--color-brand-fg); cursor: pointer; }
      .menu:focus-visible { outline-color: var(--color-brand-fg); }
      .sidebar { position: fixed; top: 0; left: 0; bottom: 0; z-index: 40; width: min(86vw, 320px); height: auto;
                 transform: translateX(-100%); visibility: hidden;
                 transition: transform 180ms ease, visibility 0s linear 180ms; }
      .sidebar.is-open { transform: translateX(0); visibility: visible; transition: transform 180ms ease; }
      .scrim { position: fixed; inset: 0; z-index: 30; display: block; background: rgba(0, 0, 0, .35); }
      main { padding: 32px 20px 64px; }
    }
  `,
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
