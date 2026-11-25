# Procedure — session handoff

**Trigger:** the session is ending, the context is getting long, or the user
asks for a summary of the session.

The goal: a new session with an empty context must be able to continue the work
from `docs/sessions/history.md` alone.

## Steps

1. **Open `docs/sessions/history.md`.** Read the newest entry so your entry
   continues it instead of repeating it.

2. **Find the next session number.**

3. **Write a new entry directly below the marker line**, above the older
   entries. Newest first.

4. **Fill every section of the entry template.** An empty section is a signal,
   so write "none" rather than deleting the heading.

5. **Be concrete.** Every claim names a file, a command, or an identifier.
   - Bad: "investigated the auth flow"
   - Good: "read `AuthFilter.java:88`; confirmed the token is validated twice,
     once in the filter and once in the controller advice"

6. **Record the dead ends.** The approach that failed, and why. This saves the
   next session more time than the approach that worked.

7. **Record every open question with an owner** — a person, a system, or a file
   that can answer it. An open question with no owner never closes.

8. **Record what you did not do** that was in scope, and why.

9. **Update `updated:` in the frontmatter.**

10. **Rotate the log.** Keep the ten newest entries in `history.md`. Move each
    older entry, unchanged, to `docs/sessions/archive/YYYY.md`, where `YYYY`
    is the year of the entry. Give a new archive file the frontmatter block
    from `docs/README.md` with `type: context`.

11. **Do not commit.** Leave the change in the working tree.

## What does not belong in the handoff

- The full conversation. Write conclusions, not transcript.
- Content that belongs in `docs/knowledge/` or `docs/decisions/`. Write it
  there and link to it from the handoff entry.
