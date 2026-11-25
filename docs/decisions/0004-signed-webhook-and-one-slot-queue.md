---
title: Accept a signed webhook and run one deployment at a time
type: decision
status: current
updated: 2026-11-25
sources: [AS_A_SERVICE_ARCHITECTURE_PLAN.md]
tags: [server-mode, webhook, security]
---

# 0004 — Accept a signed webhook and run one deployment at a time

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-25 |
| Deciders | Project owner |
| Supersedes | none |
| Superseded by | none |

## Context

GitHub sends a push event to a public URL. Anyone can send a request to that URL. Pushes can arrive in bursts, and two runs must never write to one workspace.

Evidence: `adapter/in/web/GithubWebhookController.java`, `WebhookSignature.java`, `DeployQueue.java`.

## Decision

The controller checks the body limit and the HMAC-SHA256 signature before any other rule, using `MessageDigest.isEqual`. It then filters on event, repository and branch, and answers `202` before any git work. `DeployQueue` has one worker thread and one waiting slot, so a burst gives at most two runs. A signed but ignored event gets `200`. No response holds a stack trace. `GET /healthz` reports the last outcome and stays `up` after a failed run.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| A shared token in a header | GitHub signs the body; a token does not bind the request to its content |
| An unbounded queue | A burst makes many runs that produce the same result |
| Run in the request thread | GitHub times out after 10 seconds, and a run takes longer |

## Consequences

**Positive:**

- An unsigned request never reaches the queue.
- A burst cannot start parallel git work.
- The newest push is always built, because the waiting run starts after the current run.

**Negative — the cost we accept:**

- A push that arrives while a run and a waiting run exist is merged into the waiting run.
- The service has no rate limit; the reverse proxy provides it.

## Revisit when

The service must handle many repositories, or GitHub changes the signature header.
