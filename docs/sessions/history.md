---
title: Session history
type: context
status: current
updated: 2026-11-25
sources: []
tags: [meta, sessions]
---

# Session history

Append-only log of agent sessions. It carries the context that a reset would
destroy: decisions, dead ends, and open questions.

**Read this file at the start of every session.**
**Append to it before every session ends.**

Procedure: [`.agents/procedures/session-handoff.md`](../../.agents/procedures/session-handoff.md).

## Rules

1. **Append. Never rewrite an earlier entry.**
2. **Record the dead ends.** The approach that failed saves the next session
   more time than the approach that worked.
3. **Name files, not feelings.** "Explored auth" is useless. "Read
   `AuthFilter.java:88`, confirmed the token is validated twice" is useful.
4. **An open question must name who or what can answer it.**
5. Keep the newest entry at the top.
6. **Keep ten entries here.** The handoff procedure moves an older entry,
   unchanged, to `archive/YYYY.md`. Every session reads this file, so its
   size is a cost that every session pays.

---

<!-- NEW ENTRIES GO DIRECTLY BELOW THIS LINE -->

### Session 7 — 2026-11-25

**Goal:** Run the server mode locally and check that it deploys a new `blog-ready` note.

**Done:**

- Local profile: `.env.local` (git-ignored; source: the private notes repository, target: the Pages repository, random webhook secret, poll 60 s, no start-up deploy, port 8080).
- `scripts/serve-local.sh`: loads `.env.local` (a shell variable wins), token from `gh auth token`, author from `git config`, workspace `~/.cache/mb2e/workspace`, builds the jar when missing.
- `scripts/send-test-webhook.sh`: signed `push` of the newest source commit (`gh api`), or `--ping`.
- `scripts/local-server.env.example`: template with placeholders. `.gitignore`: `.env.local`.
- Check on port 18090 with poll 0: `/healthz` up, `--ping` → `200 pong`, wrong secret → `401`, no token or secret in the logs, nothing deployed.

**Key decisions:**

- GitHub cannot reach localhost: the local test uses the poll or the test webhook script, no tunnel → this entry.

**Output files:**

- `scripts/serve-local.sh`, `scripts/send-test-webhook.sh`, `scripts/local-server.env.example`.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `.env.local` | Local settings; never commit it |

**Dead ends — do not repeat:**

- none

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Run the real test? It publishes to the live site | The owner |
| The local clone of the notes has uncommitted changes; the server builds only from the pushed source branch | The owner, before the first deploy |
| Replace the broad `gh` token (scopes `repo`, `workflow`, ...) with two fine-grained tokens (`MB2E_SOURCE_TOKEN`, `MB2E_TARGET_TOKEN`)? | The owner |
| Commit the scripts to PR #4 (`feature/service-mode`)? | The owner |

**Not done:**

- The real end-to-end test against GitHub (new note → deploy) has not run.
- The scripts, the `.gitignore` line and this entry are not committed; the working copy is on `feature/repo-memory-knowledge-base`. Do not switch to `feature/service-mode` before PR #5 is merged (see Session 6).

### Session 6 — 2026-11-25

**Goal:** Publish the repo-memory knowledge base of mb2e and enable fitting profiles.

**Done:**

- `repo-memory/bootstrap/sync.sh` run: template `2.0.0` already current; only `docs/decisions/README.md` differs (project ADR rows, expected).
- Profiles enabled in `AGENTS.md` section 9: `profiles/github-issues.md` (new), `profiles/container-service.md` (new), `profiles/obsidian.md` (from the template). Board ids moved to `docs/context/ecosystem.md`, section "Backlog and project board".
- Global rule `[R-TEST-03]` (Mockito only at boundaries) in the owner's global agent rules; mb2e exception `[P-TEST-01]` in `docs/qa/project-rules.md`.
- Backlog issue https://github.com/mbero/service-spec-harness/issues/12 (dashboard UI), board status Backlog.
- Sensitive-data scan of the 54 knowledge-base files: no secret, absolute path, e-mail, personal name or operator URL.
- Branch `feature/repo-memory-knowledge-base` from `feature/service-mode`: `ba8b290` (knowledge base, `.gitignore` keeps only `.claude/*` except `.claude/commands/`), `0936c4c` (ADR sources cite code, not the untracked plan). PR https://github.com/mbero/mb2e/pull/5, base `feature/service-mode`; checks `check` and `build` pass.

**Key decisions:**

- PR #5 targets `feature/service-mode` because the documents cite server-mode files → PR #5 description.
- The plan file stays untracked; no document may list it under `sources:` (the CI check fails) → this entry.

**Output files:**

- `profiles/github-issues.md`, `profiles/container-service.md`, `docs/qa/project-rules.md`.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `.gitignore` | On `feature/service-mode` the knowledge base is still ignored; on the new branch it is tracked |
| `.github/workflows/knowledge-base.yml` | Runs `bootstrap/check.sh` on PRs that touch the knowledge base |

**Dead ends — do not repeat:**

- A `sources:` entry that points to an untracked file passes locally and fails in CI. Test with the file moved away.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Merge PR #4, then PR #5? | The owner |
| Upstream the two new profiles to repo-memory? | The owner |
| Move `actions/setup-java` from `@v4` (deprecated) to `@v5` in `.github/workflows/build.yml`? | The owner |

**Not done:**

- **Warning:** a switch from this branch to `feature/service-mode` deletes the tracked knowledge-base files from the working tree, because that branch ignores them and does not track them. Stay on this branch, or merge PR #5 first. The files stay safe in commit `0936c4c`.
- This entry is not committed (handoff procedure, step 11).

### Session 5 — 2026-11-25

**Goal:** Finish the "mb2e as service" plan (Phases 3–5, issues #8–#10), open the merge request, review it, and fix the findings.

**Done:**

- Phase 3 (`121b56f`, `273004c`): `serve` mode — `ServerLauncher`, `ServerEnvironment` (`MB2E_*` allow-list), `ServerConfig`, webhook with HMAC, `DeployQueue`, `/healthz`, `WorkspaceLock`, start-up and poll runs. ADR 0002 accepted.
- Phase 4 (`58929fb`): `Dockerfile`, `.dockerignore`, `docker-compose.yml`, `.env.example`, `.github/workflows/build.yml`, `ServerModeEndToEndTest` (bare repositories, real git, real server).
- Phase 5 (`c753372`): README "Server mode"; ADRs 0003–0005, `docs/knowledge/server-mode.md`, `docs/lessons-learned/server-deployment-playbook.md`, `docs/context/*` corrected.
- PR https://github.com/mbero/mb2e/pull/4 opened (title `feat: add a server mode that deploys the site on a GitHub push`).
- Review of PR #4 (standards, spec, correctness `code-review high`, focused security). Findings in `docs/qa/review-learnings.md`.
- Fixes `9cab47e`: git children lose `MB2E_*` variables (`GitRunner.childEnvironment`); unread git output is a failure; body-limit overflow; deploy cause in the console; escaped commit message in the printed command; `503` while stopping; server ignores `SPRING_APPLICATION_JSON`; compose port follows `MB2E_HTTP_PORT`.
- Refactor `17e4946`: `GitWorkspaceProgram.Role/Remote/Author/Budgets`, `GitRunner.Result.orFail`, `GitTimeoutFailure`, one default per setting (`ServerProperties.DEFAULT_*`), `mb2e.server.source.repository`, `DeployService.Progress`, endpoints routed by `WebRoutes` (`RouterFunction` bean).
- Tests Mockito-free (`72cb956`): fakes everywhere, `ShellContexts` builds real Spring Shell contexts, `pom.xml` excludes `org.mockito`.
- Board: #6–#10 In review, #5 In progress (S01–S20 ticked). New backlog issue #12 "Dashboard UI for the online version".
- 254 tests green, 91.2% instruction coverage (macOS).

**Key decisions:**

- Mockito-free tests in mb2e → `docs/qa/project-rules.md` `[P-TEST-01]`, `docs/context/conventions.md`, `pom.xml`.
- Global rule: Mockito only at architecture boundaries → the owner's global agent rules (mb2e overrides it).
- Spring Shell separation: fallback variant (runners off by property) → ADR 0002.
- Endpoints are plain classes registered by `ServerConfig`; `GET /webhook/github` now answers `404`, not `405` → not recorded as an ADR.
- Kept deviations from the plan: only `[rejected]` pushes are retried; `serve` routed before the argument guard; ADR file names differ from plan §5.1 → this entry.

**Owner decisions (do not ask again):**

- After every phase: all tests, CLI check, tick the issue boxes, board item In review after push, never Done.
- Commits: `type: subject`, no JIRA key in this repository, no `Co-Authored-By` / `Claude-Session`.
- Never commit `AS_A_SERVICE_ARCHITECTURE_PLAN.md`, `GLOSSARY.md`, `src/main/resources/template/mbs-blog/README.md`.
- Save a handoff entry when the context nears 90%.
- Session-only, not carried over: commit and push without asking.

**Output files:**

- `docs/qa/review-learnings.md`, `docs/qa/review-ignore.md`, `docs/qa/project-rules.md` — review register, false alarms, project exceptions.
- `docs/decisions/0003`–`0005`, `docs/knowledge/server-mode.md`, `docs/lessons-learned/server-deployment-playbook.md`.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `ServerEnvironment.java` | The only reader of `MB2E_*`; add a setting here first |
| `infrastructure/config/ServerConfig.java` | Wires the server mode; the only place that creates a `GitWorkspace` |
| `adapter/in/web/WebRoutes.java` | Add a new endpoint here (for example the dashboard, issue #12) |
| `adapter/in/web/DeployQueue.java` | Holds only the newest deployment; a dashboard history needs more |

**Dead ends — do not repeat:**

- `JAVA_TOOL_OPTIONS` in the image prints a notice on stderr of every run; JVM flags are `ENTRYPOINT` arguments.
- Standalone MockMvc ignores a class without `@Controller`; use `MockMvcBuilders.routerFunctions(WebRoutes.of(...))`.
- `ServerEnvironment` refuses `file://` URLs; the e2e test overrides `mb2e.server.*.url` after validation.
- `DeployQueue.destroy()` now drops the waiting run; a test that releases a blocked run must wait for the second run before `destroy()`.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Does the token header work against real GitHub? Tests use `file://` only | A manual run with a real token, or the owner |
| Does the reverse proxy limit slow request bodies (review finding S2)? | The operator's proxy configuration |
| Should the ADR files be renamed to the plan names (§5.1)? | The owner |
| Merge PR #4? | The owner |

**Not done:**

- No Linux Docker test run after the review fixes (last Linux run: 246 tests, before `9cab47e`).
- `docs/` and `AGENTS.md` are git-ignored, so the knowledge base is local only.
- No item in #5 for issue #12.

### Session 4 — 2026-11-25

**Goal:** Implement Phase 2 of issue #7 (S05 git write access, S06 deploy use case).

**Done:**

- S05: `GitWorkspace` port, `GitWorkspaceProgram`, `GitCredentials`, `GitTimeout` (subclass of `GitFailure`, thrown by `GitRunner` on a time-out). Tests run real git against bare repositories (`GitWorkspaceProgramTest`, 14 tests).
- S06: `DeployUseCase`, `DeployService`, `DeployReport`, `StepLog.deploy`; `DeployServiceTest` uses hand-written fakes.
- Full build: 191 tests green on macOS and on `linux/arm64` (git 2.43); coverage 91.1% instructions.
- Commit `7110101` pushed to `origin/feature/service-mode`; issues #5 and #7 ticked; #6 and #7 set to In review, #5 to In progress.

**Key decisions:**

- A push is `REJECTED` only for a `[rejected]` line of `git push --porcelain`; `[remote rejected]` and network errors are `GitFailure` → documented in `git-push(1)`; no ADR.
- `GitWorkspaceProgram` runs git with `GIT_CEILING_DIRECTORIES` set to the clone's parent, so a corrupt clone never touches an outer repository → not recorded as an ADR.

**Output files:**

- `src/main/java/io/github/mbero/mb2e/adapter/out/external/GitWorkspaceProgram.java` — clone, sync, commit, push.
- `src/main/java/io/github/mbero/mb2e/domain/service/DeployService.java` — deploy algorithm.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `adapter/out/external/GitWorkspaceProgram.java` | `ServerConfig` (S09) builds it; the constructor is public |
| `domain/service/DeployService.java` | `DeployQueue` (S11) calls `DeployUseCase.deploy(reason)` |
| `adapter/in/log/StepLog.java` | `deploy(DeployReport)` logs the outcome |

**Dead ends — do not repeat:**

- A quoted shell variable that holds several paths is one path; use `set -- a b c` and `"$@"`.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Does the token header work against real GitHub HTTPS? Tests use `file://` only | Step S17 or a manual test with a real token |
| Is a `GIT_CEILING_DIRECTORIES` test needed (clone inside another repository)? | The owner |

**Not done:**

- No wiring: nothing creates the new classes until `ServerConfig` (S09).
- The two Phase 1 commits keep a `Co-Authored-By` line; the Phase 2 commit has none, by the owner's request.

### Session 3 — 2026-11-25

**Goal:** Implement Phase 1 of issue `mbero/service-spec-harness#6` (steps S01–S04 of `AS_A_SERVICE_ARCHITECTURE_PLAN.md`), prove it on Linux, and raise test coverage to 90%.

**Done:**

- S01: moved `StepLog` and `StepLogTest` to `adapter.in.log`; the bean moved from `ShellConfig` to `BuildConfig`.
- S02: new `adapter/out/external/GitRunner.java` (time budget, process-tree kill, secret masking, `GIT_TERMINAL_PROMPT=0`, `GIT_OPTIONAL_LOCKS=0`); `GitProgram` delegates to it. New `GitRunnerTest`.
- S03: property `mb2e.commit-message` in `Mb2eProperties` and `application.yaml`; `PublishService` takes it as a constructor argument.
- S04: `Mb2eEnvironment` fixes `mb2e.mode=cli`; `ShellConfig` is `@ConditionalOnProperty(mb2e.mode=cli, matchIfMissing)`; new `Mb2eServerModeTest`.
- Wrote `docs/decisions/0002-server-mode-in-the-same-artifact.md` (status `proposed`, fallback variant) and fixed the `StepLog` paths in `docs/context/`.
- Ran the full build in Docker on `linux/arm64` and `linux/amd64`: 141 tests green, `usage` with `{"mb2e.mode":"server"}` exits 0.
- Coverage from 78.7% to 90.6% (instructions), 91.0% (lines), measured with JaCoCo 0.8.14 from the command line. New tests: `ReportCommandsTest`, `JsonTextTest`, `FileSiteSourceTest`, `FilePublishTargetAccessTest`.
- Ticked S01–S04 in issue #5 and all Phase 1 criteria in issue #6.

**Key decisions:**

- `mb2e.mode` is set only by the launcher code (`create` sets `cli`; `forServer` in S08 will set `server`); no JSON key, system property or variable reads it → ADR 0002 (proposed).
- Shell exclusion variant for S04 is the fallback (runners switched off by property); the exclusion variant is decided at S09 → ADR 0002.

**Output files:**

- `src/main/java/io/github/mbero/mb2e/adapter/out/external/GitRunner.java` — process runner for git.
- `docs/decisions/0002-server-mode-in-the-same-artifact.md` — mode decision, proposed.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java` | S08 adds `forServer` here; it must set `mb2e.mode=server` in `mb2e-fixed` |
| `src/main/java/io/github/mbero/mb2e/adapter/out/external/GitRunner.java` | S05 `GitWorkspaceProgram` reuses it with `extraEnvironment` and `secrets` |
| `src/main/java/io/github/mbero/mb2e/infrastructure/config/ShellConfig.java` | Conditional on `mb2e.mode`; `ServerConfig` (S09) is the counterpart |

**Dead ends — do not repeat:**

- BSD `sed` on macOS does not support `0,/re/`; edit multi-line with `python3`.
- `gh issue edit` fails now and then with a GraphQL error; wait and retry. `gh api -X PATCH` failed with "unexpected end of JSON input" here.
- A plain `mvn ... | grep` hides the Maven exit code; read it with `echo $?` before the pipe.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Seven open questions of the plan (vault root, target name, one or two tokens, TLS proxy, notifications, commit id in the public body) | The owner |
| Exclusion or fallback for Spring Shell in the server context | Step S09; then set ADR 0002 to `accepted` |
| Is a commit message with a double quote safe in the printed `commit -m "..."` command? | A decision of the owner; `PublishService.commands` does not escape it |

**Not done:**

- No commit and no push.
- No new `PublishServiceTest` case for a custom commit message; the jar run proves it.
- Phase 2 (S05, S06) not started.
- Coverage is measured by a command, not by a pom plugin; the project has no JaCoCo gate.

### Session 2 — 2026-11-25

**Goal:** Turn the specification of Session 1 into trackable work on GitHub: expand issue 5 and split it into sub-issues. No source code change.

**Done:**

- Gave the `gh` token the `project` scope with `gh auth refresh -h github.com -s project`. The command needs `-h github.com` in a non-interactive shell. `gh project view 2 --owner mbero` then returned project "mb2e - markdown based blog engine".
- Expanded the body of issue `mbero/service-spec-harness#5` ("mb2e as service") from `AS_A_SERVICE_ARCHITECTURE_PLAN.md`. The original three sentences stay at the top.
- Created five sub-issues of #5, one for each phase of section 8 of the plan, and linked them with `POST repos/mbero/service-spec-harness/issues/5/sub_issues`.
- Confirmed that project 2 holds items for issues 1, 2, 4, 5 and 6–10.

**Key decisions:**

- Sub-issues by phase, not by step (5 instead of 20), in the same repository as #5 → this entry only; no ADR.

**Output files:**

- GitHub issue #5 — expanded description.
- GitHub issues #6–#10 — Phase 1 to Phase 5, each with its steps and acceptance criteria as a checklist.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `AS_A_SERVICE_ARCHITECTURE_PLAN.md` | Source of every issue body; still untracked in git |
| `docs/sessions/history.md` | Session 1 holds the design decisions that the issues summarise |

**Dead ends — do not repeat:**

- `gh auth refresh -s project` fails with "--hostname required" inside the agent shell. Add `-h github.com`, and run it in the user terminal because it needs a browser.
- `gh project item-add` for #7 and #9 failed with "Content already exists". The project adds new issues by itself, probably through an auto-add workflow. Do not add them again.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| The seven open questions of the plan (vault root, target repository name, one or two tokens, TLS proxy, failure notification, source commit id in the public commit body) | The owner; answers belong in the plan section 9 and in issue #5 |
| Is the issues repository `mbero/service-spec-harness` correct, while the code lives in `mbero/mb2e`? | The owner |
| Which project fields (status, iteration) should the new issues carry? | The owner; project 2 has 18 fields |

**Not done:**

- Did not set any project field on the new items.
- Did not add labels; the issues have none.
- Did not start step S01 of the plan.
- Did not commit. `AS_A_SERVICE_ARCHITECTURE_PLAN.md` and `src/main/resources/template/mbs-blog/README.md` stay untracked, and this file stays modified in the working tree.

### Session 1 — 2026-11-16

**Goal:** Analyse the code and design a second operating mode, "As a Service": a server that listens for pushes to a Source Repository (Markdown notes), builds the site, and pushes it to a Target Repository (GitHub Pages). Analysis only; no feature, no change of source code.

**Done:**

- Read the build, the entry point, the configuration, all ports, `PipelineService`, `PublishService`, `BuildService`, `GitProgram`, the file adapters, and the shell adapters under `src/main/java/io/github/mbero/mb2e/`.
- Wrote the specification `AS_A_SERVICE_ARCHITECTURE_PLAN.md` (repository root, untracked): 10 decisions (AD-01 to AD-11), component design, variable schema, file manifest, 32 edge cases, 20 steps (S01–S20) with acceptance criteria, 7 open questions.
- Checked with commands: tags `eclipse-temurin:25-jre` and `maven:3.9-eclipse-temurin-25` exist; the HMAC test vector and the base64 value in the plan are correct.

**Key decisions:**

- One jar, two modes; `serve` as the first argument starts the server → plan section AD-01; no ADR file yet.
- Keep `GitRepository` read-only; add a separate port `GitWorkspace` that only the server wiring creates → AD-03; no ADR file yet.
- Authentication with two fine-grained tokens, passed through the git process environment, never in a URL → AD-05; no ADR file yet.
- One worker thread plus one waiting slot to coalesce webhook bursts → AD-07; no ADR file yet.
- The diff decision comes from `git status` of the target after the build, not from changed source paths → AD-08; no ADR file yet.

**Output files:**

- `AS_A_SERVICE_ARCHITECTURE_PLAN.md` — the full specification; input for the coding session. Untracked; the owner decides when it becomes a commit.

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| `src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java:35-37` | Removes the OS environment on purpose; the server mode needs an allow-list of `MB2E_*` variables |
| `src/main/java/io/github/mbero/mb2e/adapter/out/persistence/FileSiteSource.java:13-17` | Reads the template from disk only; the image must carry the template as a directory |
| `src/main/java/io/github/mbero/mb2e/domain/service/PublishService.java:41`, `:166` | Private commit message constant; origin URL printed in a refusal (a token in a URL would leak) |
| `src/main/java/io/github/mbero/mb2e/adapter/in/shell/StepLog.java` | Must move to `adapter.in.log` before a web adapter can use it |
| `src/main/java/io/github/mbero/mb2e/domain/service/RemoteGuard.java:15-18` | Accepts only `<owner>/<owner>.github.io`; other targets need `MB2E_FORCE=true` |

**Dead ends — do not repeat:**

- A filter on changed `.md` paths to skip a build: rejected, because a tag removal, an image change, or a new template changes the site without a changed `.md` path.
- A mode variable such as `MB2E_MODE=server`: rejected, because `docker run <image> run --dry-run` must still reach the CLI.
- Spring Boot Actuator for the health check: rejected, because `Mb2eEnvironment` blocks management properties and one endpoint needs no dependency.

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| Is the vault the root of the Source Repository or a subdirectory? | The owner |
| Is the Target Repository `<owner>/<owner>.github.io`? | The owner |
| Two tokens or one token for both repositories? | The owner |
| Is a failure notification needed (commit status, e-mail)? | The owner |
| Facts marked `[UNVERIFIED]` in the plan (GitHub limits, token permissions, git minimum versions, `ImportCandidates` API) | GitHub documentation, `git --version` in the image, Spring Boot 3.5.16 source |
| Do repeated `PipelineUseCase.run` calls in one JVM equal separate processes? | Step S17 of the plan (end-to-end test) |

**Not done:**

- No code written, by design of the request.
- Did not read `MarkdownRenderer`, `NoteIndex`, `FrontMatter`, `SiteDataJson`, `Selection`, `BuildCommand`, and most test classes; the plan section 10 lists them.
- Did not start the application and ran no test.
- Did not write the ADRs, `docs/knowledge/server-mode.md`, or a `docs/todo/` file; they belong to step S20 of the plan.

## Session template — copy this block

### Session N — YYYY-MM-DD

**Goal:** <!-- what the session set out to do -->

**Done:**

- <!-- outcome, with the file it touched -->

**Key decisions:**

- <!-- decision → where it is recorded (ADR number, or "not recorded yet") -->

**Output files:**

- `<!-- path -->` — <!-- what it contains -->

**Key files and classes for later work:**

| Path | Why it matters |
|---|---|
| | |

**Dead ends — do not repeat:**

- <!-- what was tried, why it failed -->

**Open questions:**

| Question | Who or what can answer it |
|---|---|
| | |

**Not done:**

- <!-- what was in scope and got left out, and why -->
