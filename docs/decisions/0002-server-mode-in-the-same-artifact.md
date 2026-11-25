---
title: Run the server mode from the same jar and separate the modes with a property
type: decision
status: current
updated: 2026-11-25
sources: [AS_A_SERVICE_ARCHITECTURE_PLAN.md]
tags: [server-mode, spring-shell, configuration]
---

# 0002 — Run the server mode from the same jar and separate the modes with a property

| Field | Value |
|---|---|
| Status | accepted |
| Date | 2026-11-25 |
| Deciders | Project owner (pending) |
| Supersedes | none |
| Superseded by | none |

## Context

mb2e is a CLI built on Spring Shell. The service mode must reuse the same engine
and the same jar (plan AD-01, AD-02). The shell commands must not exist in the
server mode, and no external source may select the mode.

Evidence: `Mb2eEnvironment.java` builds the fixed property source `mb2e-fixed`,
which outranks `SPRING_APPLICATION_JSON`. `ShellConfig.java` registers every shell command.

## Decision

The launcher sets `mb2e.mode=cli` in `mb2e-fixed` (`Mb2eEnvironment`). `ShellConfig`
applies only when `mb2e.mode=cli` or when the property is missing. Phase 1 (step S04)
implements this part only.

**Variant of the Spring Shell exclusion used by the code: the fallback (confirmed at step S09).**
The Spring Shell auto-configurations stay on the classpath. `Mb2eServerModeTest`
switches the runners off with `spring.shell.interactive.enabled=false`,
`spring.shell.noninteractive.enabled=false` and `spring.shell.script.enabled=false`.
`Mb2eEnvironment.forServer` sets the same three properties. The server context still holds one JLine
`Terminal` bean (counted in a test on 2026-11-25), but no runner reads it, and the live server prints no
terminal warning. The exclusion variant stays unused: it needs an `[UNVERIFIED]` Spring Boot helper and gives no
behaviour that the fallback lacks.

## Alternatives considered

| Option | Why we rejected it |
|---|---|
| A second jar for the server | Two build artifacts and two release paths for one engine |
| `MB2E_MODE=server` environment variable | `docker run <image> run --dry-run` must still reach the CLI |
| `@Profile` for the mode | `Mb2eEnvironment` blocks profiles on purpose |

## Consequences

**Positive:**

- The CLI behaviour is unchanged; `Mb2eApplicationTest` passes without edit.
- No JSON key or system property can switch the mode.

**Negative — the cost we accept:**

- The server context still loads the Spring Shell auto-configurations until step S09.
- A `@SpringBootTest` that sets no mode runs as `cli`; tests must set the mode explicitly.

**Follow-up work:**

- None for the server mode. Revisit the exclusion if a Spring Shell upgrade starts a runner by itself.

## Revisit when

Spring Boot or Spring Shell changes its auto-configuration list or its runners.
