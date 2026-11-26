---
title: Stakeholder-ready documents
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta, published]
---

# Stakeholder-ready documents

Documents written to be **read outside the team** — in a wiki, in a ticket, or
in a slide. These are finished artifacts, not working notes.

## Rules

1. **Write in English.** No exceptions, even when the team speaks
   another language internally.
2. **Standard Markdown only.** No `[[wikilinks]]`. No agent metadata in the
   body. A wiki renderer must handle every construct you use.
3. **Self-contained.** A reader who has never seen the repository must
   understand it. Expand every abbreviation on first use.
4. **No internal path, hostname, credential, or ticket key** unless the audience
   already has access to it.
5. **Filename:** `<area>-<document-type>.md`, for example
   `order-checkout-api-parity.md`.
6. **Regenerate, do not patch.** When the content changes, rewrite the document
   from the current `knowledge/` and `decisions/` files.

## Document types

| Type | Purpose |
|---|---|
| `-overview` | What a component is, for a non-expert reader |
| `-api-parity` | What a new implementation does and does not match |
| `-migration-plan` | What changes, in which order, with which risk |
| `-runbook` | How to operate a component in production |
| `-postmortem` | What failed, what we changed |

## Checklist before publishing

- [ ] Written in English.
- [ ] No wikilinks and no agent metadata.
- [ ] Every abbreviation expanded on first use.
- [ ] No credential, internal hostname, or private path.
- [ ] Tables render correctly in the target wiki.
- [ ] A date and an owner appear at the top.

Start from [`_template.md`](_template.md).
