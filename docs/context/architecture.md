---
title: Architecture
type: context
status: draft
updated: 2026-11-25
sources:
  - README.md
  - src/main/java/io/github/mbero/mb2e/Mb2eApplication.java
  - src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/BuildConfig.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/ShellConfig.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/shell/RunCommand.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/log/StepLog.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/shell/RefusalReporter.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitProgram.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitRunner.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitWorkspaceProgram.java
  - src/main/java/io/github/mbero/mb2e/domain/service/DeployService.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/persistence/FilePublishTarget.java
  - src/main/java/io/github/mbero/mb2e/domain/service/BuildService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PublishService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PipelineService.java
  - src/main/resources/application.yaml
  - src/main/resources/logback.xml
tags: [architecture]
---

# Architecture — mb2e

> Record a **choice** in `docs/decisions/`, not here. This file describes the
> result, not the reasoning.

Java paths in this file are below `src/main/java/io/github/mbero/mb2e/`.

## Style

mb2e has a hexagonal architecture (`README.md:377`). The shell commands call
use case interfaces in `domain.port.in`. The domain reaches the file system and
git only through interfaces in `domain.port.out`. The domain services are plain
Java classes with no Spring annotation. `BuildConfig` creates them as beans
(`infrastructure/config/BuildConfig.java:52-78`).

mb2e is one jar with two modes. The CLI is a one-shot program: it starts no web server and keeps no state between two runs.
`mb2e serve` starts a web server in a long-lived process (`ServerLauncher.java`). Only the launcher sets `mb2e.mode`
(`Mb2eEnvironment.java`), and `SPRING_APPLICATION_JSON` cannot change it. Decisions: `docs/decisions/0002` to `0005`.

## Boundaries

| Boundary | What crosses it | Contract | Where it is defined |
|---|---|---|---|
| User → shell | Command words and the options `--json`, `--dry-run`, `--no-publish` | Spring Shell commands | `adapter/in/shell/BuildCommand.java:25`, `RunCommand.java:25`, `UsageCommand.java:14-32` |
| Shell → domain | A dry-run flag in, a report record out | Java interfaces | `domain/port/in/BuildUseCase.java`, `PublishUseCase.java`, `PipelineUseCase.java` |
| Domain → vault | Note bytes and attachment paths | Java interface, read only | `domain/port/out/VaultReader.java` |
| Domain → template | Template files | Java interface, read only | `domain/port/out/SiteSource.java` |
| Domain → publication target | File writes and deletes | Java interface | `domain/port/out/PublishTarget.java` |
| Domain → git | Origin URL, branch, status | Java interface, read only | `domain/port/out/GitRepository.java` |
| mb2e → site template script | `window.BLOG_DATA = { defaultNote, TREE, NOTES }` | Generated file `assets/js/data.js` | `README.md:238-261`, `domain/model/SiteData.java:6-7` |
| Process → caller | Report on standard output, refusal sentence on standard error, exit code 0 or 1 | Text or one JSON object | `Mb2eApplication.java:20-21`, `adapter/in/shell/RefusalReporter.java:13` |

### Server-mode boundaries

| Boundary | What crosses it | Where it is defined |
|---|---|---|
| GitHub → webhook | A signed push event over HTTP | `adapter/in/web/GithubWebhookController.java` |
| Web → domain | A deploy reason in, a `DeployReport` out | `domain/port/in/DeployUseCase.java`, `adapter/in/web/DeployQueue.java` |
| Domain → git workspace | Sync, commit, push of two shallow clones | `domain/port/out/GitWorkspace.java`, `adapter/out/external/GitWorkspaceProgram.java` |
| Environment → process | The `MB2E_*` allow-list | `ServerEnvironment.java` |

## Layers

| Layer | Package | May depend on | Must never depend on |
|---|---|---|---|
| Inbound adapter | `adapter.in.shell`, `adapter.in.web`, `adapter.in.log` | `domain.port.in`, `domain.model`, Spring Shell | `adapter.out`, `domain.service` |
| Domain services | `domain.service` | `domain.model`, `domain.port.in`, `domain.port.out`, the JDK | Spring, `adapter`, `infrastructure` |
| Domain model and ports | `domain.model`, `domain.port` | the JDK | Spring, `adapter`, `infrastructure` |
| Outbound adapters | `adapter.out.persistence`, `adapter.out.external` | `domain.port.out`, `domain.model`, the JDK | `adapter.in`, `domain.service` |
| Infrastructure | `infrastructure.config`, root package | all layers, Spring | — |

On 2026-11-13 a `grep` over the imports of `src/main/java` found no
violation of the column "Must never depend on". No automated check enforces it.

## Primary data flow

The command `run`:

```text
java -jar mb2e.jar run
  │
  ├─ Mb2eApplication.run            argument check, environment, Spring start   (Mb2eApplication.java:32-61)
  ├─ OneShotRunner                  each process argument is one word           (adapter/in/shell/OneShotRunner.java:31)
  ├─ RunCommand.run                 refuses stray words, calls the use case     (adapter/in/shell/RunCommand.java:30-31)
  └─ PipelineService.run                                                        (domain/service/PipelineService.java:29)
       ├─ BuildService.assemble                                                 (domain/service/BuildService.java:91)
       │    ├─ check the configured paths and the template shape   (:92-100)
       │    ├─ VaultReader.read            → FileVaultReader       (:101)
       │    ├─ select the tagged notes                             (:111)
       │    ├─ pass 1: front matter and titles                     (:120-125)
       │    ├─ pass 2: Markdown → one HTML block for each note     (:130-147)
       │    └─ data.js text + image list, in memory                (:166-171)
       └─ PublishService.publish(built)                                         (domain/service/PublishService.java:82)
            ├─ guards: target, overlap, git working copy, origin   (:128-176)
            ├─ GitRepository.branch, status → GitProgram           (:93-94)
            ├─ plan every write and delete before the first write  (:188-231)
            ├─ apply the plan, unless dry run → FilePublishTarget  (:109, :282-289)
            └─ report with the three git commands                  (:311-316)
```

`build` runs only `BuildService`. `publish` runs `BuildService`, then `PublishService` (`PublishService.java:72-79`).

## Persistence

The CLI has no database and keeps nothing between two runs. The server mode keeps one SQLite file,
`<workspace>/history.db`, with the newest 500 deployments (`SqliteDeploymentHistory`, schema `src/main/resources/db/deployments.sql`).

| Store | What it holds | Owner | Migration tool |
|---|---|---|---|
| Vault folder | Markdown notes and attachments; read only | the user | none |
| Template folder | `index.html`, `assets/`; read only | the user | none |
| Publication target folder | The mirrored site; the only place that the CLI writes | the user | none |
| `<workspace>/history.db` (server mode) | Deployment history for the dashboard API; a write failure never fails a deployment | mb2e | `CREATE TABLE IF NOT EXISTS` on first use |
| Optional log file (`mb2e.log.file`) | Step lines and stack traces, appended | the user | none |

## Cross-cutting concerns

| Concern | How it is handled | Where |
|---|---|---|
| Error handling | A use case returns a report with the outcome `refused`. It does not throw. The command prints the report and throws `ShellRefusal`. Each failure gives exit code 1. | `domain/port/in/BuildUseCase.java:9`, `adapter/in/shell/BuildCommand.java:32-36`, `Mb2eApplication.java:62-69` |
| Logging | Logback sends all log output to standard error. The logger `mb2e.step` writes one line for each step. | `src/main/resources/logback.xml:4-11`, `adapter/in/log/StepLog.java:28` |
| Tracing / correlation id | Each report carries a random UUID as trace id. `run` gives one id to both steps. | `domain/service/BuildService.java:79`, `domain/service/PipelineService.java:30-39` |
| Authentication | CLI: none; it calls no network service and never runs `git push`. Server: the webhook HMAC-SHA256 signature in, a GitHub token to git out, passed only in the git child environment. | `adapter/out/external/GitProgram.java:16`, `adapter/in/web/WebhookSignature.java`, `adapter/out/external/GitCredentials.java` |
| Configuration | Server: only the `MB2E_*` allow-list (`ServerEnvironment.java`). CLI: three sources, in rising precedence: packaged `application.yaml`, the local paths file, the `mb2e.*` keys of `SPRING_APPLICATION_JSON`. System properties, other OS variables, profiles, and command-line arguments cannot set a property. | `Mb2eEnvironment.java:12-14`, `Mb2eEnvironment.java:36`, `Mb2eApplication.java:55` |
| Feature flags | None. The settings `mb2e.inline-tags` and `mb2e.force` are plain configuration switches. | `src/main/resources/application.yaml:28-32` |
| External process budget | Each git call has a budget of 5000 ms and no retry. | `adapter/out/external/GitProgram.java:23`; the process, kill and masking code is in `adapter/out/external/GitRunner.java` |

## Known architectural debt

| # | Debt | Impact | Recorded in |
|---|---|---|---|
| 1 | The README names the template folder `html_template/mbs-blog` (`README.md:73`, `:133`). The repository ships it at `src/main/resources/template/mbs-blog` (`README.md:198`). | A new user cannot find the folder that the first reference names. | not recorded yet |
| 2 | No automated check enforces the layer rules. | A wrong import stays unnoticed. | not recorded yet |
| 3 | Publish still deletes a top-level folder `datasource` (`domain/service/PublishService.java:39`, `:224-226`). The owner confirmed on 2026-11-13 that this cleanup is not necessary. | Dead code in the publish step and in the file adapter. | not recorded yet |

## What could not be verified

- The agent did not read `MarkdownRenderer`, `NoteIndex`, `FrontMatter`, `Js`, `SiteDataJson`, `PolishOrder`, `ReportJson`, `PublishCommand`, `HelpCommand`, `ExitCommand`, `FileSiteSource`, and `FileTree`. Statements about rendering come from the README.
- The agent read only the first 60 lines of `FilePublishTarget` and the lines that refuse a delete.
- The code follows the layer table today. A person must confirm that it is the intended rule.
- The agent did not find a decision record for the hexagonal style. `docs/decisions/` holds only decision 0001.

## Server-mode building blocks (not wired yet)

These classes exist and are tested, but no configuration creates them, so the
CLI cannot push. The planned `ServerConfig` will create them.

| Class | Role |
|---|---|
| `domain/port/out/GitWorkspace.java` | Port: sync, hasChanges, commitAll, push for one clone. |
| `adapter/out/external/GitWorkspaceProgram.java` | Adapter: a shallow clone driven by the git program; refuses any directory not named `source` or `target`. |
| `adapter/out/external/GitCredentials.java` | Token as a git environment header; never in a URL or an argument. |
| `adapter/out/external/GitRunner.java` | Shared process runner: budget, process-tree kill, secret masking. |
| `domain/port/in/DeployUseCase.java`, `domain/service/DeployService.java` | Sync both clones, run the pipeline, commit and push when the site changed; at most three attempts; never throws. |
| `domain/model/DeployReport.java` | Outcome `deployed`, `up_to_date`, `refused` or `failed`. |
