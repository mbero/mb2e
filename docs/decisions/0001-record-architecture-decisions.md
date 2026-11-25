---
title: Record architecture decisions
type: decision
status: current
updated: 2026-11-06
sources:
  - docs/decisions/README.md
tags: [meta, process]
---

# 0001 — Record architecture decisions

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-06 |
| Deciders | project team |
| Supersedes | none |
| Superseded by | none |

## Context

A project loses the reasons behind its choices faster than it loses the choices
themselves. The code shows **what** we built. It never shows **why** we rejected
the other option.

An AI agent has the same problem, and worse. It reads the current code and
proposes a change that an earlier decision already ruled out. Without a record,
the team relitigates the same choice every few months.

## Decision

We record every architecture decision as a numbered Markdown file in
`docs/decisions/`, following the format in `_template.md`.

We write the record when the decision is made, not later.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| A wiki page per decision | It drifts from the code and nobody reviews it in a pull request. |
| Commit messages only | They are not searchable by topic and they never record a rejected option. |
| No record | It is the current state, and it is the reason for this ADR. |

## Consequences

**Positive:**

- A new person, or an agent, can read the reasons in one folder.
- A rejected option stays rejected, with a stated reason.
- The decision travels with the code, through the same review process.

**Negative — the cost we accept:**

- Every meaningful decision costs about fifteen minutes of writing.
- An abandoned ADR folder is misleading. We must keep it current or delete it.

**Follow-up work:**

- Add an ADR check to the code review checklist in `docs/context/conventions.md`.

## Revisit when

The team stops writing ADRs for two consecutive months. At that point either
fix the process or remove the folder. A half-maintained decision log is worse
than none.
