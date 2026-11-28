---
title: Sign in to the dashboard with GitHub OAuth for one allowed login
type: decision
status: current
updated: 2026-11-26
sources:
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/DashboardSecurityConfig.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/OwnerOnlyUserService.java
  - src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java
tags: [server-mode, dashboard, security]
---

# 0008 — Sign in to the dashboard with GitHub OAuth for one allowed login

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-26 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

The dashboard shows failure causes and repository names, so it needs a login. There is one user: the owner. The owner already has a GitHub account with two-factor authentication, and the service has no user store. The CLI must not start any security code.

## Decision

We use Spring Security's OAuth2 login with a GitHub OAuth App that asks for no scope. After the sign-in the service compares the GitHub login with `MB2E_DASHBOARD_ALLOWED_LOGIN` (case-insensitive), keeps only the login, and drops the GitHub token. Another account goes to `/dashboard/denied` with no session. The dashboard and its security start only when all three `MB2E_DASHBOARD_*` variables are set; the CLI and a server without them exclude the security auto-configuration, which a test proves.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| A password in a variable, form login | A new secret to rotate, no second factor, a brute-force target |
| HTTP basic auth in Caddy | Same weaknesses, and the service could not name the user in its log |
| Authentication only in a proxy (oauth2-proxy) | A second component to run and configure for one user |
| No login, loopback only, SSH tunnel | Hard to use from a phone or from another computer |

## Consequences

**Positive:**

- No password store; GitHub's two-factor authentication protects the dashboard.
- The service holds no GitHub token for the user, so a leak of the session gives no GitHub access.

**Negative — the cost we accept:**

- The sign-in depends on github.com being reachable.
- Two OAuth Apps to create by hand (production and local), because GitHub allows one callback address per app.

## Revisit when

A second user needs access, or roles other than "owner" appear.
