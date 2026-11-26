---
title: Keep the login in a server-side session with a SameSite=Lax cookie
type: decision
status: current
updated: 2026-11-26
sources:
  - src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/SessionLifetimeFilter.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/DashboardSecurityConfig.java
tags: [server-mode, dashboard, security]
---

# 0009 — Keep the login in a server-side session with a SameSite=Lax cookie

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-26 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

After the GitHub sign-in (ADR 0008) the browser must prove the login on each API call. The specification first asked for `SameSite=Strict`. GitHub's redirect back to `/login/oauth2/code/github` is a cross-site navigation, so the browser does not send a Strict cookie with it and the login state is lost.

## Decision

We keep the login in the servlet session of the service. The cookie `mb2e_session` is `HttpOnly`, `Secure` and `SameSite=Lax`; the session ends after 30 idle minutes and after 12 hours in total (`SessionLifetimeFilter`). Writes need a CSRF token: the server sets the `XSRF-TOKEN` cookie, and the Angular app sends its value in the `X-XSRF-TOKEN` header; logout is `POST /api/logout`. A restart ends every session.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| `SameSite=Strict` | Loses the login state on GitHub's redirect back |
| A JWT in local storage | Readable by any script on the page, and cannot be revoked before it expires |
| A signed stateless cookie | Logout and the total limit need server state anyway |
| A session store on disk or in SQLite | Survives a restart, but one user signing in again costs less than the code |

## Consequences

**Positive:**

- No token reaches JavaScript; logout ends the session at once.
- Lax still blocks the cookie on cross-site `POST`, and the CSRF token covers the rest.

**Negative — the cost we accept:**

- A restart or an image update asks for a new sign-in.
- The session lives in one process; this fits one container (ADR 0005).

## Revisit when

The service runs more than one replica, or restarts become frequent enough to annoy the owner.
