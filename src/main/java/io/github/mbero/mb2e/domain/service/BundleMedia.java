package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the media a published bundle addresses: the {@code <img>} src values of
 * {@code assets/js/content.js} that begin {@code assets/media/}, prefix stripped and
 * percent-decoded, in bundle order. Any other src is ignored.
 */
final class BundleMedia {

    static final String PREFIX = "assets/media/";

    private static final Pattern IMG_SRC = Pattern.compile("<img\\b[^>]*?\\bsrc=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    /** One addressed file: its datasource-relative path and the first note that shows it. */
    record Ref(String path, String noteId) {
    }

    private BundleMedia() {
    }

    /**
     * Parses the media references of a bundle text.
     * @throws IllegalArgumentException when the text is not a content bundle
     */
    static List<Ref> parse(String bundleText) {
        int start = bundleText.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("no JSON object found");
        }
        Object root = MiniJson.parse(bundleText, start);
        if (!(root instanceof Map<?, ?> bundle) || !(bundle.get("notes") instanceof Map<?, ?> notes)) {
            throw new IllegalArgumentException("no notes object found");
        }
        Map<String, Ref> refs = new LinkedHashMap<>();
        for (Map.Entry<?, ?> note : notes.entrySet()) {
            if (!(note.getValue() instanceof Map<?, ?> entry)) {
                continue;
            }
            for (String field : new String[] {"lead", "html"}) {
                if (entry.get(field) instanceof String html) {
                    collect(String.valueOf(note.getKey()), html, refs);
                }
            }
        }
        return new ArrayList<>(refs.values());
    }

    private static void collect(String noteId, String html, Map<String, Ref> refs) {
        Matcher matcher = IMG_SRC.matcher(html);
        while (matcher.find()) {
            String src = matcher.group(1).replace("&amp;", "&");
            if (src.startsWith(PREFIX)) {
                String path = Js.decodeSafe(src.substring(PREFIX.length()));
                refs.putIfAbsent(path, new Ref(path, noteId));
            }
        }
    }
}
