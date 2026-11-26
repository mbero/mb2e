---
title: Keep the deployment history in one SQLite file on the workspace volume
type: decision
status: current
updated: 2026-11-26
sources:
  - src/main/java/io/github/mbero/mb2e/adapter/out/persistence/SqliteDeploymentHistory.java
  - src/main/java/io/github/mbero/mb2e/domain/port/out/DeploymentHistory.java
  - src/main/resources/db/deployments.sql
  - Dockerfile
tags: [server-mode, dashboard, persistence]
---

# 0006 — Keep the deployment history in one SQLite file on the workspace volume

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-26 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

The dashboard shows the newest deployments and must keep them across a restart. Before this change the service kept only the last result in memory (`LatestDeploy`). The owner asked for "a light database". The service runs as one container with one volume (ADR 0005), writes at most a few rows an hour, and reads at most 500 rows.

## Decision

We store each finished deployment as one row in `history.db`, a SQLite file in the workspace volume, through the outbound port `DeploymentHistory` and plain JDBC (`SqliteDeploymentHistory`). The table keeps the newest 500 rows. A database error is logged as `step=history` and never fails a deployment. The image carries the native SQLite library in `/opt/mb2e/native`, because the driver cannot load it from the `noexec` `/tmp` of the read-only container.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| Memory only | A restart loses the history, and the dashboard would show an empty page after each update |
| A JSON or log file read back | Concurrent write and read, trimming and ordering need code that a database already has |
| PostgreSQL or another server | A second container and its backups for a few rows an hour |
| H2 file database | Works, but has no advantage here, and SQLite files are easy to inspect with standard tools |
| JPA / Spring Data | One table and two queries do not need an ORM or its start-up cost |

## Consequences

**Positive:**

- The history survives a restart and an image update; it lives on the volume that already holds the clones.
- The domain sees only the port; a test uses a temporary file.

**Negative — the cost we accept:**

- A native library per platform; the image unpacks it for the build platform (`x86_64` or `aarch64`).
- Only one process may write the file; ADR 0005 already allows one container.

## Revisit when

The dashboard needs more than one table, a search over old runs, or more than one service instance.
