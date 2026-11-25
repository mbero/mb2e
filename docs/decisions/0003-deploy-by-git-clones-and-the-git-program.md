---
title: Deploy with two shallow clones and the git program
type: decision
status: current
updated: 2026-11-25
sources: [AS_A_SERVICE_ARCHITECTURE_PLAN.md]
tags: [server-mode, git, security]
---

# 0003 — Deploy with two shallow clones and the git program

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-25 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

The service must read the notes repository and push the built site to the target repository. The CLI has a read-only git adapter and must stay unable to push.

Evidence: `adapter/out/external/GitRunner.java`, `GitWorkspaceProgram.java`, `domain/service/DeployService.java`.

## Decision

`DeployService` syncs a shallow clone of each repository, runs the pipeline, commits with the body `Source commit: <id>`, and pushes. It retries a rejected push from a fresh sync, three times at most, and never forces. The adapter runs the `git` program through `GitRunner`, which sets a time budget, kills the process tree on time-out, and masks secrets. The token reaches git only through the process environment. Only `ServerConfig` creates a workspace that can push; `BuildConfig` never references one, and a test checks this.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| A Java git library | A second implementation of git behaviour to trust; the CLI adapter already uses the program |
| Push with the token in the URL | The URL appears in `.git/config`, process lists and logs |
| Force push on a rejected push | It can destroy a commit that someone else made in the target |

## Consequences

**Positive:**

- The CLI cannot push.
- No secret is in a URL, an argument or a log line.
- A run that finds no change makes no commit.

**Negative — the cost we accept:**

- The image must contain git 2.32 or later.
- A push rejected three times ends the run with the outcome `failed`.

## Revisit when

Git LFS support is needed, or the target repository becomes too large for a shallow clone.
