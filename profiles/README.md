# Profiles

A profile is an **optional rule set** for one technology or one way of working.
Enabling a profile adds a link to section 9 of `AGENTS.md`. The agent reads the
profile only when it works on that subject.

## Why profiles exist

The always-loaded instruction file is a running cost. Copilot and Cursor inline
it into every request. A Kafka rule you read while renaming a variable is waste.

So the core stays small, and a profile carries the specialised rules.

## Available profiles

| Profile | Enable when |
|---|---|
| [`kafka.md`](kafka.md) | The project consumes or publishes Kafka topics |
| [`monolith-extraction.md`](monolith-extraction.md) | The project extracts areas out of a monolith |
| [`obsidian.md`](obsidian.md) | The team browses `docs/` in Obsidian |
| [`jira.md`](jira.md) | The team uses Jira, and the agent writes ticket text |
| [`github-issues.md`](github-issues.md) | The team plans work in GitHub Issues and a project board (mb2e only) |
| [`container-service.md`](container-service.md) | The project runs as a container service (mb2e only) |

## Enabling

`bootstrap/init.sh --profiles kafka,jira` copies the selected profiles into the
project and writes the links between the `PROFILES` markers in `AGENTS.md`.
A profile that the project did not select stays in the template repository.

To enable one later:

1. Copy its file from the template repository into `profiles/`.
2. Add a link to it inside the `<!-- PROFILES:START -->` block in `AGENTS.md`.

## Writing a new profile

- One subject per file.
- Rules for the knowledge base only. No project facts — those belong in
  `docs/context/`. No coding style guide — that belongs in
  `docs/context/conventions.md`.
- State the trigger in the first line: *"Read this when you ..."*.
- Keep it under 80 lines. A long profile is a document, not a rule set.
