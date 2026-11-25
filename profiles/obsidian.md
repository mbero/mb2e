# Profile — Obsidian

**Enable this when the team browses `docs/` inside Obsidian.**

Obsidian builds a knowledge graph from `[[wikilinks]]`. The graph is the reason
to enable this profile: it shows which concepts connect, including concepts
that have no file yet.

## Rules

Use `[[wikilinks]]` for:

- another document in `docs/`: `[[architecture]]`, `[[order-checkout-data-flow]]`
- a class or component name, even with no file behind it: `[[CheckoutService]]`
- a named domain concept or business process: `[[OrderCheckout]]`

Pair a class wikilink with the path when the location adds value:

```text
[[CheckoutService]] (`src/main/java/com/example/CheckoutService.java`)
```

Do **not** use wikilinks for:

- a module or package name — use inline code: `order-service`
- an external product or tool
- anything inside `docs/published/` — a wiki cannot render `[[...]]`

## Cost of enabling this profile

GitHub does not render `[[wikilinks]]`. They appear as literal text in the web
view and in a pull request diff.

Enable the profile when the team reads `docs/` in Obsidian more often than on
GitHub. Otherwise keep standard Markdown links.

## Settings that help

- Turn off "automatically update internal links" while an agent edits files in
  bulk. It rewrites links in files the agent is also writing.
- Keep `docs/` as the vault root, not the whole repository. A vault over the
  repository indexes `node_modules` and build output.
