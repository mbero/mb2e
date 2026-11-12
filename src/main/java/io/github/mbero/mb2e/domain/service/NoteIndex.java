package io.github.mbero.mb2e.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves links and attachments, and keeps the build's counters and lists.
 * <p>A link resolves by full note id, then by bare file name. An attachment resolves relative to
 * the note's folder, then to the vault root, then by bare file name; all case-insensitively.
 * Attachment addresses begin {@code assets/img/notes/}.
 */
final class NoteIndex {

    static final String MEDIA_PREFIX = "assets/img/notes/";

    record Entry(String id, String title) {
    }

    private final Map<String, Entry> byId = new HashMap<>();
    private final Map<String, Entry> byBasename = new HashMap<>();
    private final Map<String, String> assetById = new HashMap<>();
    private final Map<String, String> assetByBasename = new HashMap<>();
    private final Set<String> attachments = new LinkedHashSet<>();
    private final List<String> missingLinks = new ArrayList<>();
    private final List<String> missingAssets = new ArrayList<>();

    NoteIndex(List<Entry> notes, List<String> assets) {
        for (Entry note : notes) {
            byId.put(lower(note.id()), note);
            byBasename.putIfAbsent(lower(lastSegment(note.id())), note);
        }
        for (String rel : assets) {
            assetById.put(lower(rel), rel);
            assetByBasename.putIfAbsent(lower(lastSegment(rel)), rel);
        }
    }

    /** The vault paths of the attachments resolved so far, each once, in the order they were first embedded. */
    List<String> attachments() {
        return new ArrayList<>(attachments);
    }

    List<String> missingLinks() {
        return missingLinks;
    }

    List<String> missingAssets() {
        return missingAssets;
    }

    /** The resolver for the text of one note: a miss is listed under that note's id. */
    LinkResolver forNote(String noteId) {
        int slash = noteId.lastIndexOf('/');
        String folder = slash < 0 ? "" : noteId.substring(0, slash + 1);
        return new LinkResolver() {
            @Override
            public Optional<NoteLink> resolveNote(String target) {
                String key = lower(stripMarkdownExtension(normalise(target)));
                if (key.isEmpty()) {
                    return Optional.empty();
                }
                Entry note = byId.get(key);
                if (note == null) {
                    note = byBasename.get(lastSegment(key));
                }
                if (note == null) {
                    missingLinks.add(noteId + " -> [[" + target + "]]");
                    return Optional.empty();
                }
                return Optional.of(new NoteLink("#/" + encodePath(note.id()), note.title()));
            }

            @Override
            public Optional<String> resolveAsset(String target) {
                String key = lower(normalise(target));
                if (key.isEmpty() || key.endsWith(".md")) {
                    return Optional.empty();
                }
                String rel = assetById.get(lower(folder) + key);
                if (rel == null) {
                    rel = assetById.get(key);
                }
                if (rel == null) {
                    rel = assetByBasename.get(lastSegment(key));
                }
                if (rel == null) {
                    missingAssets.add(noteId + " -> [[" + target + "]]");
                    return Optional.empty();
                }
                attachments.add(rel);
                return Optional.of(MEDIA_PREFIX + encodePath(rel));
            }
        };
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    static String lastSegment(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static String stripMarkdownExtension(String value) {
        return lower(value).endsWith(".md") ? value.substring(0, value.length() - 3) : value;
    }

    /** Normalises a link target: no #anchor, no leading ./ or /, trimmed. */
    private static String normalise(String target) {
        String value = target;
        int hash = value.indexOf('#');
        if (hash >= 0) {
            value = value.substring(0, hash);
        }
        if (value.startsWith("./")) {
            value = value.substring(2);
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return Js.trim(value);
    }

    static String encodePath(String id) {
        String[] segments = id.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            segments[i] = Js.encodeUriComponent(segments[i]);
        }
        return String.join("/", segments);
    }
}
