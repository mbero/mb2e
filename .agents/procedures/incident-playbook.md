# Procedure — react to an incident

**Trigger:** something fails in a running environment — an error in production,
a failed deployment, a stuck queue, a degraded endpoint.

## Steps

### 1. Look for an existing playbook first

List `docs/lessons-learned/`. Match the symptom against the files. If a
playbook matches, follow it. Do not diagnose from zero.

### 2. Capture the evidence before you change anything

| Capture | How |
|---|---|
| The exact error message | Copy it verbatim. Do not paraphrase. |
| When it started | Timestamp of the first occurrence. |
| What changed | The last deployment, configuration change, or data load. |
| Scope | One instance, one environment, or everything. |

A fix applied before the evidence is captured destroys the postmortem.

### 3. Separate symptom from cause

State the symptom. Then list candidate causes. Mark each as `confirmed` or
`suspected`. Do not act on a suspected cause as if it were confirmed.

### 4. Confirm the cause with a command

Every diagnosis step must be a command whose output decides the question. If
you cannot write that command, you are guessing.

### 5. Apply the smallest fix that restores service

Record what you changed, where, and at what time.

### 6. Verify

Run the command that proves the symptom is gone. Not "it looks fine."

### 7. Write or extend the playbook

While the detail is fresh:

- A new failure domain → a new file in `docs/lessons-learned/`.
- A new mode of a known failure → a new `### Failure mode` section in the
  existing file.

Follow `docs/lessons-learned/_template.md`. Include the exact symptom string, so
the next search finds it.

### 8. Record the prevention work

Open a `docs/todo/` item for the change that stops a recurrence. A playbook
without prevention work means the incident repeats forever.

### 9. If a decision came out of it, write an ADR

Use `write-adr.md`.
