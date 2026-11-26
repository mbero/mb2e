---
title: Project exceptions to the global coding rules
type: lesson
status: current
updated: 2026-11-25
sources:
  - pom.xml
tags: [qa, rules]
---

# Project exceptions

> Loaded at session start after the global rules. An entry here wins over a global rule.

## [P-TEST-01] No mock library at all (overrides R-TEST-03)
- Rule: tests use hand-written fakes of the ports, also at the boundaries. Mockito is not on the class path.
- Why: the owner chose a Mockito-free test suite on 2026-11-25 (PR #4).
- Verification: test — `pom.xml` excludes `org.mockito` from `spring-boot-starter-test`, so a Mockito import does not compile.
