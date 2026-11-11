package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the one JSON value at the start of a text; objects keep their key order and numbers
 * stay as text. It lets the domain read the bundle back without a JSON library.
 */
final class MiniJson {

    private final String text;
    private int at;

    private MiniJson(String text, int at) {
        this.text = text;
        this.at = at;
    }

    /** Parses the value that starts at {@code from}; anything after it is ignored. */
    static Object parse(String text, int from) {
        MiniJson reader = new MiniJson(text, from);
        return reader.value();
    }

    private Object value() {
        space();
        if (at >= text.length()) {
            throw fail("a value");
        }
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            default -> literal();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> out = new LinkedHashMap<>();
        at++;
        space();
        if (peek() == '}') {
            at++;
            return out;
        }
        while (true) {
            space();
            if (peek() != '"') {
                throw fail("a key");
            }
            String key = string();
            space();
            expect(':');
            out.put(key, value());
            space();
            if (peek() == ',') {
                at++;
            } else {
                expect('}');
                return out;
            }
        }
    }

    private List<Object> array() {
        List<Object> out = new ArrayList<>();
        at++;
        space();
        if (peek() == ']') {
            at++;
            return out;
        }
        while (true) {
            out.add(value());
            space();
            if (peek() == ',') {
                at++;
            } else {
                expect(']');
                return out;
            }
        }
    }

    private String string() {
        StringBuilder out = new StringBuilder();
        at++;
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (at >= text.length()) {
                break;
            }
            char escape = text.charAt(at++);
            switch (escape) {
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (at + 4 > text.length()) {
                        throw fail("four hex digits");
                    }
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                }
                default -> out.append(escape);
            }
        }
        throw fail("the end of a string");
    }

    private Object literal() {
        int start = at;
        while (at < text.length() && ",]} \t\r\n".indexOf(text.charAt(at)) < 0) {
            at++;
        }
        String word = text.substring(start, at);
        return switch (word) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            case "null" -> null;
            default -> {
                if (word.isEmpty()) {
                    throw fail("a value");
                }
                yield word;
            }
        };
    }

    private void space() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
            at++;
        }
    }

    private char peek() {
        return at < text.length() ? text.charAt(at) : '\0';
    }

    private void expect(char c) {
        if (peek() != c) {
            throw fail("'" + c + "'");
        }
        at++;
    }

    private IllegalArgumentException fail(String wanted) {
        return new IllegalArgumentException("Expected " + wanted + " at character " + at);
    }
}
