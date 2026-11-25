---
title: Knowledge archive
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta, knowledge]
---

# Knowledge archive

Answers to the question **"how does X work?"**. Each file turns a one-time
explanation into a reusable document.

## Rules

1. **One topic per file.** Filename is the topic in kebab-case:
   `order-checkout-data-flow.md`, not `qa-2026-11.md`.
2. **Append, do not replace.** A new question on an existing topic becomes a new
   `## Q:` section in the same file.
3. **Every claim cites evidence.** `file:line`, a configuration key, or the exact
   log line. No source means it is a hypothesis, and you must label it.
4. **Date the answer.** Code moves. An undated answer rots silently.
5. **End with the gap.** Name what you could not verify.
6. **Set `status: draft`** when the answer rests on reading alone, and
   `status: current` when you ran it or saw it in a log.

## When the agent offers to save

After a substantive technical answer, the agent asks one line:
`Save this to docs/knowledge/? `

It does **not** ask after a meta question, a file operation, or a session
command. The full procedure: `.agents/procedures/save-knowledge.md`.

## Why this folder pays for itself

An agent that finds the answer here reads one file. An agent that does not
scans the codebase. The second costs ten to a hundred times more tokens and
gives a less reliable answer.

Start from [`_template.md`](_template.md).
