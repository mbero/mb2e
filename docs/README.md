---
title: Documentation map
type: context
status: current
updated: 2026-11-06
sources: []
tags: [meta]
---

# Documentation map

This folder is the project knowledge base. Each sub-folder answers one kind of
question. Put a document where its question belongs. One document, one question.

---

## The routing table

| Folder | Answers | Lifetime | Example filename |
|---|---|---|---|
| `context/` | What is this project? | Long. Update on a structural change. | `architecture.md` |
| `decisions/` | Why is it built this way? | Permanent. Never edit, only supersede. | `0007-use-outbox-pattern.md` |
| `knowledge/` | How does X work? | Long. Update when the code changes. | `order-checkout-data-flow.md` |
| `lessons-learned/` | What breaks, and how do I react? | Long. Grows after every incident. | `database-connection-pool-exhaustion.md` |
| `todo/` | What is still open? | Short. Closed items get checked off. | `staging-deployment-gap-analysis.md` |
| `published/` | What do the stakeholders read? | Snapshot. Regenerate, do not patch. | `order-checkout-api-parity.md` |
| `analysis/` | What is the deep analysis of one area? | Project-phase. | `analysis/checkout/business-rules.md` |
| `sessions/` | What happened in earlier sessions? | Append only. | `history.md` |

### How to choose

- The answer explains **mechanism** → `knowledge/`.
- The answer records a **choice between options** → `decisions/`.
- The answer lists **work items with a status** → `todo/`.
- The answer describes a **failure and its fix** → `lessons-learned/`.
- The answer is for **someone outside the team** → `published/`.
- The answer is a **stable fact about the repository itself** → `context/`.

When two folders fit, pick the one higher in the table and link from the other.

---

## Mandatory frontmatter

Every `.md` file under `docs/` starts with this block. `bootstrap/check.sh`
fails the build when it is missing or incomplete.

```yaml
---
title: Order checkout data flow
type: knowledge        # context | decision | knowledge | lesson | todo | published | analysis
status: current        # draft | current | superseded
updated: 2026-11-06    # ISO date of the last substantive change
sources:               # evidence this document rests on; [] only for meta files
  - src/main/java/com/example/CheckoutService.java:142
  - config/application.yaml#spring.datasource.url
tags: [checkout, payments]
---
```

| Field | Rule |
|---|---|
| `title` | A sentence, not a filename. It appears in a generated index. |
| `type` | One of the seven values. It must match the folder. |
| `status` | `draft` means unverified. `superseded` must name the replacement in the body. |
| `updated` | Change it on every substantive edit, not on a typo fix. |
| `sources` | At least one entry, unless the file is a README or a template. A repository path must exist: `bootstrap/check.sh` fails on a path that the code no longer has. |
| `tags` | Lower case, kebab-case, no more than five. |

---

## Writing rules that apply everywhere

1. **Cite evidence.** `file:line`, a configuration key, or the exact log line.
   A claim without a source is a hypothesis, and you must label it as one.
2. **Separate fact from guess.** Use a "Confirmed" and a "Hypothesis" heading
   when both appear.
3. **End with the gap.** The last section names what you could not verify and why.
4. **Use standard Markdown links.** `[[wikilinks]]` only when the Obsidian
   profile is enabled, and never in `published/`.
5. **Write in English.**
6. **One topic per file.** Split a file that answers two questions.
7. **Date every claim that can expire.** A version number, a branch, or a
   measurement needs the date it was true.
8. **One fact, one home.** Write a fact in one document and link to it from
   the others. Two copies of a fact become two different facts.
9. **Change the document with the code.** A code change that makes a document
   wrong updates that document in the same change.

---

## Before you explore the code

1. Read [`INDEX.md`](INDEX.md). It lists every document with its title, type,
   status, and date. `bootstrap/check.sh --index` generates it.
2. Read the matching file and use it as the primary source.
3. Explore the code only when no document answers the question.

This is the point of the knowledge base. It turns a repeated question into a
file read instead of a codebase scan.
