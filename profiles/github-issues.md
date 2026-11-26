# Profile — GitHub Issues

**Read this when the agent writes an issue, a pull request text, or an issue
comment, or changes an item on the project board.**

The board, its status field and its item ids are project facts: they live in
`docs/context/ecosystem.md`, section "Backlog and project board".

## Issue text

| Section | Content |
|---|---|
| **First line** | `Part of #N.` for a phase or a step of an epic; the dependency, if any |
| **Goal** or **Description** | The current situation and why the work is needed |
| **Steps** | A checklist `- [ ] **S07** <step>: <acceptance criteria>`, one box per step |
| **Open questions** | What must be decided before the work starts, with who decides |
| **Out of scope** | What the issue does not cover |

Rules:

- Title: `<epic name> - <phase or topic>`, as the existing issues do.
- Name *what* and *why*, not *how*. Acceptance criteria are observable results.
- Write in English. Mark an idea that is not specified yet as such in the first line.

## Pull request text

Use the `pr` skill template: **Summary** (a diagram, call tree or diff sketch),
**Evidence** (before and after: test counts, command output), **Merge Danger**
(one-way or two-way door, blast radius). Close the phase issues with
`Closes owner/repo#N` and refer to the epic with `Part of`.

## Ticking steps

- Tick a step box only after its acceptance criteria are verified in this session.
- Record a deviation next to the box (for example a measured value that differs from the plan) instead of ticking silently.
- Edit the body with `gh issue edit N --body-file <file>`; retry on a transient GraphQL error.

## Board status

| Moment | Status |
|---|---|
| New idea or follow-up | Backlog |
| Work on the phase starts | In progress (the epic too) |
| The phase is committed and pushed | In review |
| Merged and accepted | Done — **only the owner sets it** |

## Test result comment

Base every statement on observed evidence. Sections: **Verified**, **Commands**,
**Confirmed**, **Still to verify**, **Before release**. "Should work" is not a
test result.
