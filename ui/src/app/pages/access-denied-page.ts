import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The GitHub account is not the owner's; the page names it and offers to sign in with another one. */
@Component({
  selector: 'mb-access-denied-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="page">
      <div class="card">
        <div class="card-head"><img src="mb2e-logo.jpg" alt="mb2e — Markdown Based Blog Engine" width="220"></div>
        <div class="card-body">
          <div class="label">Deployment dashboard</div>
          <h1>No access</h1>
          <p class="lead">
            @if (login()) { You are signed in as <strong>{{ login() }}</strong>. } This dashboard is open only to the
            owner of this blog.</p>
          <p class="notice">Wrong account? Sign out, then sign in to GitHub with the owner's account.</p>
          <a class="action" href="./sign-in">Sign out</a>
        </div>
      </div>
    </main>
  `,
  styles: `
    :host { min-height: 100vh; display: flex; background: var(--color-panel); }
    .page { flex: 1 1 auto; display: flex; align-items: center; justify-content: center; padding: 48px 20px; }
    .card { width: 100%; max-width: 440px; background: var(--color-bg); border: 1px solid var(--color-line);
            border-radius: 5px; overflow: hidden; }
    .card-head { background: var(--color-brand-bg); padding: 22px 24px 18px; }
    .card-head img { display: block; height: auto; }
    .card-body { padding: 28px 36px 36px; }
    .card-body .label { color: var(--color-text); }
    h1 { margin: 10px 0 0; font-size: 32px; line-height: 1.12; font-weight: 700; letter-spacing: -.02em; text-transform: uppercase; }
    .lead { margin: 14px 0 0; font: 400 16px/1.6 var(--font-sans); color: var(--color-lead); overflow-wrap: anywhere; }
    .notice { margin: 20px 0 0; padding: 12px 14px; border-left: 3px solid var(--color-line); background: var(--color-panel);
              font-size: 13px; color: var(--color-muted); }
    .action { display: inline-block; margin-top: 20px; font-weight: 600; }
  `,
})
export class AccessDeniedPage {
  /** Bound from the "login" query parameter of the server redirect. */
  readonly login = input<string | null>(null);
}
