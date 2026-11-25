---
title: Register of code-review findings that may recur
type: lesson
status: current
updated: 2026-11-25
sources:
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitRunner.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/GithubWebhookController.java
tags: [qa, code-review]
---

# Register of code-review findings

> Updated after each merge-request review. Rule: two occurrences promote the finding to a stronger rung.
> Rungs: comment -> AGENTS.md -> skill -> test/linter -> CI

## Entry format

```
## [CAT-NN] Short description of the problem
- Count: N (MR: #...)
- Current rung: comment | AGENTS.md | skill | test/linter | CI
- Proposal: ...
- Status: OBSERVED | TO PROMOTE | DONE
```

## Entries

## [SECURITY-01] A child process inherits every secret of the JVM
- Count: 1 (MR: #4, `GitRunner.java:42-43`)
- Current rung: comment
- Proposal: start each git process from a cleared environment plus an allow-list (`PATH`, `HOME`, `LANG`, git variables, its own credentials). A test asserts that a source process cannot see the target token.
- Status: DONE (fixed in MR #4; rung unchanged)

## [ERRORS-01] Integer overflow on a configured size limit
- Count: 1 (MR: #4, `GithubWebhookController.java:87`: `maxBodyBytes + 1` overflows at `Long.MAX_VALUE`)
- Current rung: comment
- Proposal: give every numeric setting an upper bound in `ServerEnvironment`; test the bound values.
- Status: DONE (fixed in MR #4; rung unchanged)

## [ERRORS-02] Process output read with a time-out returns an empty string
- Count: 1 (MR: #4, `GitRunner.java:85`)
- Current rung: comment
- Proposal: a lost stream is a failure, never empty output. A caller that decides on output (`status --porcelain`) must not see `""` for "unknown".
- Status: DONE (fixed in MR #4; rung unchanged)

## [ERRORS-03] The failure cause reaches only an optional log file
- Count: 1 (MR: #4, `StepLog.java:110`)
- Current rung: comment
- Proposal: the console line of a failed or refused step carries the escaped detail on one line.
- Status: DONE (fixed in MR #4; rung unchanged)

## [SECURITY-02] A configurable text is pasted into a printed shell command without quoting
- Count: 1 (MR: #4, `PublishService.java:315`, commit message)
- Current rung: comment
- Proposal: every value in a printed command goes through `shellWord`; a test uses `"`, `$(...)` and a backtick.
- Status: DONE (fixed in MR #4; rung unchanged)

## [TESTS-01] Mockito in tests, against the project rule
- Count: 1 (MR: #4, web controller tests, `ReportCommandsTest`)
- Current rung: AGENTS.md (`docs/context/conventions.md`, Tests)
- Proposal: hand-written fakes; consider an ArchUnit or Maven Enforcer check that bans the Mockito dependency.
- Status: DONE (all tests use fakes; `pom.xml` excludes Mockito, so the rung is now test/linter)

## [NAMING-01] Exception name without `Failure` or `Refusal`
- Count: 1 (MR: #4, `GitTimeout`)
- Current rung: AGENTS.md (`docs/context/conventions.md`, Naming)
- Proposal: rename; an ArchUnit rule on subclasses of `RuntimeException` would make it a test.
- Status: DONE (fixed in MR #4; rung unchanged)

## [OTHER-01] A code change leaves stale statements in docs/
- Count: 1 (MR: #4, `architecture.md:128`, `glossary.md:46`, `overview.md:88`)
- Current rung: AGENTS.md (section 2, rule 8)
- Proposal: before handover, grep `docs/` for the old claim (for example "never runs `git push`").
- Status: DONE (fixed in MR #4; rung unchanged)
