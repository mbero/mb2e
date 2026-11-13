(function () {
  'use strict';

  var DATA = window.BLOG_DATA;
  var NOTES = DATA.NOTES;
  var STORAGE_KEY = 'mbs.tree.expanded';

  var els = {
    tree: document.getElementById('tree'),
    category: document.getElementById('note-category'),
    title: document.getElementById('note-title'),
    lead: document.getElementById('note-lead'),
    meta: document.getElementById('note-meta'),
    body: document.getElementById('note-body'),
    pager: document.getElementById('note-pager'),
    sidebar: document.getElementById('sidebar'),
    scrim: document.querySelector('.scrim'),
    menuBtn: document.querySelector('.topbar__menu'),
    filter: document.getElementById('tree-filter')
  };

  var filterText = '';

  /* ---------- Helpers ---------- */

  function el(tag, attrs, children) {
    var node = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        if (key === 'text') node.textContent = attrs[key];
        else if (key === 'className') node.className = attrs[key];
        else node.setAttribute(key, attrs[key]);
      });
    }
    (children || []).forEach(function (child) {
      if (child) node.appendChild(child);
    });
    return node;
  }

  function svg(markup) {
    var wrap = document.createElement('span');
    wrap.innerHTML = markup; // constant, trusted icon markup
    return wrap.firstChild;
  }

  var CHEVRON = '<svg aria-hidden="true" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>';

  function noteHref(id) {
    return '#/' + id.split('/').map(encodeURIComponent).join('/');
  }

  function currentNoteId() {
    var hash = window.location.hash.replace(/^#\/?/, '');
    if (!hash) return DATA.defaultNote;
    var id = hash.split('/').map(decodeURIComponent).join('/');
    return NOTES[id] ? id : DATA.defaultNote;
  }

  /* ---------- Folder expansion state ---------- */

  function loadExpanded() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return new Set(JSON.parse(raw));
    } catch (e) { /* no access to storage – use the defaults */ }
    return new Set(['AWS', 'Databases']);
  }

  function saveExpanded() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(expanded)));
    } catch (e) { /* ignore */ }
  }

  var expanded = loadExpanded();

  /* Folder paths that lead to a note (to reveal it in the tree) */
  function ancestorsOf(noteId) {
    var result = null;
    function walk(nodes, parents) {
      nodes.forEach(function (node) {
        if (result) return;
        if (node.note === noteId) { result = parents; return; }
        if (node.children) {
          var path = parents.length ? parents[parents.length - 1] + '/' + node.name : node.name;
          walk(node.children, parents.concat(path));
        }
      });
    }
    walk(DATA.TREE, []);
    return result || [];
  }

  /* Note order (for "Previous / Next") */
  function noteOrder() {
    var order = [];
    function walk(nodes) {
      nodes.forEach(function (node) {
        if (node.note) order.push(node.note);
        else if (node.children) walk(node.children);
      });
    }
    walk(DATA.TREE);
    return order;
  }

  /* ---------- Filter ---------- */

  function matches(text) {
    return String(text).toLowerCase().indexOf(filterText) !== -1;
  }

  /* Returns true when the branch holds a note that matches the filter */
  function branchMatches(node) {
    if (node.note) {
      var note = NOTES[node.note];
      return !!note && (matches(note.title) || matches(node.note));
    }
    if (matches(node.name)) return true;
    return (node.children || []).some(branchMatches);
  }

  /* ---------- Tree ---------- */

  function renderTree(activeId) {
    /* forced = the folder name matches, so its whole content is shown */
    function build(nodes, parentPath, forced) {
      var ul = el('ul', parentPath ? { role: 'group' } : null);

      nodes.forEach(function (node) {
        if (filterText && !forced && !branchMatches(node)) return;

        if (node.note) {
          var note = NOTES[node.note];
          if (!note) return;
          var link = el('a', {
            className: 'tree__row',
            href: noteHref(node.note),
            role: 'treeitem'
          }, [
            el('span', { className: 'tree__spacer', 'aria-hidden': 'true' }),
            el('span', { className: 'tree__label', text: note.title })
          ]);
          if (node.note === activeId) link.setAttribute('aria-current', 'page');
          ul.appendChild(el('li', null, [link]));
          return;
        }

        var path = parentPath ? parentPath + '/' + node.name : node.name;
        var isOpen = filterText ? true : expanded.has(path);
        var showAll = forced || (filterText ? matches(node.name) : false);

        var chevron = el('span', { className: 'tree__chevron', 'aria-hidden': 'true' }, [svg(CHEVRON)]);
        var button = el('button', {
          className: 'tree__row',
          type: 'button',
          role: 'treeitem',
          'aria-expanded': String(isOpen)
        }, [chevron, el('span', { className: 'tree__label', text: node.name })]);

        button.addEventListener('click', function () {
          if (expanded.has(path)) expanded.delete(path);
          else expanded.add(path);
          saveExpanded();
          renderTree(activeId);
        });

        var li = el('li', null, [button]);

        if (isOpen) {
          if (node.children && node.children.length) {
            li.appendChild(build(node.children, path, showAll));
          } else {
            var empty = el('ul', { role: 'group' }, [
              el('li', { className: 'tree__empty', text: 'No notes' })
            ]);
            li.appendChild(empty);
          }
        }

        ul.appendChild(li);
      });

      return ul;
    }

    var built = build(DATA.TREE, '', false);
    els.tree.replaceChildren.apply(els.tree, Array.from(built.childNodes));

    if (filterText && !els.tree.childNodes.length) {
      els.tree.appendChild(el('li', { className: 'tree__empty', text: 'No results' }));
    }
  }

  /* ---------- Article ---------- */

  function countWords(note) {
    var words = 0;
    note.blocks.forEach(function (b) {
      var html = b.html ? b.html.replace(/<[^>]*>/g, ' ') : '';
      var text = (b.text || '') + ' ' + (b.items || []).join(' ') + ' ' + html;
      words += text.split(/\s+/).filter(Boolean).length;
    });
    return words;
  }

  function renderBlock(block) {
    switch (block.type) {
      case 'p':
        return el('p', { text: block.text });
      case 'h2':
        return el('h2', { text: block.text });
      case 'ul':
        return el('ul', null, block.items.map(function (item) {
          return el('li', { text: item });
        }));
      case 'code':
        return el('pre', null, [el('code', { text: block.text })]);
      case 'link': {
        var target = NOTES[block.note];
        if (!target) return null;
        var p = el('p', { className: 'related' });
        p.appendChild(document.createTextNode('Related: '));
        p.appendChild(el('a', { href: noteHref(block.note), text: target.title }));
        return p;
      }
      case 'html': {
        // Note rendered from Markdown by mb2e at build time: trusted, generated content.
        var div = el('div', { className: 'note-html' });
        div.innerHTML = block.html;
        return div;
      }
      default:
        return null;
    }
  }

  function renderPager(id) {
    var order = noteOrder();
    var idx = order.indexOf(id);
    var prev = idx > 0 ? order[idx - 1] : null;
    var next = idx >= 0 && idx < order.length - 1 ? order[idx + 1] : null;

    var items = [];
    if (prev) {
      items.push(el('a', { className: 'pager__link pager__link--prev', href: noteHref(prev) }, [
        el('span', { className: 'pager__label', text: '← Previous' }),
        el('span', { className: 'pager__title', text: NOTES[prev].title })
      ]));
    }
    if (next) {
      items.push(el('a', { className: 'pager__link pager__link--next', href: noteHref(next) }, [
        el('span', { className: 'pager__label', text: 'Next →' }),
        el('span', { className: 'pager__title', text: NOTES[next].title })
      ]));
    }
    els.pager.replaceChildren.apply(els.pager, items);
  }

  function renderArticle(id) {
    var note = NOTES[id];
    var first = note.blocks[0];
    var hasLead = first && first.type === 'p';

    els.category.textContent = note.category;
    els.title.textContent = note.title;
    els.lead.textContent = hasLead ? first.text : '';
    els.lead.hidden = !hasLead;

    var meta = [];
    if (note.date) meta.push(el('time', { datetime: note.date, text: note.date }));
    meta.push(el('span', { text: Math.max(1, Math.ceil(countWords(note) / 200)) + ' min read' }));
    if (note.tags && note.tags.length) {
      meta.push(el('span', { className: 'article__tags' }, note.tags.map(function (t) {
        return el('span', { className: 'article__tag', text: '#' + t });
      })));
    }
    els.meta.replaceChildren.apply(els.meta, meta);

    var blocks = (hasLead ? note.blocks.slice(1) : note.blocks)
      .map(renderBlock)
      .filter(Boolean);
    els.body.replaceChildren.apply(els.body, blocks);

    renderPager(id);
    document.title = note.title + ' — MBS. Knowledge Hub Blog';
  }

  /* ---------- Mobile panel ---------- */

  function setDrawer(open) {
    els.sidebar.classList.toggle('is-open', open);
    els.scrim.hidden = !open;
    els.menuBtn.setAttribute('aria-expanded', String(open));
    els.menuBtn.setAttribute('aria-label', open ? 'Close the category menu' : 'Open the category menu');
  }

  els.menuBtn.addEventListener('click', function () {
    setDrawer(!els.sidebar.classList.contains('is-open'));
  });
  els.scrim.addEventListener('click', function () { setDrawer(false); });
  els.sidebar.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && els.sidebar.classList.contains('is-open')) {
      setDrawer(false);
      els.menuBtn.focus();
    }
  });

  /* ---------- Routing ---------- */

  function route(isInitial) {
    var id = currentNoteId();

    ancestorsOf(id).forEach(function (path) { expanded.add(path); });
    saveExpanded();

    renderTree(id);
    renderArticle(id);
    setDrawer(false);

    var about = document.getElementById('about-link');
    if (id === 'about') about.setAttribute('aria-current', 'page');
    else about.removeAttribute('aria-current');

    if (!isInitial) window.scrollTo(0, 0);
  }

  els.filter.addEventListener('input', function () {
    filterText = els.filter.value.trim().toLowerCase();
    renderTree(currentNoteId());
  });

  window.addEventListener('hashchange', function () { route(false); });
  route(true);
})();
