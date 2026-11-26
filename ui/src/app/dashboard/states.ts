import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The service has started and has no deployment yet. */
@Component({
  selector: 'mb-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="section notice" aria-labelledby="empty-heading">
      <h2 id="empty-heading">No deployments yet</h2>
      <p>The service started and is building your notes for the first time. Your first deployment shows up here in
        a moment — or push to <strong>{{ sourceRepo() }}</strong> to start one.</p>
    </section>
  `,
  styles: `
    .notice { padding-top: 14px; }
    h2 { margin: 0; font-size: 20px; font-weight: 700; text-transform: uppercase; letter-spacing: -.02em; }
    p { margin: 10px 0 0; font: 400 16px/1.6 var(--font-sans); color: var(--color-lead); max-width: 640px; }
  `,
})
export class EmptyState {
  readonly sourceRepo = input.required<string>();
}

/** Static blocks while the first answer is on its way. */
@Component({
  selector: 'mb-loading-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="visually-hidden">Loading deployments…</p>
    <div class="block short" aria-hidden="true"></div>
    <div class="block tall" aria-hidden="true"></div>
    <div class="block" aria-hidden="true"></div>
  `,
  styles: `
    .block { margin-top: 32px; height: 88px; background: var(--color-panel); border-radius: 4px; }
    .short { height: 72px; }
    .tall { height: 180px; }
  `,
})
export class LoadingSkeleton {}

/** The API cannot be reached, or the session has ended; the session notice leads back to the sign-in. */
@Component({
  selector: 'mb-error-notice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div role="alert" class="notice">
      @if (kind() === 'session') {
        <p class="title">Your session has expired</p>
        <p>Sign in again to keep watching your deployments. You'll come back to this page.</p>
        <a class="button" href="/oauth2/authorization/github">Sign in with GitHub</a>
      } @else {
        <p class="title">Can't reach the service</p>
        <p>@if (hasData()) { The data below may be out of date. } Trying again in 15 s.</p>
      }
    </div>
  `,
  styles: `
    .notice { margin-top: 32px; padding: 14px 16px; border-left: 3px solid var(--color-fg); background: var(--color-panel); }
    .title { margin: 0; font-weight: 700; text-transform: uppercase; font-size: 14px; }
    p { margin: 6px 0 0; font: 400 15px/1.6 var(--font-sans); color: var(--color-lead); }
    .button { display: inline-flex; margin-top: 12px; align-items: center; height: 40px; padding: 0 16px; border-radius: 4px;
              background: var(--color-brand-bg); color: #fff; font-weight: 600; font-size: 14px; text-decoration: none; }
    .button:hover { background: var(--color-brand-active); color: #fff; }
  `,
})
export class ErrorNotice {
  readonly kind = input.required<'api' | 'session'>();
  readonly hasData = input(false);
}
