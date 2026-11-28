---
title: Dashboard design handoff
type: knowledge
status: current
updated: 2026-11-26
sources:
  - docs/design/dashboard/mockups/Main.dc.html
  - docs/design/dashboard/site-styles.css
tags: [design, dashboard]
---

# mb2e dashboard — design handoff

Source of every screen, with sample data: `mockups/` (see `mockups/README.md`).

## 0. Owner decisions (2026-11-26)

| Question | Decision |
|---|---|
| How the UI learns the signed-in login | `GET /api/me` returns `{"login": "..."}` |
| Attempt limit | `/api/status` returns `maxAttempts`; the UI shows `n of {maxAttempts}` |
| Outcome badges | Monochrome; variant B (`--color-alert`) is not used |
| Shell colours | Navy brand shell as designed (`--color-brand-*`); the content area stays monochrome |
| Logo | The cropped JPEG `mockups/mb2e-logo.jpg` |

## 1. Design rationale

1. **One question, read top-down.** Status → last deployment → history. The answer to "did my last push deploy?" sits above the fold in a single block with a headline sentence, not just a badge.
2. **Same shell as a mb2e blog.** 280 px panel sidebar, white content, Fira Code UI, uppercase mono titles, 2 px black section rules. Branding is the **mb2e** logo (engine), not any single blog. The sidebar (and the narrow top bar, and the header of the sign-in card) takes the logo's palette — navy ground, warm off-white text, the blue square as the active-item marker — so the logo sits *in* the panel, not on it. The content area stays the blog's monochrome.
3. **The summary explains, the table records.** Summary uses Inter for one plain sentence ("The site still shows the deployment from 21:52"). The table stays mono and dense.
4. **Outcomes without colour.** Text + glyph + fill + border weight: `deployed` solid ✓, `up_to_date` light outline =, `refused` dashed –, `failed` solid with 2 px border ! plus a 3 px black left edge on the row.
5. **`up_to_date` is deliberately the quietest badge** — it is normal, not an event.
6. **Cause lives where the problem is.** Last failure: cause shown open in the summary. In the table: real `<button aria-expanded>` per failed/refused row reveals a dark `pre` block (reuses `--color-code-bg/fg`).
7. **Attempts > 1 is bold** ("2 of 3") so retried pushes stand out even when they ended as `deployed`.
8. **Quiet refresh.** "Updated 5 s ago · refreshes every 15 s" top-right, no spinner, no `aria-live` on the timer. Data is patched in place (track rows by `finishedAt`), so focus, expanded rows and scroll stay put.
9. **Errors keep context.** API down → keep last data, dimmed and labelled *Stale*. Session expired → inline notice with "Sign in with GitHub", auto-refresh stops.
10. **Accent is optional.** Monochrome is recommended; variant B adds a single `--color-alert` for `failed` only (see canvas, "Outcome badges").

## 2. Tokens

All tokens from `site-styles.css` are reused by name. Additions:

| Token | Value | Why |
|---|---|---|
| `--content-w-wide` | `1040px` | 700 px (`--content-w`) is too narrow for a 7-column table |
| `--color-badge-line` | `#c4c4c4` | outline of the `up_to_date` badge; `--color-line` (#e3e3e3) is below 3:1 non-text contrast on white |
| `--color-brand-bg` | `#23262d` | logo background; sidebar, top bar, sign-in card header |
| `--color-brand-fg` | `#e6e5dd` | logo lettering; sidebar text (12.6:1 on brand-bg) |
| `--color-brand-muted` | `#aaaeb7` | sidebar secondary text (6.8:1) |
| `--color-brand-line` | `#383b43` | sidebar dividers |
| `--color-brand-hover` / `--color-brand-active` | `#2e3139` / `#3a3e47` | sidebar item hover / current page |
| `--color-brand-accent` | `#4377dd` | logo's blue square; only as the active-item marker and the label dot — never text, never an outcome (3.5:1, non-text only) |
| `--color-alert` *(variant B only)* | `#b42318` | 6.5:1 on white; reserved for `failed` |

## 3. Angular component inventory

Standalone components, `OnPush`, signals. Data via one `DashboardStore` (polls both endpoints every 15 s with `timer(0, 15000)`, pauses when the tab is hidden).

| Component | Inputs | States / notes |
|---|---|---|
| `AppShellComponent` | `login: string` | desktop sidebar · narrow top bar + drawer (`aria-expanded` on menu button, scrim, Esc closes, focus returns to button) |
| `SidebarComponent` | `login`, `siteUrl`, `sourceRepo`, `targetRepo` | brand palette; logo, nav (Dashboard `aria-current="page"` + blue marker, Live site ↗), small "Pipeline" box (source ↓ build → target), footer with login + Sign out; focus ring uses `--color-brand-fg` |
| `RefreshIndicatorComponent` | `lastUpdated: Date`, `intervalSec: number` | `fresh` · `stale` (after a failed poll) |
| `StatusCardComponent` | `status: ServiceStatus` | `idle` ○ · `running` ◉ · `waiting` ◉ + "1 queued" |
| `LastDeploymentComponent` | `deployment: Deployment`, `previousDeployed?: Deployment` | one variant per outcome (headline + sentence); `failed` / `refused` show cause open |
| `DeploymentTableComponent` | `deployments: Deployment[]` | desktop table ≥ 900 px; `track d.finishedAt`; keeps expanded ids in a `Set` across refreshes |
| `DeploymentRowComponent` | `deployment`, `expanded: boolean`; output `toggle` | expandable only if `cause`; failed row gets left edge |
| `DeploymentCardComponent` | same as row | stacked list item < 900 px; "Show cause" button ≥ 44 px |
| `OutcomeBadgeComponent` | `outcome: Outcome`, `size: 'sm' \| 'lg'` | 4 outcomes; glyph `aria-hidden`, text is the label |
| `CommitLinkComponent` | `repo: string`, `sha: string \| null`, `emptyLabel` | link to `https://github.com/{repo}/commit/{sha}`; empty → muted text |
| `RelativeTimeComponent` | `value: string (ISO)` | `<time datetime>`; absolute local time + relative ("8 min ago") |
| `EmptyStateComponent` | `sourceRepo` | — |
| `LoadingSkeletonComponent` | — | static blocks, `aria-busy="true"` on main |
| `ErrorNoticeComponent` | `kind: 'api' \| 'session'`, `lastUpdated?` | `role="alert"`; `session` renders the sign-in button |
| `SignInPageComponent` | `returnUrl?` | — |
| `AccessDeniedPageComponent` | `login` | — |

Routes: `/sign-in`, `/denied`, `/` (guarded). A 401 from either endpoint → `ErrorNotice kind="session"`; 403 → `/denied`.

## 4. Microcopy

**Global**
- Skip link: `Skip to content`
- Sidebar: `Dashboard`, `Live site`, `Sign out`
- Menu button (aria-label): `Open menu` / `Close menu`
- Refresh: `Updated {n} s ago · refreshes every 15 s` · stale: `Last updated {n} min ago`

**Sign-in**
- Label: `Deployment dashboard` · Title: `Sign in`
- Body: `See whether your last push to the notes repository reached your blog — and if not, why.`
- Button: `Sign in with GitHub`
- Note: `Only the owner's GitHub account has access. The dashboard is read-only.`

**Access denied**
- Title: `No access`
- Body: `You are signed in as {login}. This dashboard is open only to the owner of this blog.`
- Notice: `Wrong account? Sign out, then sign in to GitHub with the owner's account.`
- Action: `Sign out`

**Dashboard**
- Title: `Deployments` · sections: `Service`, `Last deployment`, `Recent deployments`
- Status labels: `State`, `Uptime`, `Source`, `Target`, `Site` · states: `Idle`, `Running`, `Waiting · 1 queued`
- Table header: `Finished`, `Outcome`, `Trigger`, `Attempts`, `Source`, `Target` · caption: `{n} shown · newest first · times in your time zone`
- Badges: `deployed`, `up to date`, `refused`, `failed`
- Empty target: `— no change` · empty source: `—`
- Attempts: `1` · `{n} of 3`
- Expand button (aria-label): `Show cause for {time}` / `Hide cause for {time}` · narrow: `Show cause` / `Hide cause`
- Cause label: `Cause`

**Last deployment headlines + sentences**
- deployed — `Your last push is live` / `Commit {src} was built and pushed to {targetRepo} as {tgt}. GitHub Pages usually publishes within a minute.`
- up_to_date — `Nothing to publish` / `Commit {src} built the same site as before, so there was nothing to push. This is normal.`
- refused — `The build stopped on purpose` / `Commit {src} was not published. The site stays as it is.` + cause
- failed — `Your last push did not deploy` / `Commit {src} was built, but the push to {targetRepo} failed. The site still shows the deployment from {time} ({tgt}). The next push or poll will try again.` + cause
- Meta line: `Finished {date, time} · {relative}` · `Trigger {trigger}` · `Attempts {n}`

**Empty** — `No deployments yet` / `The service started and is building your notes for the first time. Your first deployment shows up here in a moment — or push to {sourceRepo} to start one.`

**Loading** — `Loading deployments…`

**Errors**
- API: `Can't reach the service` / `The last successful update was {relative}. The data below may be out of date. Trying again in 15 s.` · tag: `Stale · as of {time}`
- API, no data yet: `Can't reach the service` / `Trying again in 15 s.`
- Session: `Your session has expired` / `Sign in again to keep watching your deployments. You'll come back to this page.` · button `Sign in with GitHub`

## 5. Open questions / assumptions

1. **"Site still shows…" needs the previous `deployed` row.** Assumed the UI finds it in the 50 rows; if none, the sentence drops that clause.
2. **Max attempts = 3** is hard-coded in "n of 3". Expose it in `/api/status` if it can change.
3. **`cause` is plain text**, rendered with `white-space: pre-wrap`, never as HTML.
4. **Commit links**: source repo is private — the link works only for the owner (fine, single user).
5. **Logo**: the JPEG's background matches `--color-brand-bg`, so it blends into the sidebar. A transparent SVG would remove any edge from JPEG noise and scale better.
6. **Live site link** points to `siteUrl` from `/api/status`, so the shell works for any blog built with mb2e.
7. **Hidden tab**: polling pauses on `visibilitychange` and refreshes immediately on return.
