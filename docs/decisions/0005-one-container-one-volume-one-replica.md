---
title: Run one container on one volume with a read-only root
type: decision
status: current
updated: 2026-11-25
sources: [AS_A_SERVICE_ARCHITECTURE_PLAN.md]
tags: [server-mode, docker, operations]
---

# 0005 — Run one container on one volume with a read-only root

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-25 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

The operator runs the service on one small host. Two services on one workspace would corrupt the clones.

Evidence: `Dockerfile`, `docker-compose.yml`, `infrastructure/config/WorkspaceLock.java`, `ServerModeEndToEndTest`.

## Decision

The image runs as user 10001 with `LANG=C.UTF-8`. Compose binds the port to `127.0.0.1`, mounts one named volume at `/var/lib/mb2e`, sets `read_only: true` with a tmpfs `/tmp`, and uses `stop_grace_period: 75s`. A file lock `.mb2e.lock` in the workspace refuses a second service. Measured on 2026-11-25: the service starts, git clones, commits and pushes with this layout; `docker compose stop` takes 1 second and the exit code is 143 (128 plus SIGTERM). The JVM memory flags are `ENTRYPOINT` arguments, because `JAVA_TOOL_OPTIONS` prints a notice on every run.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| Several replicas with a shared volume | Clones and the lock are local state; one run at a time is enough |
| A writable root file system | It gives no benefit; git needs only the volume and `/tmp` |
| TLS inside the service | A reverse proxy on the host already provides TLS and rate limits |

## Consequences

**Positive:**

- A second start fails with one line that names the lock file.
- A compromised process cannot change the image.

**Negative — the cost we accept:**

- No high availability: a host restart stops the service until the container starts again.
- The exit code after a clean stop is 143, not 0.

## Revisit when

The service needs more than one replica, or a platform expects exit code 0 on stop.
