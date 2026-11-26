---
title: Business domain
type: context
status: draft
updated: 2026-11-13
sources:
  - README.md
  - src/main/java/io/github/mbero/mb2e/Mb2eApplication.java
  - src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java
  - src/main/java/io/github/mbero/mb2e/domain/service/BuildService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PublishService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PipelineService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/Selection.java
  - src/main/java/io/github/mbero/mb2e/domain/service/RemoteGuard.java
  - src/main/java/io/github/mbero/mb2e/domain/service/TemplateCheck.java
  - src/main/java/io/github/mbero/mb2e/domain/port/in/BuildUseCase.java
  - src/main/java/io/github/mbero/mb2e/domain/port/out/PublishTarget.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/persistence/FileVaultReader.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/persistence/FilePublishTarget.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitProgram.java
tags: [domain, business]
---

# Business domain — mb2e

> The business truth of the project, in business words. Name no class here.
> A reader who never saw the code must understand this file.

The column "Source" names files, so that a person can check each rule. Paths in
that column are below `src/main/java/io/github/mbero/mb2e/`. For the terms,
see [`glossary.md`](glossary.md).

## What the business needs from this project

A person keeps notes in Obsidian and wants to publish some of them as a web
site. The person marks a note with a tag. The tool puts the marked notes on a
site in the person's GitHub Pages repository. The person keeps the last
decision: the tool prepares the change, and the person pushes it
(`README.md:8`, `README.md:316`).

When the project stops, the site stays online with its last content.

## Actors

| Actor | Goal | How the actor reaches the project |
|---|---|---|
| Author | Publish selected notes; keep the other notes private | Tags in the notes; the commands `build`, `publish`, `run` in a terminal |
| Script of the author | Read the result of a command | The option `--json` and the exit code |
| Reader of the site | Read the published notes | The web site on GitHub Pages; never the tool |
| Template designer | Control the look of the site | The site template folder |

The author and the template designer can be the same person.

## Business processes

| Process | Trigger | Outcome | Detail |
|---|---|---|---|
| Check what the tool selects | The author runs `build` | A report shows the selected notes, the images, the unresolved links, and the warnings. Nothing changes on disk. | `README.md:295-299` |
| Preview a publication | The author runs `run --dry-run` | A report shows the files that a publication changes and deletes. Nothing changes on disk. | `README.md:300-303` |
| Publish | The author runs `run` or `publish` | The local Pages repository holds the site. The report lists three git commands. | `README.md:184-192` |
| Deploy | The author runs the three git commands | GitHub Pages shows the new site. | `README.md:344-355` |
| Withdraw a note | The author removes the tag and publishes | The note and its images leave the site. | `README.md:362` |

No document in `docs/knowledge/` describes these processes yet.

## Business rules

| ID | Rule | Confidence | Source | Confirmed by |
|---|---|---|---|---|
| BR-001 | The tool publishes a note only when the note carries the selection tag in its front matter. | inferred | `domain/service/Selection.java:32-34` | |
| BR-002 | A tag in the body of a note selects the note only when the author switches inline tags on. | inferred | `domain/service/Selection.java:33` | |
| BR-003 | The tool compares tags without letter case, quotes, and a leading hash sign. | inferred | `domain/service/Selection.java:26-29` | |
| BR-004 | The tool refuses the build when no note carries the selection tag. | inferred | `domain/service/BuildService.java:112-115` | |
| BR-005 | A folder appears on the site only when a selected note lies in it or below it. | inferred | `domain/service/BuildService.java:195-209` | |
| BR-006 | The title of an article is the title in the front matter. Without one, it is the file name. | inferred | `domain/service/BuildService.java:262-267` | |
| BR-007 | The tool removes the first heading of a note when it repeats the title. | inferred | `domain/service/BuildService.java:134-137` | |
| BR-008 | The selected note `about` at the root of the vault is the About page. The tree does not show it. | inferred | `domain/service/BuildService.java:242-246` | |
| BR-009 | A missing About page gives a warning, not a refusal. | inferred | `domain/service/BuildService.java:153-156` | |
| BR-010 | The site opens on the configured start note. When that note is not published, the site opens on the first note of the tree, and the tool gives a warning. | inferred | `domain/service/BuildService.java:157-164` | |
| BR-011 | The build never writes a file. | inferred | `domain/port/in/BuildUseCase.java:6` | |
| BR-012 | The tool never writes to the vault and never writes to the template. | inferred | `domain/model/ConfiguredPaths.java:7` | |
| BR-013 | The scan of the vault ignores each name that starts with a dot, the folder `node_modules`, and each symbolic link. | inferred | `adapter/out/persistence/FileVaultReader.java:56-63` | |
| BR-014 | The template must hold `index.html` and `assets/js/app.js`. The script must contain the text `case 'html'`. The folder `assets/img/notes` must be empty or absent. | inferred | `domain/service/TemplateCheck.java:28-44` | |
| BR-015 | The publication target must be a git working copy with a remote `origin`. | inferred | `domain/service/PublishService.java:152-164` | |
| BR-016 | The origin must be `<owner>/<owner>.github.io` on GitHub, in the HTTPS form or the SSH form. | inferred | `domain/service/RemoteGuard.java:14-31` | |
| BR-017 | The setting `force` changes a wrong origin from a refusal to a warning. | inferred | `domain/service/PublishService.java:165-174` | |
| BR-018 | The tool refuses a target that is the file system root, or that contains, equals, or lies in the vault or the template. | inferred | `domain/service/PublishService.java:135-142` | |
| BR-019 | Uncommitted changes in the target give a warning, not a refusal. | inferred | `domain/service/PublishService.java:95-99` | |
| BR-020 | The tool completes each check and the full plan before it writes the first file. | inferred | `domain/service/PublishService.java:32-33` | |
| BR-021 | A publication deletes files only below `assets/` and in a top-level folder `datasource`. The owner states that the `datasource` cleanup is no longer necessary; the code still does it. | inferred | `adapter/out/persistence/FilePublishTarget.java:146-147`, `:216-217` | owner, 2026-11-13: the `datasource` part is obsolete |
| BR-022 | A publication never copies a Markdown file, a `.DS_Store` file, or the sample `data.js` of the template. | inferred | `domain/service/PublishService.java:192-196` | |
| BR-023 | A publication never writes through a symbolic link. | inferred | `domain/port/out/PublishTarget.java:29-32` | |
| BR-024 | A file whose name differs from a site file only in letter case or Unicode form is written again, not deleted. | inferred | `domain/service/PublishService.java:233-247` | |
| BR-025 | The tool never runs `git add`, `git commit`, or `git push`. It prints these commands. | inferred | `adapter/out/external/GitProgram.java:16`, `domain/service/PublishService.java:311-316` | |
| BR-026 | A run stops after a refused build. It does not publish. | inferred | `domain/service/PipelineService.java:35` | |
| BR-027 | The exit code is 0 for success and 1 for each refusal. No other code exists. | inferred | `Mb2eApplication.java:20-21`, `:28-30` | |
| BR-028 | A command-line argument never changes the configuration. | inferred | `Mb2eApplication.java:53-55` | |
| BR-029 | The variable `SPRING_APPLICATION_JSON` accepts only keys below `mb2e.`. | inferred | `Mb2eEnvironment.java:58-61` | |
| BR-030 | Each wait for the git program has a limit of 5 seconds, and the tool does not try again. | inferred | `adapter/out/external/GitProgram.java:18`, `:23` | |

> **`inferred`** means an agent read the rule out of the code. A business owner
> did not confirm it. Change it to `confirmed` only when a person confirms it.

## Constraints from outside the code

| Constraint | Origin | Effect on the project |
|---|---|---|
| A user site on GitHub Pages lives in a repository with the name `<owner>.github.io`. | GitHub | The origin check of BR-016 |
| GitHub Pages runs Jekyll on a repository by default. | GitHub | The tool writes an empty file `.nojekyll` (`README.md:190`) |
| The site must work when a person opens `index.html` from disk. | `README.md:192` | All content is in one script file; the template uses relative paths |
| Obsidian accepts three notations for tags in the front matter. | Obsidian | The selection accepts all three (`README.md:30`, `:43`) |
| The file system of macOS ignores letter case and Unicode form in names. | macOS | BR-024 |
| The tool must run on macOS and Linux. | owner, 2026-11-13 | No platform-specific path or command; Windows is out of scope |

## Open business questions

| # | Question | Who can answer | Blocks |
|---|---|---|---|
| 1 | Why does the project exist, beyond the text of the README? | the owner; unknown on 2026-11-13 | The section "What the business needs" |
| 2 | Must the tool accept a GitHub Pages project site? See the note below the table. | the owner | BR-016 |
| 3 | Does the code still agree with the specification in `service-spec-harness`? | the owner, or a run of the acceptance cases of the harness | `ecosystem.md` |

A GitHub user can publish a site from two kinds of repository:

- A **user site** comes from the repository `<owner>.github.io`. Its address is `https://<owner>.github.io/`. mb2e accepts this kind.
- A **project site** comes from any other repository, for example `<owner>/blog`. Its address is `https://<owner>.github.io/blog/`. mb2e refuses this kind, unless `mb2e.force` is `true`.

## Answered questions

| Question | Answer | Who, when |
|---|---|---|
| Is the Polish proposed commit message on purpose? | No, it was a mistake. The code now proposes `chore(content): refresh Knowledge Hub`, as the README shows. | owner, 2026-11-13 |
| Is the removal of the folder `datasource` still necessary? | No. See BR-021 and the debt in `architecture.md`. | owner, 2026-11-13 |
| Which platforms must the tool support? | macOS and Linux. | owner, 2026-11-13 |
| Is `service-spec-harness` the binding specification of mb2e? | It should be. The owner is not sure that the code agrees with it today. | owner, 2026-11-13 |

## What could not be verified

- Each rule has the confidence `inferred`. No person confirmed a rule.
- BR-006, BR-007, and BR-010 rest on code that calls classes the agent did not read (the front matter parser and the Markdown renderer).
- BR-021 rests on two refusal lines of the file adapter. The agent did not read the full adapter.
- The rules for Markdown rendering and for link resolution are not in this file. The agent did not read that code.
- The actors come from the README. No person confirmed the list.
