import { ChangeDetectionStrategy, Component } from '@angular/core';

/** A centred card with one action: the GitHub login of the server. */
@Component({
  selector: 'mb-sign-in-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="page">
      <div class="card">
        <div class="card-head"><img src="mb2e-logo.jpg" alt="mb2e — Markdown Based Blog Engine" width="220"></div>
        <div class="card-body">
          <div class="label">Deployment dashboard</div>
          <h1>Sign in</h1>
          <p class="lead">See whether your last push to the notes repository reached your blog — and if not, why.</p>
          <a class="github" href="/oauth2/authorization/github">
            <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <path d="M8 0a8 8 0 0 0-2.53 15.59c.4.07.55-.17.55-.38v-1.33c-2.23.48-2.7-1.07-2.7-1.07-.36-.92-.89-1.17-.89-1.17-.73-.5.06-.49.06-.49.8.06 1.23.83 1.23.83.72 1.22 1.88.87 2.33.66.07-.52.28-.87.5-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.28.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48v2.2c0 .21.15.46.55.38A8 8 0 0 0 8 0Z" />
            </svg>
            Sign in with GitHub</a>
          <p class="note">Only the owner's GitHub account has access. The dashboard is read-only.</p>
        </div>
      </div>
    </main>
    <footer><span class="brand">mb2e</span><span>Markdown Based Blog Engine</span></footer>
  `,
  styles: `
    :host { min-height: 100vh; display: flex; flex-direction: column; background: var(--color-panel); }
    .page { flex: 1 1 auto; display: flex; align-items: center; justify-content: center; padding: 48px 20px; }
    .card { width: 100%; max-width: 440px; background: var(--color-bg); border: 1px solid var(--color-line);
            border-radius: 5px; overflow: hidden; }
    .card-head { background: var(--color-brand-bg); padding: 22px 24px 18px; }
    .card-head img { display: block; height: auto; }
    .card-body { padding: 28px 36px 36px; }
    .card-body .label { color: var(--color-text); }
    h1 { margin: 10px 0 0; font-size: 32px; line-height: 1.12; font-weight: 700; letter-spacing: -.02em; text-transform: uppercase; }
    .lead { margin: 14px 0 0; font: 400 16px/1.6 var(--font-sans); color: var(--color-lead); }
    .github { margin-top: 28px; display: flex; align-items: center; justify-content: center; gap: 10px; height: 48px;
              border-radius: 4px; background: var(--color-brand-bg); color: #fff; font-size: 15px; font-weight: 600;
              text-decoration: none; }
    .github:hover { background: var(--color-brand-active); color: #fff; }
    .note { margin: 16px 0 0; font-size: 12px; line-height: 1.6; color: var(--color-muted); }
    footer { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px 24px; padding: 20px 40px;
             border-top: 1px solid var(--color-line-soft); font-size: 13px; color: var(--color-muted); }
    .brand { color: var(--color-text); font-weight: 600; }
    @media (max-width: 900px) { .card-body { padding: 24px; } footer { padding: 20px; } }
  `,
})
export class SignInPage {}
