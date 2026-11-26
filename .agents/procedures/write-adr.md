# Procedure — record an architecture decision

**Trigger:** a choice gets made between two or more real options, and the choice
will constrain future work.

Examples: picking a library, defining a service boundary, accepting technical
debt, making a deliberate exception to a convention.

**Not a trigger:** a bug fix, a rename, a version bump, a task list.

## Steps

1. **Confirm it is a decision.** Ask: *was there a rejected option?* If no, it
   belongs in `docs/knowledge/`, not here.

2. **Find the next number.** List `docs/decisions/`, take the highest `NNNN`,
   add one. Never reuse a number.

3. **Name the file** `NNNN-kebab-slug.md`. The slug states the decision, not the
   problem: `0007-use-outbox-pattern.md`, not `0007-messaging-problem.md`.

4. **Copy `docs/decisions/_template.md`.** Keep every section.

5. **Write Context first, Decision second.** Context states the forces and cites
   evidence. It must not describe the solution.

6. **List the rejected alternatives with the deciding reason.** One line each.
   An ADR with no rejected option is incomplete — go back to step 1.

7. **Write the negative consequences.** Every decision has a cost. Name it. A
   consequences section with only benefits is not credible.

8. **Set `Revisit when`** to a concrete trigger: a threshold, a version, a date.

9. **Add a row to the index table** in `docs/decisions/README.md`.

10. **Set the status.** `proposed` until the deciders agree, then `accepted`.

11. **Refresh the index.** Run `bootstrap/check.sh --index`.

## Superseding an earlier ADR

1. Write the new ADR. Fill its `Supersedes` field.
2. Open the old ADR. Change `status` to `superseded` and fill `Superseded by`.
3. Change nothing else in the old file. An accepted ADR is a historical record.
