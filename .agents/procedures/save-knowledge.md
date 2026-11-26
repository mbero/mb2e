# Procedure — save an answer into the knowledge base

**Trigger:** you gave a substantive technical answer — an explanation, a data
flow, an architecture description, or a debugging result.

**Do not trigger** after a meta question about the agent, a file operation, or a
session command.

## Steps

1. **Offer, in one line.** End the answer with:
   `Save this to docs/<folder>/? `
   Name the folder you propose. Do not list all seven.

2. **Pick the folder** with the routing table in `docs/README.md`:
   - explains a mechanism → `docs/knowledge/`
   - records a choice → `docs/decisions/` (use `write-adr.md` instead)
   - lists work with a status → `docs/todo/`
   - describes a failure and its fix → `docs/lessons-learned/`
   - is for an outside reader → `docs/published/`

3. **Wait for the answer.** Do not write the file unprompted.

4. **Choose the filename.** Kebab-case, the topic as a noun phrase:
   `order-checkout-data-flow.md`. Reuse an existing file when the topic matches,
   and append a new `## Q:` section instead of creating a near-duplicate.

5. **Write the frontmatter** exactly as `docs/README.md` defines it. Every
   mandatory field. `sources` must list real evidence.

6. **Set the status honestly.**
   - `draft` — you read the code but never ran it.
   - `current` — you ran it, or you saw it in a log or a running system.

7. **Fill the body from the template** in that folder. Keep the section order.

8. **End with the gap.** The last section names what you could not verify.

9. **Refresh the index.** Run `bootstrap/check.sh --index`.

10. **Confirm in one line.** State the path you wrote. Do not restate the content.

## Quality bar

A saved document must let a future reader skip the codebase. If it does not, it
is a note, not knowledge. Add the evidence that makes it standalone.
