# `.agents/` — the portable capability layer

A procedure holds the **logic**. A binding holds only the **trigger**.

```text
.agents/
├── procedures/   plain Markdown. Any agent, or any person, can follow these.
└── bindings/     per-tool wrappers. This folder exists only in the template.
```

## Why this split

Tool-specific features do not port. A slash command of one tool means nothing
to another tool.

So the behaviour lives in a Markdown procedure that every agent can read. A
binding is a three-line wrapper that says *"follow `.agents/procedures/x.md`"*.

**A project with no bindings still works.** The agent reads `AGENTS.md`, which
links the procedures. The binding only makes the trigger convenient.

## Rules

1. **Never put logic in a binding.** If you change behaviour, change the procedure.
2. **A procedure is written for a reader, not a parser.** Numbered steps, plain
   language, no tool-specific syntax.
3. **A procedure states its trigger** in its first section, so an agent knows
   when to apply it without being told.
4. **A procedure holds no project fact.** It refers to `AGENTS.md` or `docs/`
   for a command or a name. `bootstrap/sync.sh` can then replace it safely.

## Where the bindings go

`bootstrap/init.sh --tools <list>` copies the bindings of each selected tool
out of the template and into the folder that the tool reads:

| Tool | Installed to | Mechanism |
|---|---|---|
| GitHub Copilot | `.github/prompts/` | Prompt files, for example `/save-knowledge` |
| Claude Code | `.claude/commands/` | Slash commands |
| Cursor | `.cursor/rules/` | Rule files |

The project keeps one copy of each binding, in the folder of the tool. Skip a
tool and nothing breaks.
