package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decides which vault notes carry the selection tag, in front matter or, when switched on, inline. */
final class Selection {

    private static final Pattern KEY = Js.regex("^([A-Za-z0-9_-]+)\\s*:\\s*(.*)$");
    private static final Pattern ITEM = Js.regex("^\\s*-\\s*(.+?)\\s*$");
    private static final Pattern LIST_SPLIT = Js.regex("[,\\s]+");
    private static final Pattern FRONT_MATTER_BLOCK = Pattern.compile("\\A---\\n.*?\\n---\\n", Pattern.DOTALL);

    private Selection() {
    }

    /** Strips a BOM and turns every line break into a line feed. */
    static String normalise(String raw) {
        return raw.replaceFirst("^﻿", "").replaceAll("\r\n?", "\n");
    }

    /** Returns a tag as it is compared: no quotes, no leading hash, trimmed and in lower case. */
    static String cleanTag(String value) {
        String s = Js.trim(value).replaceAll("^['\"]|['\"]$", "").replaceFirst("^#", "");
        return Js.trim(s).toLowerCase(Locale.ROOT);
    }

    /** Returns whether the note carries the tag in its front matter, or inline when that is switched on. */
    static boolean selects(String text, String tag, boolean inlineTags) {
        return frontMatterTags(text).contains(tag) || (inlineTags && hasInlineTag(text, tag));
    }

    private static List<String> splitInlineList(String value) {
        List<String> tags = new ArrayList<>();
        for (String part : LIST_SPLIT.split(value.replaceAll("^\\[|\\]$", ""))) {
            String tag = cleanTag(part);
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    /** The tags of the front matter block: block list, inline list and bare list. */
    static List<String> frontMatterTags(String text) {
        String[] lines = text.split("\n", -1);
        if (!Js.trim(lines[0]).equals("---")) {
            return List.of();
        }
        int end = -1;
        for (int i = 1; i < lines.length && end < 0; i++) {
            if (Js.trim(lines[i]).equals("---")) {
                end = i;
            }
        }
        if (end < 0) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        boolean inList = false;
        for (int i = 1; i < end; i++) {
            String line = lines[i];
            Matcher key = KEY.matcher(line);
            if (key.find()) {
                inList = false;
                if (!key.group(1).toLowerCase(Locale.ROOT).equals("tags")) {
                    continue;
                }
                String value = Js.trim(key.group(2));
                if (value.isEmpty()) {
                    inList = true;
                } else {
                    tags.addAll(splitInlineList(value));
                }
                continue;
            }
            if (!inList) {
                continue;
            }
            Matcher item = ITEM.matcher(line);
            if (item.find()) {
                tags.add(cleanTag(item.group(1)));
            } else if (!Js.trim(line).isEmpty()) {
                inList = false;
            }
        }
        tags.removeIf(String::isEmpty);
        return tags;
    }

    /** A {@code #tag} in the body, outside the front matter block, in any letter case. */
    static boolean hasInlineTag(String text, String tag) {
        String body = FRONT_MATTER_BLOCK.matcher(text).replaceFirst("");
        Pattern pattern = Pattern.compile("(^|\\s)#" + Pattern.quote(tag) + "(\\s|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
        return pattern.matcher(body).find();
    }
}
