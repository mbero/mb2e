# Procedure — review before you hand over a change

**Trigger:** you finished a code change and are about to report it as done.

**This procedure never commits.** It prepares the working tree for the user's
decision.

## Steps

1. **Re-read the request.** List what it asked for. Confirm each item is done.
   Name anything you left out and why.

2. **Read your own diff.** `git diff` and `git diff --staged`. Look for:
   - a debug statement, a `TODO`, or commented-out code you left behind,
   - a hard-coded value that belongs in configuration,
   - a secret, a token, or an absolute machine path,
   - a fully qualified class name in a method body.

3. **Check error handling.** For every new path, ask:
   - What happens when the input is null, empty, or malformed?
   - What happens when the downstream call times out or fails?
   - Is the failure logged with enough context to diagnose it?
   - Does the caller receive a usable error?

   Error handling is the single most common gap in generated code. Review it
   explicitly, not incidentally.

4. **Check test coverage of new behaviour.** Every new unit of observable
   behaviour needs a test: an endpoint, a filter, a repository query, an
   exception handler, a scheduled job. A pure data holder does not.

5. **Run the commands.** Take the lint command and the unit test command from
   the toolchain table in `AGENTS.md`. Run both.
   Report the real result. A failing test is a result, not a reason to stay quiet.

6. **Check the conventions.** Read `docs/context/conventions.md` and confirm the
   change matches naming, layering, and style.

7. **Update the knowledge base when the change invalidates it.**
   - A changed boundary or data flow → `docs/context/architecture.md`.
   - A new command → `docs/context/toolchain.md`.
   - A changed business rule or actor → `docs/context/domain.md`.
   - A decision made along the way → an ADR.
   - A document that is now wrong → fix it or mark it `status: superseded`.

8. **Run `bootstrap/check.sh --index`, then `bootstrap/check.sh`,** if you
   touched anything under `docs/`.

9. **Report.** State what changed, what you verified, what you did not verify,
   and which files are in the working tree.

10. **Stop.** Do not commit, push, or create a branch. Ask whether to commit.
