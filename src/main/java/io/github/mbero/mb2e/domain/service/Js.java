package io.github.mbero.mb2e.domain.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavaScript string behaviours the renderer depends on: trim, replace with a function,
 * encodeURIComponent and decodeURIComponent. Patterns use \s as JavaScript does (Unicode
 * white space), and a digit is written [0-9] because JavaScript's \d is ASCII only.
 */
final class Js {

    private static final String UNRESERVED = "-_.!~*'()";

    private Js() {
    }

    static Pattern regex(String pattern) {
        return Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS);
    }

    static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '﻿';
    }

    static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    static boolean isBlank(String value) {
        return trim(value).isEmpty();
    }

    /** Emulates String.replace with a global regex and a function; the function's result is inserted literally. */
    static String replace(Pattern pattern, String input, Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(input, last, matcher.start()).append(replacement.apply(matcher));
            last = matcher.end();
        }
        return out.append(input, last, input.length()).toString();
    }

    static boolean truthy(String value) {
        return value != null && !value.isEmpty();
    }

    static String encodeUriComponent(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || (c < 0x80 && UNRESERVED.indexOf(c) >= 0)) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return out.toString();
    }

    /** Applies decodeURIComponent, returning the input unchanged where JavaScript would throw. */
    static String decodeSafe(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    return value;
                }
                int hi = Character.digit(value.charAt(i + 1), 16);
                int lo = Character.digit(value.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return value;
                }
                bytes.write(hi * 16 + lo);
                i += 2;
            } else {
                byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                if (Character.isHighSurrogate(c) && i + 1 < value.length()) {
                    encoded = value.substring(i, i + 2).getBytes(StandardCharsets.UTF_8);
                    i++;
                }
                bytes.write(encoded, 0, encoded.length);
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException failure) {
            return value;
        }
    }
}
