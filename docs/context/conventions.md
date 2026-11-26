---
title: Development conventions
type: context
status: draft
updated: 2026-11-26
sources:
  - pom.xml
  - src/main/java/io/github/mbero/mb2e/domain/service/BuildService.java
  - src/main/java/io/github/mbero/mb2e/domain/service/RemoteGuard.java
  - src/main/java/io/github/mbero/mb2e/adapter/in/shell/BuildCommand.java
  - src/main/java/io/github/mbero/mb2e/adapter/out/external/GitProgram.java
  - src/main/java/io/github/mbero/mb2e/infrastructure/config/Mb2eProperties.java
  - src/test/java/io/github/mbero/mb2e/domain/service/RemoteGuardTest.java
  - src/test/java/io/github/mbero/mb2e/adapter/out/external/GitProgramTest.java
  - src/test/java/io/github/mbero/mb2e/Mb2eExitCodeTest.java
  - src/test/java/io/github/mbero/mb2e/domain/service/BuildServiceTest.java
  - src/test/java/io/github/mbero/mb2e/Mb2eApplicationTest.java
  - ui/.prettierrc
tags: [conventions, code]
---

# Development conventions — mb2e

> Load this file before you write or modify any code in this repository.

Each rule in this file is an observation from the files in `sources`. No
written convention existed before this file. Java paths are below
`src/main/java/io/github/mbero/mb2e/` or `src/test/java/io/github/mbero/mb2e/`.

## Naming

| Element | Rule | Example |
|---|---|---|
| Package | `io.github.mbero.mb2e.<layer>.<direction>.<technology>` | `io.github.mbero.mb2e.adapter.out.persistence` |
| Class | A domain noun. No `Impl` suffix. A service ends with `Service`. An adapter starts with its technology. | `BuildService`, `FileVaultReader`, `GitProgram` |
| Inbound port | Ends with `UseCase` | `BuildUseCase` |
| Outbound port | A role noun, no prefix and no suffix | `VaultReader`, `PublishTarget` |
| Shell command class | Ends with `Command` | `RunCommand` |
| Data holder | A Java `record` | `VaultNote`, `GitChange` |
| Exception | Ends with `Failure` or `Refusal`, extends `RuntimeException` | `StoreFailure`, `GitFailure`, `ShellRefusal` |
| Test class | `<ClassUnderTest>Test`, package-private | `RemoteGuardTest` |
| Test method | A camelCase sentence that states the behaviour | `aMissingVaultIsRefusedAndNamesThePath` |
| Database table | Plural snake_case noun; columns in snake_case | `deployments`, `finished_at` |
| Configuration key | `mb2e.<kebab-case>` | `mb2e.default-note`, `mb2e.vault.path` |

## Package structure

```text
io.github.mbero.mb2e
├── adapter
│   ├── in.shell           shell commands, reports, refusals
│   ├── out.persistence    file access
│   └── out.external       the git program
├── domain
│   ├── model              records
│   ├── port.in            use case interfaces
│   ├── port.out           interfaces to the outside
│   └── service            the logic
└── infrastructure.config  Spring wiring and properties
```

**Rule:** this project groups by layer, not by feature. Put a new class in the
package of its layer.

## Layering

| Layer | May call | Must not call |
|---|---|---|
| `adapter.in.shell` | `domain.port.in`, `domain.model`, `adapter.in.log` | `adapter.out`, `domain.service` |
| `adapter.in.web` | `domain.port.in`, `domain.model`, `adapter.in.log`, Spring Web | `adapter.out`, `domain.service`, `adapter.in.shell` |
| `adapter.in.log` | `domain.model` | `adapter.out`, `domain.service` |
| `domain.service` | `domain.model`, `domain.port` | Spring, `adapter`, `infrastructure` |
| `adapter.out` | `domain.port.out`, `domain.model` | `adapter.in`, `domain.service` |
| `infrastructure.config` | all layers | — |

See [`architecture.md`](architecture.md) for the evidence.

## Code style

- One public type for each file.
- Helper classes of `domain.service` are `final`, package-private, and have a private constructor (`domain/service/RemoteGuard.java:12-21`).
- Domain services take their dependencies through the constructor. They have no Spring annotation (`domain/service/BuildService.java:42-50`).
- `infrastructure.config` creates each bean with a `@Bean` method (`infrastructure/config/BuildConfig.java:52-78`).
  HTTP endpoints are plain classes; `ServerConfig` registers their routes with a `RouterFunction` bean (`adapter/in/web/WebRoutes.java`).
- Properties are a `record` with `@ConfigurationProperties` and `@DefaultValue` (`infrastructure/config/Mb2eProperties.java:11-20`).
- A record makes a defensive copy of each list (`domain/model/Vault.java:14-17`).
- Indentation is four spaces. The longest line in `src/main/java` has 124 characters (measured 2026-11-13).
- Each class has a Javadoc block. The first sentence is a summary in the third person. A block has at most five lines.
- A comment explains **why**, never **what**.
- No lint or format tool exists for the Java code. Match the formatting of the surrounding file.
- Dashboard UI (`ui/`): one standalone component per file, `OnPush`, signals. The template and the CSS live in `<name>.html` and `<name>.css` beside `<name>.ts` (`docs/qa/project-rules.md` `[P-UI-01]`). Format with `npx prettier --write` in `ui/` (`ui/.prettierrc`, 100 columns).

## Tests

| Test type | Framework | Naming | Runs with |
|---|---|---|---|
| Unit | JUnit 5, AssertJ | `<ClassUnderTest>Test` | `mvn test` |
| Spring context | `@SpringBootTest`, same phase as the unit tests | `Mb2eApplicationTest` | `mvn test` |
| Integration | n/a | | n/a |

**Rules:**

- Every new unit of observable behaviour gets a test.
- A pure data holder does not need a test.
- A test name states the behaviour, not the method name.
- Tests use hand-written fakes of the ports, never a mock library (`domain/service/PublishServiceTest.java`).
  `pom.xml` excludes Mockito from `spring-boot-starter-test`, so a Mockito import does not compile.
  Spring Shell contexts are real objects built by `adapter/in/shell/ShellContexts.java`.
- A file test uses `@TempDir` (`adapter/out/external/GitProgramTest.java:17-18`).
- A process test calls `Mb2eApplication.run(args)` and captures standard output and standard error (`Mb2eExitCodeTest.java:19-34`).
- A test must not depend on the local file `application_local_paths.yaml`.

## Error handling

- The project defines `StoreFailure` (file system), `GitFailure` (git program) with its subclass `GitTimeoutFailure`, `ShellRefusal` (command), and `Mb2eEnvironment.Refused` (configuration).
- A use case catches `StoreFailure` and `GitFailure`. It returns a report with the outcome `refused`, a title, and a detail (`domain/service/BuildService.java:80-84`).
- The command prints the refused report on standard output. Then it throws `ShellRefusal` (`adapter/in/shell/BuildCommand.java:32-36`).
- The refusal sentence goes to standard error as plain text, with no stack trace and no colour.
- The exit code is 0 for success and 1 for each refusal or failure. No other code exists.
- A refusal message names the thing that it refuses: the path, the key, or the word.
- The step line goes to the logger `mb2e.step`: ERROR for `outcome=failed`, WARN for `refused` (and a denied login), INFO otherwise. Free text is quoted and kept on one line; a stack trace goes only to the optional log file (`adapter/in/log/StepLog.java`).

Review error handling **before** you hand a change over. See
`.agents/procedures/pre-commit-review.md`.

## Commits and branches

Observed in `git -C mb2e log` and `git -C mb2e branch -a` on 2026-11-13.

| Item | Rule |
|---|---|
| Branch name | `<type>/<snake_case_slug>`, for example `refactor/less_params`, `feature/added_base_template` |
| Commit subject | Mixed. Some commits use `type: imperative subject` (`feat: ...`, `refactor: ...`, `docs: ...`). Other commits use free text (`Updated README.md`). |
| Ticket reference | Not used. No commit and no branch contains a ticket key. |

**The agent never commits, pushes, or creates a branch without an explicit request.**

## Code review checklist

- [ ] The change does what the request asks, and nothing more.
- [ ] Error paths are handled and tested.
- [ ] No secret, token, or absolute machine path in the diff.
- [ ] New behaviour has a test.
- [ ] Public API changes are documented.
- [ ] `mvn -q clean verify` passes.
- [ ] The README and `docs/` agree with the code.
- [ ] A decision worth remembering became an ADR.

## Common traps in this codebase

- **Symptom:** `./mvnw: no such file or directory`. **Cause:** the project has no Maven wrapper. **Fix:** use `mvn`.
- **Symptom:** a command refuses with `The property mb2e.vault.path is not set.` **Cause:** mb2e reads `application_local_paths.yaml` from the working directory. **Fix:** run the command in the repository root, or set `MB2E_CONFIG`.
- **Symptom:** `--mb2e.tag=x` gives a refusal. **Cause:** command-line arguments never set a property. **Fix:** use `SPRING_APPLICATION_JSON`.
- **Symptom:** a system property `-Dmb2e.tag=x` has no effect. **Cause:** `Mb2eEnvironment` removes system properties and OS variables on purpose (`Mb2eEnvironment.java:36`).
- **Symptom:** a Spring Shell built-in command such as `history` or `--help` does not exist. **Cause:** `ShellConfig` and `Mb2eApplication` remove them on purpose.

## What could not be verified

- No person confirmed these conventions. They describe the code as it is.
- The preferred commit format is not known, because the history is mixed.
