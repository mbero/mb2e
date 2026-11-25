# Procedure — fill the knowledge base for the first time

**Trigger:** `bootstrap/init.sh` installed the knowledge base, and the files in
`docs/context/` still hold empty tables and `status: draft`.

The goal: every file in `docs/context/` states facts from this repository, and
each fact cites its evidence.

## Steps

1. **Read `AGENTS.md`.** Check the project identity table and the toolchain
   table against the repository. Correct a wrong value. Replace each `TBD`
   that the repository can answer.

2. **Read the build file and the top-level folders.** Do not read every source
   file. Read the entry points, the configuration files, and one typical
   feature from end to end.

3. **Fill the files in this order.** A later file uses an earlier one.

   | Order | File | Evidence to read |
   |---|---|---|
   | 1 | `docs/context/overview.md` | build file, folder layout, README |
   | 2 | `docs/context/toolchain.md` | build file, CI configuration, scripts |
   | 3 | `docs/context/architecture.md` | entry points, one feature end to end |
   | 4 | `docs/context/conventions.md` | three typical source files, three tests |
   | 5 | `docs/context/local-setup.md` | README, container files, example configuration |
   | 6 | `docs/context/glossary.md` | names of the main types and tables |
   | 7 | `docs/context/domain.md` | the feature you read, plus the user's answers |
   | 8 | `docs/context/ecosystem.md` | client code and message channels to other systems |

4. **Cite the evidence.** Add each file you used to `sources:` in the
   frontmatter, as a repository-relative path. Cite `file:line` in the body.

5. **Leave an empty cell empty.** Do not invent a value to complete a table.
   Name the gap in the section "What could not be verified".

6. **Mark each business rule `inferred`** in `domain.md`. Only a person
   changes a rule to `confirmed`.

7. **Keep `status: draft`.** You read the code and did not run it. The user
   sets `status: current` after a review.

8. **Delete `docs/context/ecosystem.md`** only when the user confirms that the
   project calls no other system. Ask first.

9. **Ask the user the questions that the code cannot answer.** Ask them in one
   list, at the end: the business purpose, the owner, the environments, and
   the open business questions.

10. **Run `bootstrap/check.sh --index`, then `bootstrap/check.sh`.** Correct
    each failure.

11. **Report.** List the files you filled, the gaps you left, and the questions
    that stay open. Do not commit.
