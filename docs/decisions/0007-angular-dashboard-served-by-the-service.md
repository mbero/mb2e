---
title: Build the dashboard as an Angular app in the image and serve it from the service
type: decision
status: current
updated: 2026-11-26
sources:
  - ui/package.json
  - Dockerfile
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/DashboardFiles.java
tags: [server-mode, dashboard, ui, docker]
---

# 0007 — Build the dashboard as an Angular app in the image and serve it from the service

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-26 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

The dashboard is one read-only page with a few states, a table and polling. The owner asked for Angular. The CLI jar must not grow a Node toolchain, and the service runs as one container (ADR 0005).

## Decision

We write the UI as an Angular single-page app in `ui/` (standalone components, signals, plain CSS, no UI library). A Node stage of the `Dockerfile` builds it; the final image copies the files to `/opt/mb2e/dashboard`. The service serves them under `/dashboard/` (`DashboardFiles`): a path without a file extension gets `index.html`, a missing asset gets `404`. The Maven build and the jar contain no Node and no UI files.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| Server-side HTML (Thymeleaf) | Not the owner's choice; polling and expandable rows need client code anyway |
| UI files inside the jar (frontend-maven-plugin) | Every Maven build, also of the CLI, would need Node |
| A separate UI container or Caddy serving the files | A second artifact to version and deploy for one page |

## Consequences

**Positive:**

- One image, one deployment; the UI and the API always match.
- `mvn verify` stays fast and Node-free; the UI has its own tests and CI workflow.

**Negative — the cost we accept:**

- The image build needs Node and the npm registry.
- Two toolchains in one repository: Maven for the service, npm for `ui/`.

## Revisit when

The UI needs its own release cycle, or a second consumer of the API appears.
