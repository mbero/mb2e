# mbs-blog — the default site template of mb2e

A static blog and notebook: a category tree on the left (in the style of Obsidian), the article on the right.
Plain HTML, CSS and JavaScript, with no build step and no dependencies.

## Layout

```
mbs-blog/
├── index.html            page skeleton
└── assets/
    ├── css/styles.css    all styling (colour and font variables in :root)
    ├── js/data.js        sample content; mb2e replaces it when it publishes
    ├── js/app.js         tree, article, routing, mobile menu
    └── img/logo.jpeg     the site logo
```

## Run it locally

- Open `index.html` in a browser (it also works from `file://`), or
- run `npx serve .` or `python3 -m http.server` in this folder.

The page then shows the sample notes of `assets/js/data.js`.

## Content

Do not edit `assets/js/data.js` by hand for the real site. `mb2e build` and `mb2e run` read the vault,
render each note tagged for publication, and write `data.js` with the tree, the notes and the default note.
The repository README ("The data your `app.js` gets") describes its format.

The sample `data.js` uses small blocks (`p`, `h2`, `ul`, `code`, `link`) for design work; mb2e writes one
`html` block per note.

A note opens at `index.html#/<folder>/<note>`, for example `index.html#/Java/Streams`.

## Behaviour

- The page keeps the open folders in `localStorage`.
- A link to a note opens the folders above it.
- Below 900 px the category tree moves into a slide-in menu (button in the top-right corner).
- When the first block of a note is a `p` block, the page shows it as the lead under the title.
- The `date` of a note is optional; without it the page shows no date.
