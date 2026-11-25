# AGENTS.md — mb2e

Canonical instructions for every AI coding agent that works in this repository.

`CLAUDE.md`, `.github/copilot-instructions.md`, `.cursor/rules/`, and `GEMINI.md`
are stubs. They point to this file. Never duplicate content into them.

---

## 1. Project identity

| Field | Value |
|---|---|
| Name | mb2e |
| Purpose | Markdown based blog engine: a CLI, and a server mode that deploys the site on a GitHub push |
| Stack | Spring Boot 3.5.16, Spring Shell 3.4.3, Maven |
| Language / runtime | Java 25 |
| Package root / namespace | `io.github.mbero.mb2e` |
| Code location | Repository root: `pom.xml`, `src/`; the knowledge base is in `docs/` |
| Code owner | Private project (github.com/mbero) |
| Repository | https://github.com/mbero/mb2e |
| Default branch | main |
| Documentation language | English |

---

## 2. Hard rules

1. **Never commit, push, or create a branch without an explicit request.**
   Leave all work in the working tree. The user decides when it becomes a commit.
2. **Never state a fact you did not verify.** Cite the evidence: `file:line`,
   a configuration key, or the exact log line.
3. **Label every hypothesis.** Keep a confirmed fact and a guess visually separate.
4. **Never invent a path, an API, a class name, or a configuration key.** Read it first.
5. **Write all documentation in English.**
6. **Use repository-relative paths.** Never write the absolute path of one machine.
7. **Report what you skipped.** A partial result with a stated gap beats a silent gap.
8. **Change the document with the code.** A code change that makes a document
   in `docs/` wrong updates that document in the same change.

---

## 3. Read the knowledge base before you read the code

Follow these three steps for every technical question.

1. **Read** `docs/INDEX.md`. It lists every document with its title, type,
   status, and date. One file read is cheap. A codebase scan is not.
2. **Read** only the documents whose title matches the question. Use them as the primary source.
3. **Explore the code** only when no document answers the question.

Search order when more than one document could match:

| Order | Folder | Answers the question |
|---|---|---|
| 1 | `docs/decisions/` | Why is it built this way? |
| 2 | `docs/knowledge/` | How does it work? |
| 3 | `docs/context/` | What is the shape of the project? |
| 4 | `docs/lessons-learned/` | What breaks, and how do I react? |
| 5 | `docs/todo/` | What is still open? |
| 6 | `docs/published/` | What did we tell the stakeholders? |
| 7 | `docs/analysis/` | What is the deep domain analysis? |

---

## 4. Load on demand

Do not read every file at the start of a session. Read a file when its trigger fires.

| File | Load when |
|---|---|
| `docs/context/overview.md` | You need the module map, the environments, or the business purpose |
| `docs/context/architecture.md` | You change a boundary, a data flow, or an integration |
| `docs/context/conventions.md` | You write or modify any code in this repository |
| `docs/context/local-setup.md` | The build fails, the application fails to start, or the IDE import fails |
| `docs/context/toolchain.md` | You need the exact build, test, or lint command |
| `docs/context/domain.md` | The question is about a business rule, an actor, or a process |
| `docs/context/glossary.md` | A domain term appears that you cannot define |
| `docs/context/ecosystem.md` | The question crosses a repository boundary |
| `docs/sessions/history.md` | **Always, at the start of a session** |

---

## 5. Where new knowledge goes

| You produced | Write it to | Format rules |
|---|---|---|
| An answer to "how does X work" | `docs/knowledge/<topic>.md` | `docs/knowledge/README.md` |
| A business rule, an actor, or a process | `docs/context/domain.md` | Mark it `inferred` until a person confirms it |
| A decision and its reasons | `docs/decisions/NNNN-<slug>.md` | `docs/decisions/README.md` |
| An incident analysis or a diagnostic playbook | `docs/lessons-learned/<area>-<topic>.md` | `docs/lessons-learned/README.md` |
| A list of things that must be done | `docs/todo/<area>-<goal>.md` | `docs/todo/README.md` |
| A page for stakeholders | `docs/published/<area>-<type>.md` | `docs/published/README.md` |
| A deep analysis of one domain area | `docs/analysis/<area>/` | `docs/analysis/README.md` |

Every file under `docs/` starts with the YAML frontmatter block defined in
[`docs/README.md`](docs/README.md). A file without frontmatter fails `bootstrap/check.sh`.
Run `bootstrap/check.sh --index` after you add, rename, or retitle a document.

**Ask before you write.** End a substantive answer with one line:
`Save this to docs/<folder>/? ` Do not ask after a meta question, a file
operation, or a session-management request.

---

## 6. Session start and session end

**At the start of every session:**

1. Read `docs/sessions/history.md`. It holds the decisions that a context reset would lose.
2. Read this file. Do not read the rest of `docs/` yet.

**Before the session ends, or when the context grows long:**

Run the handoff procedure in [`.agents/procedures/session-handoff.md`](.agents/procedures/session-handoff.md).
It adds a structured entry to `docs/sessions/history.md` and keeps that file
at ten entries. Older entries move to `docs/sessions/archive/`.

---

## 7. Procedures

These are tool-neutral. Any agent can read and follow them. A tool-specific
wrapper, if one exists, lives in the folder of that tool, for example
`.github/prompts/`.

| Procedure | Use it when |
|---|---|
| [`save-knowledge.md`](.agents/procedures/save-knowledge.md) | You save an answer into `docs/` |
| [`write-adr.md`](.agents/procedures/write-adr.md) | A decision gets made |
| [`session-handoff.md`](.agents/procedures/session-handoff.md) | The session ends or the context fills |
| [`incident-playbook.md`](.agents/procedures/incident-playbook.md) | Something broke in a running environment |
| [`pre-commit-review.md`](.agents/procedures/pre-commit-review.md) | You finished a code change |
| [`first-fill.md`](.agents/procedures/first-fill.md) | `docs/context/` still holds empty templates |

---

## 8. Toolchain

| Task | Command |
|---|---|
| Build | `mvn -q clean verify` (no Maven wrapper exists) |
| Run, no write | `java -jar target/mb2e.jar run --dry-run` |
| Run | `java -jar target/mb2e.jar run` (writes into the Pages repository) |
| Server mode (local) | `java -jar target/mb2e.jar serve` (needs the `MB2E_*` variables of `.env.example`) |
| Server mode (container) | `cp .env.example .env && docker compose up -d --build` |
| Unit tests | `mvn test` |
| Integration tests | `n/a` |
| Lint / format | `n/a` |
| Knowledge-base check | `bootstrap/check.sh` |
| Knowledge-base index | `bootstrap/check.sh --index` |

---

## 9. Enabled profiles

A profile adds optional rules for one technology or one way of working.
`bootstrap/init.sh` writes the enabled list here. Read a profile only when you
work on that subject.

<!-- PROFILES:START -->
- [`profiles/github-issues.md`](profiles/github-issues.md) — writing issues, PR text, ticking steps, board status
- [`profiles/container-service.md`](profiles/container-service.md) — Docker image, compose, `MB2E_*` settings, the running service
- [`profiles/obsidian.md`](profiles/obsidian.md) — `[[wikilinks]]` in `docs/`, which is browsed in Obsidian
<!-- PROFILES:END -->
