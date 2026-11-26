---
title: Glossary
type: context
status: draft
updated: 2026-11-25
sources:
  - README.md
  - src/main/java/io/github/mbero/mb2e/domain/model/Vault.java
  - src/main/java/io/github/mbero/mb2e/domain/model/SiteData.java
  - src/main/java/io/github/mbero/mb2e/domain/model/GeneratedSite.java
  - src/main/java/io/github/mbero/mb2e/domain/model/ConfiguredPaths.java
  - src/main/java/io/github/mbero/mb2e/domain/model/StepResult.java
  - src/main/java/io/github/mbero/mb2e/domain/service/BuildService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PublishService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/Selection.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/shell/ArgumentGuard.java
tags: [glossary, domain]
---

# Glossary — mb2e

One term, one definition. When a term has two meanings in the codebase, that is
a finding — record it and name both.

| Term | Definition | Where it appears in code | Do not confuse with |
|---|---|---|---|
| Vault | The folder that holds the Markdown notes and their attachments. mb2e only reads it. | `Vault`, `VaultReader`, `mb2e.vault.path` | Publication target |
| Note | One Markdown file of the vault. | `VaultNote`, `NoteEntry` | Attachment |
| Note id | The path of a note in the vault, without `.md`, with `/` between folders. | `BuildService.java:182` | Title |
| Front matter | The block between two `---` lines at the top of a note. It holds `title`, `tags`, and `date`. | `FrontMatter`, `Selection.frontMatterTags` | The body of the note |
| Selection tag | The tag that marks a note for publication. The default is `blog-ready`. | `mb2e.tag`, `Selection` | The other tags of a note |
| Inline tag | A `#tag` in the body of a note. It selects a note only when `mb2e.inline-tags` is `true`. | `Selection.hasInlineTag` | A tag in the front matter |
| Selected note | A note that carries the selection tag. Only a selected note goes to the site. | `BuildService.select` | Scanned note (each note of the vault) |
| Attachment | A file of the vault that is not Markdown, for example an image. | `Vault.files`, `BuildReport.attachmentCount` | Note |
| Site template | A folder with `index.html` and `assets/` that gives the site its look. mb2e only reads it. | `SiteSource`, `TemplateCheck`, `mb2e.template.path` | The generated site |
| Site | The template files, the generated data script, and the note images together. | `GeneratedSite`, `Assembly` | Site template |
| Data script | The generated file `assets/js/data.js`. It holds all notes as `window.BLOG_DATA`. | `SiteData`, `SiteDataJson`, `PublishService.DATA_SCRIPT` | The sample `data.js` of the template, which mb2e never publishes |
| Tree | The folders and notes that the side panel of the site shows. | `TreeNode`, `SiteData.tree` | The file tree of the target |
| About page | The page from the note `about.md` at the root of the vault. It is not in the tree. | `BuildService.ABOUT_ID` | An `about.md` in a sub-folder, which is a usual note |
| Start note | The note that the site opens when the URL names no note. | `mb2e.default-note`, `SiteData.defaultNote` | About page (the default start note, but a different concept) |
| Wikilink | A link in the form `[[Another note]]`. | `NoteIndex`, `LinkResolver` | A standard Markdown link |
| Unresolved link | A wikilink to a note that is not selected or does not exist. | `BuildReport.missingLinks` | Missing file |
| Publication target | The local working copy of the GitHub Pages repository. It is the only place that mb2e writes. | `PublishTarget`, `mb2e.target.path` | The Maven folder `target/` |
| Pages repository | A GitHub repository with the name `<owner>.github.io`. | `RemoteGuard` | Publication target (its local clone) |
| Build | The step that selects, renders, and assembles the site in memory. It writes nothing. | `BuildService`, command `build` | A Maven build |
| Publish | The step that mirrors the site into the publication target. | `PublishService`, command `publish` | `git push`, which the CLI never runs (the server mode deploys) |
| Pipeline / run | Build, then publish, with one report. | `PipelineService`, command `run` | To start the application |
| Mirror | To make `assets/` of the target equal to the site: write changed files, delete stale files. | `PublishService.planFor` | Copy |
| Plan | The complete list of writes and deletes of one publication. mb2e completes it before the first write. | `PublishService.Plan` | |
| Dry run | A run that plans the operation and writes nothing. | option `--dry-run` | The command `build` |
| Report | The result of one step: outcome, counts, warnings. | `BuildReport`, `PublishReport`, `RunReport` | Log line |
| Refusal | An operation that mb2e does not do, with a title and a detail. The exit code is 1. | `ShellRefusal`, outcome `refused` | Warning, which does not stop the operation |
| Step | Build or publish as one part of a run. | `StepResult`, `StepLog` | |
| Trace id | A random identifier of one operation. The report and the log line show it. | `BuildReport.traceId` | |
| Stale datasource | A top-level folder `datasource` from an earlier layout of the site. Publish deletes it. | `PublishService.STALE_DATASOURCE` | |
| Twin | A target file whose name differs from a site file only in letter case or Unicode form. | `PublishService.rewriteTwin` | |
| One-shot mode | A run with a command on the command line. It prints no banner. | `OneShotRunner` | Interactive shell, script mode |
| Script mode | A run with one argument `@file`. mb2e reads the commands from the file. | `ArgumentGuard.isScriptMode` | One-shot mode |

## Rules

1. Define a term the first time a document uses it, then link here.
2. A definition must stand on its own. Do not define a term with another
   undefined term.
3. If the business and the code use different names for one thing, record both
   and mark which name the code uses.
4. Remove a term when the code removes it. A stale glossary is worse than none.

## Ambiguous terms

| Term | Meaning A | Meaning B | How to tell them apart |
|---|---|---|---|
| target | The publication target, `mb2e.target.path` | The Maven output folder `target/` | Meaning B is always a path in the repository of the code. |
| build | The mb2e command `build` | The Maven build `mvn clean verify` | Meaning A reads a vault. Meaning B makes `mb2e.jar`. |
| run | The mb2e command `run` | To start the application | Meaning A is always written as code: `run`. |
| template | The folder that `mb2e.template.path` names | The sample template in `src/main/resources/template/mbs-blog` | mb2e reads only meaning A. The Java code does not read the sample from the jar. |
| image / media / attachment | The code uses `images`, `media`, and `attachments` for the files that notes embed | | `BuildReport` says attachments. `PublishReport` says media. `GeneratedSite` says images. |
| README name and code name | The README says "Pages repo" | The code says "publication target" | The code name is `target`. |

## What could not be verified

- The agent did not read `NoteIndex`, `LinkResolver`, `FrontMatter`, `SiteDataJson`, `BuildReport`, and `PublishReport`. The definitions of the terms that cite them come from the README and from the callers.
- It is not known which attachment types mb2e embeds. The README names only images.
