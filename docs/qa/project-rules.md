---
title: Project exceptions to the global coding rules
type: lesson
status: current
updated: 2026-11-26
sources:
  - pom.xml
  - ui/src/app/app.ts
  - src/main/java/io/github/mbero/mb2e/adapter/in/log/StepLog.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/GithubWebhookController.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/web/DashboardApiController.java
tags: [qa, rules]
---

# Project exceptions

> Loaded at session start after the global rules. An entry here wins over a global rule.

## [P-TEST-01] No mock library at all (overrides R-TEST-03)
- Rule: tests use hand-written fakes of the ports, also at the boundaries. Mockito is not on the class path.
- Why: the owner chose a Mockito-free test suite on 2026-11-25 (PR #4).
- Verification: test — `pom.xml` excludes `org.mockito` from `spring-boot-starter-test`, so a Mockito import does not compile.

## [P-SEC-01] No private link in a committed file
- Rule: before a file goes into this repository, remove every private URL: claude.ai artifact links, private dashboards, operator hosts.
- Why: the repository is public; a private link reveals an account resource and helps nobody who reads the code.
- Verification: manual — `grep -rn 'claude.ai/' docs src README.md` returns nothing.

## [P-SEC-02] API answers hide local paths
- Rule: text that comes from a git or file error and goes into an HTTP answer replaces the workspace and template paths with `<local path>`.
- Why: a path reveals the server layout; the operator finds the full text in the log.
- Verification: test — `DashboardApiControllerTest.deploymentsComeNewestFirstWithUtcTimesAndNullsForMissingCommits`.

## [P-UI-01] Angular templates and styles in separate files
- Rule: each Angular component in `ui/` keeps its HTML in `<name>.html` and its CSS in `<name>.css`, beside `<name>.ts`, linked with `templateUrl` and `styleUrl`. One component per file. No inline `template:` or `styles:`.
- Why: the owner asked for it on 2026-11-26. Markup and CSS read better, and get editor and Prettier support, in their own files.
- Verification: manual — `grep -rnE "^\s+(template|styles):" ui/src/app` returns nothing.

## [P-TEST-02] A characterization test comes before a refactor of untested output
- Rule: before you restructure code whose exact output no test fixes, add a golden test that captures the current output, then refactor until it passes unchanged.
- Why: a refactor must keep the behaviour; without a captured output, a changed byte in the rendered site goes unnoticed.
- Verification: test — `MarkdownBlocksCharacterizationTest` against `src/test/resources/markdown/every-block.html.txt`.

## [P-LOG-01] A log line stays one line and its level shows the outcome
- Rule: quote every free-text value in a `step=` line and replace its line breaks; reduce an outside value (header, id, login) to one safe word. Log `failed` at ERROR, `refused` at WARN, the rest at INFO.
- Why: a line break in a note title or a forged header would add a fake log line; one level for all outcomes hides the failures.
- Verification: test — `StepLogTest.aTitleWithALineBreakCannotForgeASecondLine`, `GithubWebhookControllerTest.everyDeliveryWritesOneLogLineAndAForgedDeliveryIdStaysOneWord`.

## [P-LOG-02] Every answer of an inbound endpoint writes its log line
- Rule: an inbound HTTP endpoint builds each answer, refusals included, through one method that also writes the log line. No early return skips the log.
- Why: a refused webhook (wrong secret, too large) is exactly the case the operator must see in the log.
- Verification: test — `GithubWebhookControllerTest.everyDeliveryWritesOneLogLineAndAForgedDeliveryIdStaysOneWord`.
