---
title: Lessons learned and diagnostic playbooks
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta, incidents]
---

# Lessons learned and diagnostic playbooks

What **goes wrong**, and how to react. `knowledge/` explains how a system works.
This folder explains how it fails.

## Rules

1. **One failure domain per file.** `kafka-offset-reset.md`,
   `database-connection-pool-exhaustion.md`.
2. **Every failure mode follows the same three steps:**
   **Symptoms → Root cause → Resolution.** A reader under pressure needs the
   same shape every time.
3. **A symptom must be copy-pasteable.** The exact error message, the exact log
   line, the exact metric and threshold.
4. **A resolution step must be a command**, not advice. "Check the config" is
   not a step. `grep -n 'auto.offset.reset' config/application.yaml` is.
5. **Separate the confirmed cause from the suspected cause.**
6. **End with what you could not verify.**

## During an incident

Read this folder **first**. Follow an existing playbook instead of diagnosing
from zero. The full procedure: `.agents/procedures/incident-playbook.md`.

## After an incident

Write or extend the playbook while the detail is fresh. An incident with no
playbook will be diagnosed from zero again.

Start from [`_template.md`](_template.md).
