---
title: Project exceptions to the global coding rules
type: lesson
status: current
updated: 2026-11-26
sources:
  - pom.xml
  - ui/src/app/app.ts
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
