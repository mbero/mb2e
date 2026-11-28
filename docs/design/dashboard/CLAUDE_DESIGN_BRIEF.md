---
title: Dashboard design brief
type: knowledge
status: current
updated: 2026-11-26
sources:
  - docs/design/dashboard/site-styles.css
tags: [design, dashboard]
---

# Design brief: mb2e dashboard UI/UX

> Paste this whole file into the Claude app as the first message.
> Attach two files from this folder: `site-styles.css` and `site-logo.jpeg`.

---

## Your role

You are a senior UI/UX designer. You design a small operator dashboard for **mb2e**.
The dashboard must look like part of the same product family as the blog
**https://mbero.github.io**. Reuse its visual language. Do not invent a new brand.

Work in this order:

1. Read this brief and the attached `site-styles.css`.
2. Ask me at most 3 questions, only if a decision blocks you.
3. Give a short design rationale (max 10 bullet points).
4. Deliver the screens listed in "Deliverables".

Write all UI text in English.

---

## The product

**mb2e** turns a folder of Markdown notes into a static blog. In server mode it runs
as one container. A push to the notes repository triggers a GitHub webhook. The service
builds the site and pushes it to the GitHub Pages repository `<owner>/<owner>.github.io`.

The dashboard answers one question: **"Did my last push deploy, and if not, why?"**

- One user: the owner. They sign in with GitHub.
- Read-only. No buttons that change the service in this version.
- The page refreshes the data every 15 seconds.
- Metrics, logs and alerts are out of scope (a separate Grafana setup covers them).

---

## Data the UI shows

### `GET /api/status`

| Field | Example | Notes |
|---|---|---|
| `state` | `idle` / `running` / `waiting` | `waiting` = one run is queued behind the current one |
| `uptime` | `3d 4h 12m` | since the last service start |
| `sourceRepo` | `mbero/notes` | private repository |
| `targetRepo` | `mbero/mbero.github.io` | public repository |
| `siteUrl` | `https://mbero.github.io` | link |

### `GET /api/deployments?limit=50` (newest first)

| Field | Example | Notes |
|---|---|---|
| `finishedAt` | `2026-11-25T21:14:03Z` | UI shows it in the browser time zone, plus a relative time ("3 min ago") |
| `outcome` | `deployed` / `up_to_date` / `refused` / `failed` | see meanings below |
| `trigger` | `push` / `startup` / `poll` | |
| `attempts` | `1`–`3` | more than 1 means the push was rejected and retried |
| `sourceCommit` | `a1b2c3d` | short id, links to the commit; may be empty |
| `targetCommit` | `9f8e7d6` | short id, links to the commit; empty when nothing changed |
| `cause` | `git push rejected 3 times` | only for `failed` and `refused`; may be several lines |

Outcome meanings:

- **deployed**: the site changed and the service pushed it.
- **up_to_date**: the build gave the same site; nothing to push. This is normal, not an error.
- **refused**: the build stopped on purpose (for example, no note carries the publish tag). The site stays as it is.
- **failed**: a technical error (expired token, network, rejected push). The next trigger tries again.

Use realistic sample data: about 12 rows with a mix of all four outcomes and all three triggers.

---

## Screens and states

1. **Sign-in page**: logo, one sentence about the dashboard, one "Sign in with GitHub" button.
2. **Access denied page**: the GitHub account is not the allowed one. Short message, a "Sign out" link. No technical details.
3. **Dashboard** (main screen):
   - a status card: state, uptime, source repo, target repo, link to the site;
   - a "last deployment" summary that answers the main question at a glance;
   - a table of recent deployments; a `failed` or `refused` row expands to show `cause`;
   - the signed-in GitHub login and a "Sign out" action;
   - a quiet "updated 5 s ago" indicator for the auto-refresh.
4. **Empty state**: the service started, no deployment yet.
5. **Loading state** for the first load.
6. **Error states**: the API does not answer; the session expired (lead the user back to sign-in).
7. **Narrow screen** (below 900 px): same content, table becomes a stacked list.

---

## Visual language (from mbero.github.io)

Follow the attached `site-styles.css`. Reuse its CSS custom properties by name. The key rules:

**Colour: strictly monochrome.**

| Token | Value | Use |
|---|---|---|
| `--color-bg` | `#ffffff` | page background |
| `--color-fg` / `--color-border` | `#000000` | strong text, strong borders, focus ring |
| `--color-text` | `#111111` | main text |
| `--color-muted` | `#5c5c5c` | metadata, secondary text |
| `--color-panel` | `#f6f6f6` | sidebar, table header, notices |
| `--color-line` | `#e3e3e3` | thin dividers |
| `--color-hover` / `--color-active` | `#ebebeb` / `#e2e2e2` | row hover, selected item |
| `--color-code-bg` / `--color-code-fg` | `#111111` / `#f2f2f2` | dark code block, usable for an inverted badge |

**Typography.**

- `Fira Code` (`--font-mono`) for UI, headings, labels, metadata, commit ids, numbers.
- `Inter` (`--font-sans`) for longer sentences only.
- Headings and labels: uppercase, bold, `letter-spacing: -0.02em` (titles) or `0.06–0.08em` (small labels).
- Page title like `.article__title`: 44 px, uppercase, bold (32 px on narrow screens).

**Shapes and lines.**

- Radius 4–5 px. No shadows. No gradients.
- Section separators: a 2 px black top border (like `.article__meta` and `.pager`).
- Thin 1 px `--color-line` dividers inside components.
- Tables like `.prose table`: panel-grey header with uppercase mono labels and a 2 px black bottom border.
- Notices like `.prose .notice`: grey panel with a 3 px left border.

**Layout.**

- Same shell as the blog: a 280 px left sidebar on `--color-panel` with the logo at the top (`mix-blend-mode: multiply`), and a content area on white.
- The sidebar can hold: the logo, "Dashboard", a link to the live site, and the user block with "Sign out" in the footer area (like `.sidebar__footer`).
- Below 900 px: a top bar with a small logo and a square menu button with a 2 px black border (like `.topbar__menu`).

**Outcome badges without colour.** The blog uses no accent colour. Keep it that way. Tell outcomes apart by text, weight, fill and a simple glyph, never by colour alone:

- `deployed`: inverted badge (black fill, white text) with a check glyph;
- `up_to_date`: outlined badge, muted text, an "=" glyph;
- `refused`: outlined badge with a dashed border, a "–" glyph;
- `failed`: inverted badge with a heavy 2 px border and a "!" glyph; the row also gets a 3 px black left border.

You may propose one restrained accent for `failed` only. Show it as an optional variant, next to the monochrome version, and explain the trade-off.

**Accessibility.**

- WCAG 2.2 AA contrast. Keyboard access for every action. A visible `:focus-visible` ring (2 px black, 2 px offset).
- A "Skip to content" link like `.skip-link`.
- Expandable rows use a real button with `aria-expanded`.
- Respect `prefers-reduced-motion`. No animation is needed apart from the expand/collapse.
- The auto-refresh must not move the keyboard focus or steal the scroll position.

---

## Constraints for the implementation

The developer builds the design in **Angular** with **plain CSS** (no UI library, no Tailwind).

- Use only the tokens above. If a new token is necessary, name it in the same `--color-*` style and justify it.
- Use simple SVG icons with `stroke="currentColor"`, as in the blog.
- No charts in this version.
- No dark mode in this version (the blog has none).

---

## Deliverables

1. **Design rationale**: max 10 bullet points.
2. **Low-fidelity wireframes** of all screens in "Screens and states".
3. **High-fidelity mockup** of the dashboard, the sign-in page and the narrow screen, as one self-contained HTML file with inline CSS that uses the tokens from `site-styles.css`. Use the sample data.
4. **Component inventory** for Angular: each component name, its inputs, and its states (for example `StatusCard`, `DeploymentTable`, `DeploymentRow`, `OutcomeBadge`, `EmptyState`, `ErrorNotice`, `SignInPage`, `AccessDeniedPage`).
5. **Microcopy list**: every label, message and empty/error text, in English.
6. **Open questions**: anything you assumed.

---

## Out of scope

- A "Deploy now" button.
- A settings view.
- Editing notes or settings.
- More than one user or more than one source/target pair.
- Metrics, charts, log viewer.
