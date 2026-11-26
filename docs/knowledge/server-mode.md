---
title: Server mode — how a push becomes a deployed site
type: knowledge
status: current
updated: 2026-11-26
sources:
  - src/main/java/io/github/mbero/mb2e/ServerLauncher.java
  - src/main/java/io/github/mbero/mb2e/ServerEnvironment.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/GithubWebhookController.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/DeployQueue.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/DashboardApiController.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/DashboardFiles.java
  - Dockerfile
  - src/main/java/io/github/mbero/mb2e/adapter/out/persistence/SqliteDeploymentHistory.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/DashboardSecurityConfig.java
  - src/test/java/io/github/mbero/mb2e/DashboardLoginEndToEndTest.java
  - src/main/java/io/github/mbero/mb2e/domain/service/DeployService.java
  - src/test/java/io/github/mbero/mb2e/ServerModeEndToEndTest.java
tags: [server-mode, webhook, deploy]
---

# Server mode

## Summary

- `java -jar mb2e.jar serve` starts a web server. Every other argument list starts the CLI.
- A signed push webhook starts one deployment: sync, build, commit, push to the target repository.
- One run at a time; a burst of pushes gives at most two runs.

---

## Q: How does a push become a deployed site?

**Date:** 2026-11-25
**Answered from:** code and a running system

### Answer

1. GitHub sends `POST /webhook/github`. The controller checks the body size and the HMAC-SHA256 signature first.
2. It filters on event, repository and branch. A valid push gets `202` at once.
3. `DeployQueue` runs `DeployService` on one worker thread. One more run may wait.
4. `DeployService` syncs the notes clone and the target clone, and runs the same pipeline as the CLI.
5. If the site changed, it commits with the body `Source commit: <id>` and pushes. A rejected push is retried from a fresh sync, three times at most.
6. The outcome is `deployed`, `up_to_date`, `refused` or `failed`. `GET /healthz` shows the last one.
7. `DeployQueue` stores each finished run in `<workspace>/history.db` (newest 500). A history error is logged as `step=history` and never fails the run.

Other facts:

- `mb2e.mode` is set only by the launcher. `SPRING_APPLICATION_JSON` cannot change it.
- Settings come from `MB2E_*` variables (`ServerEnvironment`). A secret may come from a `_FILE` twin.
- The endpoints are plain classes routed by `WebRoutes`; another method or path gets `404`. A push during shutdown gets `503`.
- Dashboard API (read-only): `GET /api/status` (state `idle`/`running`/`waiting`, uptime, repositories, site URL, `maxAttempts`) and `GET /api/deployments?limit=1..500` (default 50, newest first). A failure cause shows `<local path>` in place of the workspace or template path.
- The dashboard exists only when `MB2E_DASHBOARD_ALLOWED_LOGIN`, `MB2E_DASHBOARD_OAUTH_CLIENT_ID` and `MB2E_DASHBOARD_OAUTH_CLIENT_SECRET` are all set; a partial set is refused at start. Without them the server starts no web security and `/api/*` answers `404`.
- UI: the image builds `ui/` in a Node stage and copies the result to `/opt/mb2e/dashboard` (`MB2E_DASHBOARD_FILES`). `DashboardFiles` serves it under `/dashboard/`: a path without a file extension gets `index.html`, a missing or unreadable asset or a path outside the folder gets `404`. Hashed bundles are cached for a year, `index.html` never. The shell is public; it holds no data. Without a built UI the server logs one warning and `/dashboard/` answers `404`. The jar and the Maven build contain no Node and no UI files.
- Login: GitHub OAuth with no scope, for one login (case-insensitive). The GitHub token is dropped after the user name is read (`DiscardedClientRepository`). Another account is redirected to `/dashboard/denied?login=...` and gets no session.
- Session cookie `mb2e_session`: `HttpOnly`, `Secure`, `SameSite=Lax`, idle limit 30 minutes, total limit 12 hours (`SessionLifetimeFilter`). Lax, not Strict: GitHub's redirect back to `/login/oauth2/code/github` is a cross-site navigation, and a Strict cookie would lose the login state.
- `/api/*` without a session answers `401 {"error":"Sign in first."}`. `POST /api/logout` needs the `XSRF-TOKEN` cookie value in the `X-XSRF-TOKEN` header and answers `204`. The webhook and `/healthz` stay outside the login.
- Each login event writes `step=login outcome=success|denied|failed|logout login=<login>`; never a token, a cookie or a code.
- A git child process never sees an `MB2E_*` variable: each clone gets only its own token (`GitRunner.childEnvironment`).
- A lock file `<workspace>/.mb2e.lock` refuses a second service on one workspace.
- At start-up, `MB2E_SYNC_ON_START=true` runs one deployment to catch up missed pushes. `MB2E_POLL_INTERVAL_SECONDS` adds a periodic run.
- Stop: the context waits up to 60 seconds for a run in progress. The JVM exit code is 143.

### Evidence

- `ServerModeEndToEndTest` (5 tests): signed push, unchanged site, non-ASCII note name, custom `CNAME` kept, burst of ten pushes, refusal when no note has the tag.
- Live run on 2026-11-25: `/healthz` returned `{"status":"up","lastDeploy":null}`; an unsigned POST returned `401`.
- Container run: healthy in about 8 seconds; stop in 1 second; root file system read-only.

### Gap

- No run against real GitHub. Tests use bare repositories and `file://` URLs.
- Git LFS is not supported.
