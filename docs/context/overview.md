---
title: Project overview
type: context
status: draft
updated: 2026-11-25
sources:
  - README.md
  - pom.xml
  - src/main/resources/application.yaml
  - src/main/java/io/github/mbero/mb2e/Mb2eApplication.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/ShellConfig.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/BuildConfig.java
tags: [overview]
---

# Project overview — mb2e

## Purpose

mb2e is a command-line tool. It selects the tagged notes of an Obsidian vault,
renders them to HTML, and mirrors a static site into a local clone of a GitHub
Pages repository (`README.md:8`, `README.md:12-14`).

The user is one person who writes notes and owns the Pages repository. The tool
needs no server, no database, and no CMS (`README.md:8`). When the tool
stops, the published site stays online but receives no new content.

## Scope

**In scope:**

- Select the notes that carry the selection tag (`README.md:30`).
- Render Markdown to HTML and generate `assets/js/data.js` (`README.md:14`).
- Mirror the site into the working tree of the Pages repository (`README.md:184-190`).
- Print the git commands that publish the change (`README.md:167`).

**Out of scope** (and who owns it instead):

- `git add`, `git commit`, `git push` → the user in the CLI (`README.md`, section "Deploying to GitHub Pages"); in the server mode, `DeployService` commits and pushes.
- Hosting and deployment → GitHub Pages (`README.md:332`).
- Note editing → Obsidian. mb2e only reads the vault (`README.md:359`).
- Design of the site → the site template. mb2e only reads it (`README.md:73`).

## Module map

One Maven module, at the repository root. Package root: `io.github.mbero.mb2e`.

| Module / package | Responsibility | Entry point |
|---|---|---|
| root package | Start the process, build the configuration environment, map every failure to exit code 1 | `Mb2eApplication.java:23` |
| `adapter.in.shell` | Shell commands `build`, `publish`, `run`, `usage`, `help`, `exit`; reports; refusals; step log | `ShellConfig.java:31-32` |
| `adapter.out.persistence` | File access to the vault, the template, and the publication target | `BuildConfig.java:43-62` |
| `adapter.out.external` | Three read-only calls of the `git` program | `GitProgram.java:86` |
| `domain.model` | Records for notes, the assembled site, and the reports | — |
| `domain.port.in` | Use case interfaces `BuildUseCase`, `PublishUseCase`, `PipelineUseCase` | — |
| `domain.port.out` | Interfaces `VaultReader`, `SiteSource`, `PublishTarget`, `GitRepository` | — |
| `domain.service` | Selection, Markdown rendering, site assembly, publication plan, guards | `BuildService.java:78`, `PublishService.java:72`, `PipelineService.java:29` |
| `infrastructure.config` | Spring wiring and the `mb2e.*` properties | `BuildConfig.java`, `ShellConfig.java`, `Mb2eProperties.java` |

Paths in this table are below `src/main/java/io/github/mbero/mb2e/`.

## Technology stack

Versions are true on 2026-11-13.

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 25 (`pom.xml:20`) |
| Framework | Spring Boot | 3.5.16 (`pom.xml:10`) |
| Framework | Spring Shell | 3.4.3 (`pom.xml:21`) |
| Build | Maven, no wrapper script | 3.9+ (`README.md:71`) |
| Database | none | |
| Messaging | none | |
| Test | `spring-boot-starter-test` (JUnit 5, AssertJ) | managed by the Spring Boot parent (`pom.xml:42-46`) |
| Site template | HTML, CSS, JavaScript without a build step | `src/main/resources/template/mbs-blog/` |

## External integrations

The CLI calls no network service: every integration is a local folder or a local program. The server mode (`mb2e serve`) receives a GitHub webhook, and its `git` child process reaches GitHub over HTTPS to clone and push.

| System | Direction | Protocol | Purpose | Owner |
|---|---|---|---|---|
| Obsidian vault | in | file read | Source of the notes and the images | the user |
| Site template | in | file read | `index.html` and `assets/` of the site | the user |
| Local clone of the Pages repository | out | file write and delete | Receives the site | the user |
| `git` program | in | child process, read only in the CLI; clones that push in the server mode | Origin URL, branch, status of the target (`GitProgram.java:16`) | the user |
| GitHub webhook (server mode) | in | HTTPS through a reverse proxy, then HTTP `POST /webhook/github` | Push event that starts a deploy (`GithubWebhookController.java`) | GitHub, the operator |
| GitHub source and target repositories (server mode) | in and out | HTTPS by the `git` program | Shallow clone of the notes; commit and push of the site (`GitWorkspaceProgram.java`) | the operator |

## Environments

The CLI runs on the machine of the user. The server mode runs in a container: the repository holds `Dockerfile`, `docker-compose.yml` and `.env.example`.

| Environment | URL / host | Purpose | Who deploys |
|---|---|---|---|
| local | the machine of the user | Build the jar and run the commands | the user |
| test | none found | | |
| staging | none found | | |
| production | none found. The output site runs on GitHub Pages after the user pushes. | | the user, with `git push` |

## Repository layout

```text
.
├── pom.xml                               Maven build
├── README.md                             user guide
├── application_local_paths.example.yaml  template for the three local paths
├── src/main/java                         the application
├── src/main/resources                    application.yaml, logback.xml, banner, template/mbs-blog
├── src/test/java                         the tests
├── AGENTS.md                             canonical agent instructions; CLAUDE.md points to it
├── .agents/                              tool-neutral procedures
├── .claude/commands/                     slash commands for the procedures
├── .github/workflows/                    knowledge-base check
├── bootstrap/                            check.sh, the knowledge-base check
├── docs/                                 the knowledge base
└── profiles/                             optional rule profiles
```

## What could not be verified

- **Business purpose beyond the README.** The README states what the tool does. On 2026-11-13 the owner could not state more.
- **Environments.** No CI file and no deployment file exist for the code. The rows "test" and "staging" stay empty.
- **Specification.** A separate working copy, `service-spec-harness`, holds a specification of mb2e. See [`ecosystem.md`](ecosystem.md).
- **Runtime behaviour.** The agent read the code and did not start the application.
