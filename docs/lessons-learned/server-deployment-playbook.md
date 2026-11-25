---
title: Server mode deployment — landscape and diagnostic playbooks
type: lesson
status: draft
updated: 2026-11-25
sources:
  - docker-compose.yml
  - .env.example
  - src/main/java/io/github/mbero/mb2e/ServerLauncher.java
tags: [server-mode, docker, operations]
---

# Server mode deployment — landscape and diagnostic playbooks

> Status `draft`: the cases below come from local tests and a local container, not from a production incident.

## Part 1 — Current state

### What this component does

The container runs `mb2e serve`. It receives a GitHub push webhook and pushes the built site to the target repository.

### Configuration that matters

| Key | Current value | Where | Why it matters |
|---|---|---|---|
| `MB2E_WEBHOOK_SECRET` | at least 32 characters | `.env` | Must equal the secret of the GitHub webhook |
| `MB2E_GITHUB_TOKEN` | fine-grained token | `.env` | Needs read access to the notes and write access to the target |
| `MB2E_WORKSPACE` | `/var/lib/mb2e` | `docker-compose.yml` | One volume, one container |
| `stop_grace_period` | `75s` | `docker-compose.yml` | Longer than the 60 s that a run may need to finish |

---

## Part 2 — Diagnostic playbooks

### Shared toolkit

```bash
docker compose ps
docker compose logs --tail 100 mb2e
curl -fsS http://127.0.0.1:8080/healthz
```

### Failure mode 1 — the service does not start

**Symptoms:** the container exits with code 1 at once.
**Check:** `docker compose logs mb2e`. The log has one line. It names the variable that is missing or wrong, or the lock file.
**Action:** fix the variable in `.env`. For the lock file, stop the second container that uses the volume.

### Failure mode 2 — GitHub shows a delivery error

**Symptoms:** the webhook page shows `401`, `404` or a time-out.
**Check:** `401` means the secret differs. `404` means the path is not `/webhook/github`. A time-out means the reverse proxy does not reach port 8080.
**Action:** correct the secret or the proxy. Redeliver the event from the GitHub webhook page.

### Failure mode 3 — the site does not change after a push

**Symptoms:** the delivery shows `202`, but the target repository has no new commit.
**Check:** `/healthz` shows `lastDeploy.outcome`. `up_to_date` means the built site did not change. `refused` means a safety check stopped the run, for example no note has the tag. `failed` means git failed.
**Action:** read the `step=deploy` lines in the log. For `failed`, check that the token has not expired and has write access to the target.

### Failure mode 4 — an image does not load on the site

**Symptoms:** images are broken only for notes whose images use Git LFS.
**Cause:** the clone holds pointer files. The server mode does not support Git LFS.
**Action:** store the images in the repository without LFS.
