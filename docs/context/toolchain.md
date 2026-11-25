---
title: Toolchain
type: context
status: draft
updated: 2026-11-13
sources:
  - pom.xml
  - README.md
  - bootstrap/check.sh
  - .github/workflows/knowledge-base.yml
tags: [toolchain, commands]
---

# Toolchain — mb2e

The exact command for every routine task. Run each command in the repository
root, because `pom.xml` and `application_local_paths.yaml` are there.

| Task | Command | Notes |
|---|---|---|
| Build | `mvn -q clean verify` | Runs the tests and writes `target/mb2e.jar` (`README.md:81-82`, `pom.xml:50`) |
| Run locally, no write | `java -jar target/mb2e.jar run --dry-run` | Plans build and publish and writes nothing (`README.md:173`) |
| Run locally | `java -jar target/mb2e.jar run` | **Writes into the Pages repository.** |
| Unit tests | `mvn test` | |
| Integration tests | n/a | No separate integration test phase exists in `pom.xml` |
| Lint / format | n/a | No lint or format plugin exists in `pom.xml` |
| Knowledge-base check | `bootstrap/check.sh` | Run in the knowledge base root, before you open a pull request |
| Knowledge-base index | `bootstrap/check.sh --index` | Run after you add, rename, or retitle a document |

No Maven wrapper exists. The commands `./mvnw ...` do not work in this repository.

The command `mb2e run` works only after the user adds the shell function from
`README.md:90` to `~/.zshrc`. An agent shell does not have this function.

## Longer recipes

### Run a single test

```bash
mvn -q test -Dtest=RemoteGuardTest
mvn -q test -Dtest='BuildServiceTest#aMissingVaultIsRefusedAndNamesThePath'
```

### See what the build selects, with no write

```bash
java -jar target/mb2e.jar build
java -jar target/mb2e.jar build --json
```

### Override one setting for one run

```bash
export SPRING_APPLICATION_JSON='{"mb2e.tag":"draft-ready"}'
```

Source: `README.md:157`. Command-line arguments never change the configuration (`README.md:160`).

### Produce a release artifact

```bash
mvn -q clean verify
ls target/mb2e.jar
```

The repository defines no release procedure and no version tag. The jar is the only artifact.

## Versions that matter

True on 2026-11-13.

| Component | Pinned version | Where it is pinned |
|---|---|---|
| Java | 25 | `pom.xml:20` |
| Spring Boot | 3.5.16 | `pom.xml:10` |
| Spring Shell | 3.4.3 | `pom.xml:21` |
| Project | 0.1.0-SNAPSHOT | `pom.xml:16` |
| Maven | 3.9 or later, not pinned | `README.md:71` |

## Continuous integration

- `.github/workflows/knowledge-base.yml` runs `bootstrap/check.sh` on a pull request and on a push to `main`.
- No workflow builds or tests the Java code.

## What could not be verified

- On 2026-11-13 the agent ran `mvn -q clean verify`: exit code 0, 135 tests in 23 classes, 0 failures, 0 errors.
- The syntax `-Dtest=...` is standard Maven Surefire syntax. The agent did not run it in this repository.
- The knowledge-base workflow runs on GitHub only after the folders `.github/`, `bootstrap/` and `docs/` reach the remote.
