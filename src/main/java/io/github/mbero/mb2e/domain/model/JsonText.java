package io.github.mbero.mb2e.domain.model;

/**
 * Writes JSON text exactly as JavaScript's {@code JSON.stringify} does: non-ASCII and "/"
 * are not escaped, control characters are written as lowercase \\u00xx.
 */
public final class JsonText {

    private JsonText() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    boolean loneSurrogate = Character.isSurrogate(c) && !pairedSurrogate(value, i);
                    if (c < 0x20 || loneSurrogate) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static boolean pairedSurrogate(String value, int i) {
        char c = value.charAt(i);
        if (Character.isHighSurrogate(c)) {
            return i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1));
        }
        return i > 0 && Character.isHighSurrogate(value.charAt(i - 1));
    }
}
