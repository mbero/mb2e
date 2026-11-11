package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a front matter block: {@code key: value} lines and lists in square brackets,
 * nothing more. A value is a String or a List of values.
 */
final class FrontMatter {

    private static final Pattern LINE = Js.regex("^([A-Za-z0-9_-]+)\\s*:\\s*(.*)\\z");

    record Parsed(Map<String, Object> data, String body) {
    }

    private FrontMatter() {
    }

    static Object parseValue(String raw) {
        String value = Js.trim(raw);
        if (value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if (first == '[' && last == ']') {
            List<Object> items = new ArrayList<>();
            for (String item : value.substring(1, value.length() - 1).split(",", -1)) {
                Object parsed = parseValue(item);
                if (!"".equals(parsed)) {
                    items.add(parsed);
                }
            }
            return items;
        }
        if (value.length() >= 1 && ((first == '"' && last == '"') || (first == '\'' && last == '\''))) {
            return value.length() >= 2 ? value.substring(1, value.length() - 1) : "";
        }
        return value;
    }

    static Parsed parse(String raw) {
        String text = raw.replaceFirst("^﻿", "").replaceAll("\r\n?", "\n");
        String[] lines = text.split("\n", -1);
        if (!Js.trim(lines[0]).equals("---")) {
            return new Parsed(new HashMap<>(), text);
        }
        int end = -1;
        for (int i = 1; i < lines.length; i++) {
            if (Js.trim(lines[i]).equals("---")) {
                end = i;
                break;
            }
        }
        if (end == -1) {
            return new Parsed(new HashMap<>(), text);
        }
        Map<String, Object> data = new HashMap<>();
        for (int j = 1; j < end; j++) {
            Matcher match = LINE.matcher(lines[j]);
            if (match.find()) {
                data.put(match.group(1), parseValue(match.group(2)));
            }
        }
        return new Parsed(data, String.join("\n", java.util.Arrays.asList(lines).subList(end + 1, lines.length)));
    }
}
