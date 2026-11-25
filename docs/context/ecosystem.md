---
title: Ecosystem context
type: context
status: draft
updated: 2026-11-25
sources:
  - README.md
  - src/main/java/io/github/mbero/mb2e/domain/service/RemoteGuard.java
  - src/main/java/io/github/mbero/mb2e/domain/service/TemplateCheck.java
  - src/main/java/io/github/mbero/mb2e/domain/model/SiteData.java
  - src/main/resources/template/mbs-blog/assets/js/app.js
tags: [ecosystem, integration]
---

# Ecosystem context — mb2e

> **Delete this file if the project is isolated.** Keep it only when a question
> about this project regularly needs an answer from another repository.

This file tells an agent when the answer to a question is not in this
repository. mb2e calls no network service, but it depends on three things
outside this repository: the Pages repository that it writes into, the site
templates that read its data file, and the specification in `service-spec-harness`.
Without this file, an agent searches this repository for an answer that is not here.

## Related repositories

| Short name | Repository | Role in the ecosystem |
|---|---|---|
| **mb2e** | https://github.com/mbero/mb2e | **This project.** Markdown based blog engine |
| Pages repository | `https://github.com/<owner>/<owner>.github.io` | Receives the site. GitHub Pages serves it. mb2e writes into its local clone (`README.md:338-342`). |
| Obsidian vault | a local folder; no repository is necessary | Source of the notes. mb2e only reads it. |
| service-spec-harness | a separate local working copy, next to the clone of mb2e; its repository URL is not verified | Holds the specification and the acceptance cases of mb2e. The owner wants it to be the binding specification (2026-11-13). |

> Use the repository URL, not a local path. A local path breaks on every other
> machine.

## Backlog and project board

| Item | Value |
|---|---|
| Issue repository | https://github.com/mbero/service-spec-harness (issues #5–#10: "mb2e as service"; #12: dashboard UI) |
| Project board | GitHub project 2 of the owner `mbero`; its id: `gh project view 2 --owner mbero --format json --jq .id` |
| Status field | `PVTSSF_lAHOANUm6M4Bqhdbzho4PcE` |
| Status options | Backlog `f75ad846`, Ready `61e4505c`, In progress `47fc9ee4`, In review `df73e18b`, Done `98236657` |

Set a status: `gh project item-edit --id <item> --project-id <id> --field-id <field> --single-select-option-id <option>`.
Find an item id: `gh project item-list 2 --owner mbero --format json`. Rules: `profiles/github-issues.md`.

## When to look outside this repository

| The question is about | Look in | Start at |
|---|---|---|
| The content that is online now | Pages repository | `assets/js/data.js` |
| A file in the Pages repository that mb2e did not write | Pages repository | its git history; mb2e leaves `README.md`, `CNAME`, and `.github/` alone (`README.md:192`) |
| Why a note is not on the site | Obsidian vault | the front matter of the note |
| The deployment of the site | GitHub | Settings → Pages of the Pages repository (`README.md:340-342`) |
| The written specification of a behaviour | service-spec-harness | `specification/` and `acceptance/` in that working copy |

## Shared contracts

| Contract | Owner | Consumers | Where it is defined | Breaking-change policy |
|---|---|---|---|---|
| The data object `window.BLOG_DATA` (`defaultNote`, `TREE`, `NOTES`) | mb2e | The script `app.js` of each site template | `README.md:238-261`, `domain/model/SiteData.java` | not defined |
| The template contract: `index.html`, `assets/js/app.js` with the text `case 'html'`, an empty `assets/img/notes/` | mb2e | Each author of a template | `README.md:213-218`, `domain/service/TemplateCheck.java` | not defined |
| The HTML of a rendered note and its CSS classes | mb2e | The stylesheet of each template | `README.md:272-279` | not defined |
| The JSON report of `--json` | mb2e | Scripts of the user | `adapter/in/shell/ReportJson.java` | not defined |
| The name of the Pages repository, `<owner>.github.io` | GitHub | mb2e | `domain/service/RemoteGuard.java` | set by GitHub |

Java paths in this table are below `src/main/java/io/github/mbero/mb2e/`.

**A contract change is a cross-repository change.** A change of the data object
breaks each template that a user copied. Find every consumer before you change
a field.

## What could not be verified

- **service-spec-harness.** Its git remote names `github.com/mbero/mb2e`, the same URL as this repository, but its commits differ. The agent did not find its true repository. The agent read only the first 40 lines of its README and the first 30 lines of its `service.yaml`.
- **Agreement with the specification.** Nobody knows if the code agrees with the harness today. The owner is not sure.
- **Status of the harness specification.** It is not known if the code must still agree with it.
- **The JSON report.** The agent did not read `ReportJson`. The row states only that the file exists.
- **Other templates.** The agent knows only the shipped template `mbs-blog`. It is not known if other consumers of the data object exist.
- **The Pages repository of the owner.** The agent did not read it.
