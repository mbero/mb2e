---
title: Local setup
type: context
status: draft
updated: 2026-11-13
sources:
  - README.md
  - pom.xml
  - application_local_paths.example.yaml
  - .gitignore
  - src/main/resources/application.yaml
  - src/main/java/io/github/mbero/mb2e/Mb2eEnvironment.java
  - src/main/java/io/github/mbero/mb2e/domain/service/TemplateCheck.java
  - src/main/java/io/github/mbero/mb2e/domain/service/PublishService.java
tags: [setup, local]
---

# Local setup — mb2e

> Load this file when the build fails, the application does not start, or the
> IDE import breaks.

## Prerequisites

Supported platforms: macOS and Linux (owner, 2026-11-13). Windows is not supported.

| Tool | Version | Install |
|---|---|---|
| Java (JDK) | 25 (`pom.xml:20`) | not stated in the repository |
| Maven | 3.9 or later (`README.md:71`) | not stated in the repository |
| git | not stated | Necessary for `publish` and `run`; mb2e starts the `git` program |
| An Obsidian vault on disk | | `README.md:72` |
| A site template folder | | The shipped one is `src/main/resources/template/mbs-blog` |
| A local clone of a GitHub Pages repository | | `README.md:74` |

## First run

```bash
# 1. clone and enter
git clone https://github.com/mbero/mb2e.git
cd mb2e

# 2. build; this also runs the tests
mvn -q clean verify

# 3. configure the three paths
cp application_local_paths.example.yaml application_local_paths.yaml
#    then edit application_local_paths.yaml

# 4. look, with no write
java -jar target/mb2e.jar build
java -jar target/mb2e.jar run --dry-run
```

**Warning:** `java -jar target/mb2e.jar run` without `--dry-run` writes and deletes files in the Pages repository.

## Configuration

Sources, in rising precedence (`README.md:154`):

1. the packaged `application.yaml`,
2. `application_local_paths.yaml` in the working directory, or the file that `MB2E_CONFIG` names,
3. the `mb2e.*` keys of `SPRING_APPLICATION_JSON`.

| Variable | Default | Purpose | Secret |
|---|---|---|---|
| `mb2e.vault.path` | none | The Obsidian vault; read only | no |
| `mb2e.template.path` | none | The site template; read only | no |
| `mb2e.target.path` | none | The local Pages repository; mb2e writes here | no |
| `mb2e.tag` | `blog-ready` | The tag that selects a note | no |
| `mb2e.inline-tags` | `false` | `true`: a `#tag` in the body also selects a note | no |
| `mb2e.default-note` | `about` | The note that the site opens first | no |
| `mb2e.force` | `false` | `true`: a wrong origin of the target gives a warning, not a refusal | no |
| `mb2e.log.file` | empty | Optional log file | no |
| `MB2E_CONFIG` (OS variable) | unset | Path of the local paths file | no |
| `SPRING_APPLICATION_JSON` (OS variable) | unset | Overrides; accepts only keys below `mb2e.` | no |

Defaults: `src/main/resources/application.yaml:26-35`. mb2e has no secret.

`application_local_paths.yaml` holds machine paths. `.gitignore:6` excludes it. Never commit it.

`build` needs the vault path and the template path. `publish` and `run` need all three paths (`README.md:138`).

## IDE setup

The folder `.idea/` exists, and `.gitignore:2` excludes it. Import
`pom.xml` as a Maven project. The project uses no annotation processor.
`.editorconfig` sets UTF-8, LF line ends, and an indent of 4 spaces for Java
and 2 spaces for the other files.

## Known local traps

### The paths file is not found

**Symptom:** `The property mb2e.vault.path is not set.`
**Cause:** mb2e reads `application_local_paths.yaml` from the working directory (`Mb2eEnvironment.java:65-67`). The file is optional, so a wrong directory gives no file error.
**Fix:**

```bash
cd /path/to/mb2e && java -jar target/mb2e.jar build
# or
export MB2E_CONFIG=/path/to/application_local_paths.yaml
```

**Verify:** the build report shows the line `Vault:` with your path.

### No Maven wrapper

**Symptom:** `./mvnw: no such file or directory`
**Cause:** the repository has no wrapper script.
**Fix:**

```bash
mvn -q clean verify
```

**Verify:** `ls target/mb2e.jar` shows the file.

### The template is refused

**Symptom:** `The configured directory is not a site template`
**Cause:** `assets/js/app.js` does not contain the text `case 'html'`, or the template holds files below `assets/img/notes` (`TemplateCheck.java:36-43`).
**Fix:** point `mb2e.template.path` at a folder that follows the template contract in `README.md:196-218`.
**Verify:** `java -jar target/mb2e.jar build` exits with 0.

### The target is refused

**Symptom:** `Publication target is not the expected repository`
**Cause:** the origin of the target is not `<owner>/<owner>.github.io` on GitHub (`PublishService.java:165-171`).
**Fix:** correct `mb2e.target.path`, or set `mb2e.force: true` for a different repository.
**Verify:** `java -jar target/mb2e.jar run --dry-run` exits with 0.

### An alias for mb2e fails in zsh

**Symptom:** `parse error near 'run'`
**Cause:** a zsh alias cannot take arguments (`README.md:105`).
**Fix:** use the shell function from `README.md:90`.
**Verify:** `type mb2e` prints `mb2e is a shell function`.

## What could not be verified

- The agent did not run the build or the application. The traps come from the code and the README.
- The repository does not state how to install Java 25 or Maven.
- The minimum git version is not known.
- The repository gives install steps only for macOS and zsh (`README.md:85`). Nobody ran mb2e on Linux yet.
