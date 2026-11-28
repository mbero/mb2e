---
title: Architecture Decision Records
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta, decisions]
---

# Architecture Decision Records

An ADR records **one choice, the alternatives, and the reason**. It answers the
question a new person asks six months later: *why is it built this way?*

## Rules

1. **One decision per file.** Never bundle two choices.
2. **Numbered and immutable.** `NNNN-kebab-slug.md`, numbers never reused.
   Once a record is `accepted`, you do not edit it.
3. **To change a decision, write a new ADR** that supersedes the old one. Then
   set the old record to `status: superseded` and link forward to the new one.
   That single edit is the only edit an accepted ADR ever receives.
4. **Record the alternatives you rejected.** An ADR without rejected options is
   a note, not a decision.
5. **Record the consequences, including the bad ones.** A decision with no cost
   was not a decision.
6. **Write it when the choice is made**, not at the end of the project.

## What deserves an ADR

- A choice between two libraries, patterns, or protocols.
- A boundary: what this service owns and what it does not.
- A deliberate exception to a convention.
- A choice to accept technical debt, and the trigger to revisit it.

**Not an ADR:** a bug fix, a rename, a dependency version bump, a task list.

## Status values

| Status | Meaning |
|---|---|
| `proposed` | Written, not yet agreed |
| `accepted` | In force |
| `superseded` | Replaced. The body names the replacement. |
| `deprecated` | No longer applies, with no replacement |

## Index

| # | Title | Status | Date |
|---|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | accepted | 2026-11-06 |
| [0002](0002-server-mode-in-the-same-artifact.md) | Run the server mode from the same jar and separate the modes with a property | accepted | 2026-11-25 |
| [0003](0003-deploy-by-git-clones-and-the-git-program.md) | Deploy with two shallow clones and the git program | accepted | 2026-11-25 |
| [0004](0004-signed-webhook-and-one-slot-queue.md) | Accept a signed webhook and run one deployment at a time | accepted | 2026-11-25 |
| [0005](0005-one-container-one-volume-one-replica.md) | Run one container on one volume with a read-only root | accepted | 2026-11-25 |
| [0006](0006-deployment-history-in-sqlite.md) | Keep the deployment history in one SQLite file on the workspace volume | accepted | 2026-11-26 |
| [0007](0007-angular-dashboard-served-by-the-service.md) | Build the dashboard as an Angular app in the image and serve it from the service | accepted | 2026-11-26 |
| [0008](0008-github-oauth-for-one-login.md) | Sign in to the dashboard with GitHub OAuth for one allowed login | accepted | 2026-11-26 |
| [0009](0009-server-side-session-with-lax-cookie.md) | Keep the login in a server-side session with a SameSite=Lax cookie | accepted | 2026-11-26 |

> Add a row for every new ADR. `bootstrap/check.sh` reports a file missing from
> this table.

Start from [`_template.md`](_template.md).
