package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders note Markdown to HTML exactly as the site's earlier JavaScript renderer did.
 * Its quirks are intended and must stay: an ordered list after an unordered one is absorbed
 * into it, a bare URL's "&" is escaped twice in its href, and "[t](javascript:x(1))" leaves a stray ")".
 */
final class MarkdownRenderer {

    /** A code point of the Unicode private-use area. Note content does not contain it. */
    private static final String MARKER = "";

    private static final Pattern CODE_SPAN = Js.regex("(`+)([\\s\\S]*?)\\1");
    private static final Pattern PADDED_CODE = Js.regex("^ (.*) \\z");
    private static final Pattern EMBED = Js.regex("!\\[\\[([^\\]|]+?)(?:\\|([^\\]]*))?\\]\\]");
    private static final Pattern WIKILINK = Js.regex("\\[\\[([^\\]|]+?)(?:\\|([^\\]]*))?\\]\\]");
    private static final Pattern IMAGE = Js.regex(
            "!\\[([^\\]]*)\\]\\(\\s*([^)\\s]+)(?:\\s+&quot;[^&]*&quot;)?\\s*\\)");
    private static final Pattern LINK = Js.regex(
            "\\[([^\\]]*)\\]\\(\\s*([^)\\s]+)(?:\\s+&quot;[^&]*&quot;)?\\s*\\)");
    private static final Pattern STRONG = Js.regex("\\*\\*([^*]+)\\*\\*");
    private static final Pattern STRIKE = Js.regex("~~([^~]+)~~");
    private static final Pattern EMPHASIS = Js.regex("\\*([^*\\n]+)\\*");
    private static final Pattern BARE_URL = Js.regex("(^|[\\s(])(https?://[^\\s<)]+)");
    private static final Pattern MARKED_CODE = Js.regex(MARKER + "([0-9]+)" + MARKER);
    private static final Pattern EXTERNAL = Pattern.compile("^https?:", Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS = Pattern.compile(
            "^\\s*(javascript:|data:text/html)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern ITEM = Js.regex("^(\\s*)([-*+]|[0-9]+[.)])\\s+(.*)\\z");
    private static final Pattern FENCE = Js.regex("^(\\s*)(`{3,}|~{3,})\\s*([^\\s`]*)\\s*\\z");
    private static final Pattern HEADING = Js.regex("^(#{1,6})\\s+(.*)\\z");
    private static final Pattern QUOTE = Js.regex("^\\s*>\\s?");
    private static final Pattern HR_DASH = Js.regex("^\\s*(-\\s*){3,}\\z");
    private static final Pattern HR_STAR = Js.regex("^\\s*(\\*\\s*){3,}\\z");
    private static final Pattern HR_UNDERSCORE = Js.regex("^\\s*(_\\s*){3,}\\z");
    private static final Pattern TABLE_SEPARATOR = Js.regex("^[\\s|:-]+\\z");
    private static final Pattern ALIGN_CENTER = Js.regex("^:-+:\\z");
    private static final Pattern ALIGN_RIGHT = Js.regex("^-+:\\z");
    private static final Pattern LEADING_HASHES = Js.regex("^#+\\s*");
    private static final Pattern TRAILING_HASHES = Js.regex("\\s+#+\\s*\\z");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern TAB = Pattern.compile("\t");

    private MarkdownRenderer() {
    }

    static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String safeUrl(String url) {
        String value = Js.trim(url);
        return DANGEROUS.matcher(value).find() ? "#" : value;
    }

    /** Strips tags: every tag and every "&name;" sequence becomes a space. */
    static String stripTags(String html) {
        return html.replaceAll("<[^>]*>", " ").replaceAll("&[a-z]+;", " ");
    }

    static List<Block> render(String markdown, LinkResolver resolver) {
        return renderBlocks(markdown, resolver);
    }

    // ---------- inline ----------

    private static String alt(String alias, String target) {
        return Js.truthy(alias) ? alias : target;
    }

    static String renderInline(String text, LinkResolver ctx) {
        List<String> codes = new ArrayList<>();
        String out = Js.replace(CODE_SPAN, text, m -> {
            String code = m.group(2);
            Matcher padded = PADDED_CODE.matcher(code);
            codes.add("<code>" + escapeHtml(padded.find() ? padded.group(1) : code) + "</code>");
            return MARKER + (codes.size() - 1) + MARKER;
        });

        out = escapeHtml(out);

        // Obsidian embed: ![[file]]
        out = Js.replace(EMBED, out, m -> {
            String target = m.group(1);
            String alias = m.group(2);
            Optional<String> asset = ctx.resolveAsset(Js.trim(target));
            if (asset.isPresent()) {
                return "<img src=\"" + escapeHtml(asset.get()) + "\" alt=\"" + escapeHtml(alt(alias, target))
                        + "\" loading=\"lazy\">";
            }
            Optional<LinkResolver.NoteLink> note = ctx.resolveNote(Js.trim(target));
            if (note.isPresent()) {
                return anchor(note.get(), alias);
            }
            return "<span class=\"missing-link\" title=\"Brak pliku w vaulcie\">"
                    + escapeHtml(alt(alias, target)) + "</span>";
        });

        // Obsidian link: [[Note|alias]]
        out = Js.replace(WIKILINK, out, m -> {
            String target = m.group(1);
            String alias = m.group(2);
            Optional<LinkResolver.NoteLink> note = ctx.resolveNote(Js.trim(target));
            if (note.isPresent()) {
                return anchor(note.get(), alias);
            }
            return "<span class=\"missing-link\" title=\"Notatka nie jest publikowana\">"
                    + escapeHtml(alt(alias, target)) + "</span>";
        });

        // Image: ![alt](src)
        out = Js.replace(IMAGE, out, m -> {
            String src = m.group(2);
            Optional<String> resolved = ctx.resolveAsset(Js.decodeSafe(src));
            String address = resolved.filter(s -> !s.isEmpty()).orElseGet(() -> safeUrl(src));
            return "<img src=\"" + escapeHtml(address) + "\" alt=\"" + m.group(1) + "\" loading=\"lazy\">";
        });

        // Link: [text](url)
        out = Js.replace(LINK, out, m -> {
            String href = safeUrl(m.group(2));
            boolean external = EXTERNAL.matcher(href).find();
            Optional<LinkResolver.NoteLink> note = external ? Optional.empty()
                    : ctx.resolveNote(Js.decodeSafe(href));
            if (note.isPresent()) {
                href = note.get().href();
            }
            return "<a href=\"" + escapeHtml(href) + "\""
                    + (external ? " target=\"_blank\" rel=\"noopener noreferrer\"" : "") + ">" + m.group(1) + "</a>";
        });

        out = STRONG.matcher(out).replaceAll("<strong>$1</strong>");
        out = STRIKE.matcher(out).replaceAll("<del>$1</del>");
        out = EMPHASIS.matcher(out).replaceAll("<em>$1</em>");

        // Bare URL
        out = Js.replace(BARE_URL, out, m -> m.group(1) + "<a href=\"" + escapeHtml(m.group(2))
                + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + m.group(2) + "</a>");

        return Js.replace(MARKED_CODE, out, m -> codes.get(Integer.parseInt(m.group(1))));
    }

    private static String anchor(LinkResolver.NoteLink note, String alias) {
        return "<a href=\"" + escapeHtml(note.href()) + "\">" + escapeHtml(alt(alias, note.title())) + "</a>";
    }

    // ---------- blocks ----------

    private static int indentOf(String line) {
        Matcher m = Js.regex("^\\s*").matcher(line);
        m.find();
        return TAB.matcher(m.group()).replaceAll("    ").length();
    }

    private static boolean isHr(String line) {
        return HR_DASH.matcher(line).find() || HR_STAR.matcher(line).find() || HR_UNDERSCORE.matcher(line).find();
    }

    private static boolean isTableSeparator(String line) {
        return line.indexOf('|') != -1 && TABLE_SEPARATOR.matcher(line).find() && line.indexOf('-') != -1;
    }

    private static boolean startsBlock(String line) {
        return Js.isBlank(line) || HEADING.matcher(line).find() || FENCE.matcher(line).find()
                || isHr(line) || ITEM.matcher(line).find() || QUOTE.matcher(line).find();
    }

    private static List<String> splitRow(String line) {
        String row = Js.trim(line);
        if (row.startsWith("|")) {
            row = row.substring(1);
        }
        if (row.endsWith("|")) {
            row = row.substring(0, row.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        for (String cell : row.split("\\|", -1)) {
            cells.add(Js.trim(cell));
        }
        return cells;
    }

    private static String joinHtml(List<Block> blocks, String separator) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                out.append(separator);
            }
            out.append(blocks.get(i).html());
        }
        return out.toString();
    }

    static List<Block> renderBlocks(String source, LinkResolver ctx) {
        String[] lines = source.replaceAll("\r\n?", "\n").split("\n", -1);
        List<Block> blocks = new ArrayList<>();
        int i = 0;

        while (i < lines.length) {
            String line = lines[i];

            if (Js.isBlank(line)) {
                i++;
                continue;
            }

            Matcher fence = FENCE.matcher(line);
            if (fence.find()) {
                char closing = fence.group(2).charAt(0);
                Pattern closeRe = Js.regex("^\\s*" + (closing == '`' ? "`" : "~") + "{3,}\\s*\\z");
                List<String> code = new ArrayList<>();
                i++;
                while (i < lines.length && !closeRe.matcher(lines[i]).find()) {
                    code.add(lines[i]);
                    i++;
                }
                i++;
                String lang = Js.truthy(fence.group(3)) ? " class=\"language-" + escapeHtml(fence.group(3)) + "\"" : "";
                blocks.add(new Block("code", "<pre><code" + lang + ">" + escapeHtml(String.join("\n", code))
                        + "</code></pre>"));
                continue;
            }

            Matcher heading = HEADING.matcher(line);
            if (heading.find()) {
                int level = heading.group(1).length();
                String title = Js.trim(TRAILING_HASHES.matcher(
                        LEADING_HASHES.matcher(heading.group(2)).replaceFirst("")).replaceFirst(""));
                i++;
                if (title.isEmpty()) {
                    continue;
                }
                blocks.add(new Block("h" + level,
                        "<h" + level + ">" + renderInline(title, ctx) + "</h" + level + ">"));
                continue;
            }

            if (isHr(line)) {
                blocks.add(new Block("hr", "<hr>"));
                i++;
                continue;
            }

            if (line.indexOf('|') != -1 && i + 1 < lines.length && isTableSeparator(lines[i + 1])) {
                List<String> header = splitRow(line);
                List<String> align = new ArrayList<>();
                for (String cell : splitRow(lines[i + 1])) {
                    align.add(ALIGN_CENTER.matcher(cell).find() ? " style=\"text-align:center\""
                            : ALIGN_RIGHT.matcher(cell).find() ? " style=\"text-align:right\"" : "");
                }
                i += 2;
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && !Js.isBlank(lines[i]) && lines[i].indexOf('|') != -1) {
                    rows.add(splitRow(lines[i]));
                    i++;
                }
                StringBuilder table = new StringBuilder("<div class=\"table-wrap\"><table><thead><tr>");
                for (int index = 0; index < header.size(); index++) {
                    table.append("<th").append(alignAt(align, index)).append('>')
                            .append(renderInline(header.get(index), ctx)).append("</th>");
                }
                table.append("</tr></thead><tbody>");
                for (List<String> row : rows) {
                    table.append("<tr>");
                    for (int index = 0; index < header.size(); index++) {
                        String cell = index < row.size() ? row.get(index) : "";
                        table.append("<td").append(alignAt(align, index)).append('>')
                                .append(renderInline(cell, ctx)).append("</td>");
                    }
                    table.append("</tr>");
                }
                blocks.add(new Block("table", table.append("</tbody></table></div>").toString()));
                continue;
            }

            if (QUOTE.matcher(line).find()) {
                List<String> quoted = new ArrayList<>();
                while (i < lines.length && (QUOTE.matcher(lines[i]).find()
                        || (!Js.isBlank(lines[i]) && !quoted.isEmpty() && !startsBlock(lines[i])))) {
                    quoted.add(QUOTE.matcher(lines[i]).replaceFirst(""));
                    i++;
                }
                String inner = joinHtml(renderBlocks(String.join("\n", quoted), ctx), "");
                blocks.add(new Block("quote", "<blockquote>" + inner + "</blockquote>"));
                continue;
            }

            if (ITEM.matcher(line).find()) {
                ParsedList list = parseList(lines, i, ctx);
                blocks.add(new Block("list", list.html()));
                i = list.next();
                continue;
            }

            List<String> paragraph = new ArrayList<>();
            while (i < lines.length && !Js.isBlank(lines[i])
                    && !HEADING.matcher(lines[i]).find() && !FENCE.matcher(lines[i]).find()
                    && !isHr(lines[i]) && !ITEM.matcher(lines[i]).find() && !QUOTE.matcher(lines[i]).find()) {
                paragraph.add(Js.trim(lines[i]));
                i++;
            }
            blocks.add(new Block("p", "<p>" + renderInline(String.join(" ", paragraph), ctx) + "</p>"));
        }

        return blocks;
    }

    private static String alignAt(List<String> align, int index) {
        return index < align.size() ? align.get(index) : "";
    }

    private record ParsedList(String html, int next) {
    }

    /** Removes one leading "&lt;p&gt;" and one trailing "&lt;/p&gt;". */
    static String unwrapParagraph(String html) {
        String out = html.startsWith("<p>") ? html.substring(3) : html;
        return out.endsWith("</p>") ? out.substring(0, out.length() - 4) : out;
    }

    private static ParsedList parseList(String[] lines, int start, LinkResolver ctx) {
        Matcher first = ITEM.matcher(lines[start]);
        first.find();
        int baseIndent = indentOf(lines[start]);
        boolean ordered = DIGIT.matcher(first.group(2)).find();
        List<List<String>> items = new ArrayList<>();
        int i = start;

        while (i < lines.length) {
            String line = lines[i];

            if (Js.isBlank(line)) {
                int lookahead = i;
                while (lookahead < lines.length && Js.isBlank(lines[lookahead])) {
                    lookahead++;
                }
                if (lookahead >= lines.length) {
                    break;
                }
                int nextIndent = indentOf(lines[lookahead]);
                boolean nextIsItem = ITEM.matcher(lines[lookahead]).find();
                boolean continues = (nextIsItem && nextIndent >= baseIndent)
                        || (!nextIsItem && nextIndent > baseIndent);
                if (!continues) {
                    break;
                }
                if (!items.isEmpty()) {
                    items.get(items.size() - 1).add("");
                }
                i = lookahead;
                continue;
            }

            int indent = indentOf(line);
            Matcher item = ITEM.matcher(line);
            boolean isItem = item.find();

            if (isItem && indent <= baseIndent + 1) {
                List<String> buffer = new ArrayList<>();
                buffer.add(item.group(3));
                items.add(buffer);
                i++;
                continue;
            }

            if (indent > baseIndent && !items.isEmpty()) {
                int cut = Math.min(Math.min(indent, baseIndent + 2), line.length());
                items.get(items.size() - 1).add(line.substring(cut));
                i++;
                continue;
            }

            break;
        }

        StringBuilder body = new StringBuilder();
        for (List<String> buffer : items) {
            List<Block> inner = new ArrayList<>(renderBlocks(String.join("\n", buffer), ctx));
            if (!inner.isEmpty() && inner.get(0).type().equals("p")) {
                inner.set(0, new Block("p", unwrapParagraph(inner.get(0).html())));
            }
            body.append("<li>").append(joinHtml(inner, "")).append("</li>");
        }

        String tag = ordered ? "ol" : "ul";
        return new ParsedList("<" + tag + ">" + body + "</" + tag + ">", i);
    }
}
