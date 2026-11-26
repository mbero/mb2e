---
title: Dashboard mockups
type: knowledge
status: current
updated: 2026-11-26
sources:
  - docs/design/dashboard/mockups/Main.dc.html
tags: [design, dashboard]
---

# mb2e dashboard — mockups (source for implementation)

Design sources exported from the design canvas.

Read together with `../CLAUDE_DESIGN_BRIEF.md` (requirements) and `../DESIGN_HANDOFF.md`
(rationale, tokens, Angular component inventory, microcopy, open questions).

## Files

| File | Screen | Brief section |
|---|---|---|
| `Main.dc.html` | Dashboard, desktop (hi-fi, expandable rows) | Screen 3 |
| `Mobile.dc.html` | Dashboard, narrow < 900 px (stacked list) | Screen 7 |
| `SignIn.dc.html` | Sign-in | Screen 1 |
| `AccessDenied.dc.html` | Access denied | Screen 2 |
| `States.dc.html` | Empty, loading, API down, session expired | Screens 4–6 |
| `FailedVariant.dc.html` | Outcome badges: monochrome (recommended) vs. `--color-alert` accent | Visual language |
| `Wireframes.dc.html` | Low-fi wireframes of all screens | Deliverable 2 |
| `canvas.json` | Canvas layout index (frame sizes) — not needed for implementation |
| `mb2e-logo.jpg` | Cropped logo used in the sidebar, top bar and sign-in card |

## How to read a `.dc.html` file

They are design-tool files, not runnable pages, and do not open correctly in a browser on their own.
Treat them as the visual spec:

- Markup inside `<x-dc>` is the layout. All styles are inline or in the `<helmet><style>` block —
  copy the values, then move them into component CSS using the tokens from `../site-styles.css`
  plus the `--color-brand-*` additions listed in `DESIGN_HANDOFF.md`.
- `{{name}}` is a template hole; `<sc-for list="{{rows}}" as="r">` is a loop (→ `@for`);
  `<sc-if value="{{x}}">` is a condition (→ `@if`).
- The `<script type="text/x-dc">` block at the bottom holds the **sample data** (12 deployments)
  and the expand/collapse logic. Reuse the sample data as a mock for `/api/deployments`.
- `support.js` and `data-props` belong to the design tool — ignore them.

## Implementation notes

- Angular, plain CSS, no UI library (see brief "Constraints").
- Hard-coded hex values in the mockups map 1:1 to tokens: `#111`→`--color-text`,
  `#5c5c5c`→`--color-muted`, `#f6f6f6`→`--color-panel`, `#e3e3e3`→`--color-line`,
  `#c4c4c4`→`--color-badge-line`, `#23262d`→`--color-brand-bg`, `#e6e5dd`→`--color-brand-fg`,
  `#aaaeb7`→`--color-brand-muted`, `#383b43`→`--color-brand-line`, `#3a3e47`→`--color-brand-active`,
  `#4377dd`→`--color-brand-accent`.
- Sample repos (`mbero/notes`, `mbero/mbero.github.io`) are placeholders; real values come from `/api/status`.
