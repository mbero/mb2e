---
title: <Failure domain>
type: lesson
status: draft
updated: YYYY-MM-DD
sources:
  - <path/to/file.ext:LINE>
  - <log excerpt or dashboard reference>
tags: []
---

# <Failure domain> — landscape and diagnostic playbooks

## Part 1 — Current state

### What this component does

<!-- Two sentences. Enough to read the playbooks below. -->

### Configuration that matters

| Key | Current value | Where | Why it matters |
|---|---|---|---|
| | | `<!-- file:line -->` | |

---

## Part 2 — Diagnostic playbooks

### Shared toolkit

```bash
# Commands used by more than one playbook below.
```

---

### Failure mode 1 — <name>

**Symptoms**

- Exact message: `<!-- paste it -->`
- Where it appears: <!-- log, metric, user report -->
- How often: <!-- -->

**Root cause**

<!-- Confirmed cause. Cite the evidence. -->

**Suspected contributing factors** *(not confirmed)*

- <!-- -->

**Resolution steps**

1. ```bash
   # confirm the diagnosis
   ```
2. ```bash
   # apply the fix
   ```
3. ```bash
   # verify it worked
   ```

**Prevention**

- <!-- the change that stops this from recurring; link to docs/todo/ -->

---

### Failure mode 2 — <name>

<!-- same shape -->

---

## What could not be verified

<!-- -->

## Related

- <!-- ADR, knowledge file, or todo list -->
