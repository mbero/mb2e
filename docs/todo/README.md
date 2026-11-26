---
title: Actionable gap lists
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta, todo]
---

# Actionable gap lists

Lists of **what must be done**, produced by an analysis. A deployment gap
analysis, a migration checklist, a configuration parity list.

This is not a backlog and not a ticket system. It is the output of "I compared
two things and found these differences."

## Rules

1. **One goal per file.** `staging-deployment-gap-analysis.md`, not `todo.md`.
2. **Every item is a checkbox pointing at something concrete:** a file, a line,
   a configuration key, a value mismatch. Generic advice is not an item.
3. **Every item carries a severity.** Use the four levels below.
4. **Close an item in place.** Flip `- [ ]` to `- [x]` and append how it closed:
   a commit, a merge request, or a decision.
5. **End with "Not verifiable offline"** — what needs live access.
6. **Do not delete a closed list.** Set `status: superseded` and keep it.

## Severity

| Mark | Meaning |
|---|---|
| 🔴 | Blocks the deployment |
| 🟠 | Must be fixed before cutover |
| 🟡 | Parity or hardening, not blocking |
| ✅ | Verified, no gap found |

## Before you start deployment or migration work

Check this folder for an open list on the subject. Continue it instead of
re-running the analysis from zero.

Start from [`_template.md`](_template.md).
