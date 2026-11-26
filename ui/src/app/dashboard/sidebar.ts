import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/** The navy brand panel: logo, navigation, the pipeline, and the signed-in login with "Sign out". */
@Component({
  selector: 'mb-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="brand">
      <img src="mb2e-logo.jpg" alt="mb2e — Markdown Based Blog Engine" width="220">
      <div class="tag"><span aria-hidden="true"></span>Deployment dashboard</div>
    </div>
    <ul class="nav">
      <li><a href="./" aria-current="page" class="item current">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
          <rect x="2" y="2" width="5" height="5" /><rect x="9" y="2" width="5" height="5" />
          <rect x="2" y="9" width="5" height="5" /><rect x="9" y="9" width="5" height="5" /></svg>
        Dashboard<span class="marker" aria-hidden="true"></span></a></li>
      @if (siteUrl(); as site) {
        <li><a [href]="site" class="item">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
            <path d="M9 3h4v4M13 3 7 9M11 9v4H3V5h4" /></svg>
          Live site<span class="visually-hidden"> (opens the blog)</span></a></li>
      }
    </ul>
    @if (sourceRepo() && targetRepo()) {
      <div class="pipeline">
        <div class="pipeline-title">Pipeline</div>
        {{ sourceRepo() }}<br><span aria-hidden="true">↓</span> build<br>{{ targetRepo() }}
      </div>
    }
    <div class="user">
      <div class="who">
        <span class="avatar" aria-hidden="true">{{ (login() || '?').charAt(0).toUpperCase() }}</span>
        <span class="login">{{ login() }}</span>
      </div>
      <button type="button" class="signout" (click)="signOut.emit()">Sign out</button>
    </div>
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: 100%; background: var(--color-brand-bg);
            color: var(--color-brand-fg); font-family: var(--font-mono); }
    :host :focus-visible { outline-color: var(--color-brand-fg); }
    .brand { padding: 20px 14px 18px; border-bottom: 1px solid var(--color-brand-line); }
    .brand img { display: block; width: 220px; height: auto; }
    .tag { margin: 6px 0 0 8px; display: flex; align-items: center; gap: 8px; font: 500 11px var(--font-mono);
           letter-spacing: .1em; text-transform: uppercase; color: var(--color-brand-muted); }
    .tag span { width: 6px; height: 6px; background: var(--color-brand-accent); }
    .nav { list-style: none; margin: 0; padding: 16px 10px; flex: 1 1 auto; display: flex; flex-direction: column; gap: 2px; }
    .item { position: relative; display: flex; align-items: center; gap: 10px; height: 38px; padding: 0 12px;
            border-radius: 5px; color: #d4d3cb; font-size: 14px; text-decoration: none; }
    .item:hover { background: var(--color-brand-hover); color: #fff; }
    .item.current { background: var(--color-brand-active); color: #fff; font-weight: 600; }
    .marker { margin-left: auto; width: 8px; height: 8px; background: var(--color-brand-accent); }
    .pipeline { margin: 0 10px 12px; padding: 12px; border: 1px solid var(--color-brand-line); border-radius: 5px;
                font-size: 12px; line-height: 1.7; color: var(--color-brand-muted); overflow-wrap: anywhere; }
    .pipeline-title { color: var(--color-brand-fg); font-weight: 600; text-transform: uppercase; letter-spacing: .06em; font-size: 11px; }
    .user { display: flex; align-items: center; justify-content: space-between; gap: 8px; height: 60px; padding: 0 14px;
            border-top: 1px solid var(--color-brand-line); }
    .who { display: flex; align-items: center; gap: 10px; min-width: 0; }
    .avatar { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 4px;
              background: var(--color-brand-active); font-size: 13px; font-weight: 700; }
    .login { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .signout { background: none; border: 0; padding: 4px; font: 400 13px var(--font-mono); color: var(--color-brand-muted);
               text-decoration: underline; text-underline-offset: 3px; cursor: pointer; }
    .signout:hover { color: #fff; }
  `,
})
export class Sidebar {
  readonly login = input<string | null>(null);
  readonly siteUrl = input<string | null>(null);
  readonly sourceRepo = input<string | null>(null);
  readonly targetRepo = input<string | null>(null);
  readonly signOut = output<void>();
}
