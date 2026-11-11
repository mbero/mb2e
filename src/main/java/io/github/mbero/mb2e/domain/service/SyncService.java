package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.SyncPlan;
import io.github.mbero.mb2e.domain.model.SyncReport;
import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.VaultNote;
import io.github.mbero.mb2e.domain.port.in.SyncUseCase;
import io.github.mbero.mb2e.domain.port.out.DatasourceMirror;
import io.github.mbero.mb2e.domain.port.out.DatasourceMirror.Presence;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies the vault notes that carry the tag into the datasource, with their attachments and links.
 * <p>Only what is new or different is written, and nothing is ever deleted. Reported paths are
 * in one fixed order: notes by path, then attachments by path.
 */
public class SyncService implements SyncUseCase {

    private static final Pattern KEY = Js.regex("^([A-Za-z0-9_-]+)\\s*:\\s*(.*)$");
    private static final Pattern ITEM = Js.regex("^\\s*-\\s*(.+?)\\s*$");
    private static final Pattern LIST_SPLIT = Js.regex("[,\\s]+");
    private static final Pattern WIKI_EMBED = Js.regex("!\\[\\[([^\\]|]+?)(?:\\|[^\\]]*)?\\]\\]");
    private static final Pattern MARKDOWN_EMBED = Js.regex(
            "!\\[[^\\]]*\\]\\(\\s*([^)]+?)(?:\\s+(?:\"[^\"]*\"|'[^']*'))?\\s*\\)");
    private static final Pattern NOTE_LINK = Js.regex("(^|[^!])\\[\\[([^\\]|]+?)(?:\\|[^\\]]*)?\\]\\]");
    private static final Pattern SCHEME = Pattern.compile("^[a-z]+:", Pattern.CASE_INSENSITIVE);
    private static final Pattern MD_SUFFIX = Pattern.compile("\\.md$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRONT_MATTER_BLOCK = Pattern.compile("\\A---\\n.*?\\n---\\n", Pattern.DOTALL);

    private final VaultReader vaultReader;
    private final DatasourceMirror mirror;
    private final String configuredTag;
    private final boolean inlineTags;

    public SyncService(VaultReader vaultReader, DatasourceMirror mirror, String configuredTag,
            boolean inlineTags) {
        this.vaultReader = vaultReader;
        this.mirror = mirror;
        this.configuredTag = configuredTag;
        this.inlineTags = inlineTags;
    }

    private record Note(String path, byte[] bytes, String text) {
    }

    @Override
    public SyncReport sync(boolean dryRun) {
        SyncPlan planned = execute(dryRun);
        if (!dryRun) {
            try {
                planned.writes().forEach(mirror::write);
            } catch (StoreFailure failure) {
                return SyncReport.refused(planned.report().traceId(), "The sync could not complete",
                        failure.getMessage(), false, null, 0);
            }
        }
        return planned.report();
    }

    @Override
    public SyncPlan plan() {
        return execute(true);
    }

    /** Reads and decides; writes nothing. The caller writes {@code writes()} when the run is real. */
    private SyncPlan execute(boolean dryRun) {
        String traceId = UUID.randomUUID().toString();
        try {
            return run(traceId, dryRun);
        } catch (StoreFailure failure) {
            return new SyncPlan(SyncReport.refused(traceId, "The sync could not complete", failure.getMessage(),
                    dryRun, null, 0), Map.of());
        }
    }

    private static SyncPlan refused(SyncReport report) {
        return new SyncPlan(report, Map.of());
    }

    private SyncPlan run(String traceId, boolean dryRun) {
        Vault vault = vaultReader.read();
        if (vault.status() == Vault.Status.MISSING) {
            return refused(SyncReport.refused(traceId, "Vault directory not found",
                    "The vault directory '" + vault.path() + "' does not exist.", dryRun, vault.path(), 0));
        }
        if (vault.status() == Vault.Status.NOT_A_DIRECTORY) {
            return refused(SyncReport.refused(traceId, "Vault path is not a directory",
                    "The vault path '" + vault.path() + "' is not a directory.", dryRun, vault.path(), 0));
        }

        String tag = cleanTag(configuredTag);
        List<Note> selected = new ArrayList<>();
        for (VaultNote note : vault.notes()) {
            String text = normalise(new String(note.bytes(), StandardCharsets.UTF_8));
            if (readFrontMatterTags(text).contains(tag) || (inlineTags && hasInlineTag(text, tag))) {
                selected.add(new Note(note.path(), note.bytes(), text));
            }
        }
        int scanned = vault.notes().size();
        if (selected.isEmpty()) {
            return refused(SyncReport.refused(traceId, "No note carries the selection tag",
                    "No note in the vault carries the tag '" + tag + "'.", dryRun, vault.path(), scanned));
        }
        selected.sort(Comparator.comparing(Note::path));

        // attachments: first the one with the same path, then the first with the same file name
        Map<String, String> assetsByPath = new HashMap<>();
        Map<String, String> assetsByName = new HashMap<>();
        for (String file : vault.files().stream().sorted().toList()) {
            assetsByPath.put(lower(file), file);
            assetsByName.putIfAbsent(lower(file.substring(file.lastIndexOf('/') + 1)), file);
        }
        Map<String, Boolean> wanted = new TreeMap<>();
        List<String> missingAssets = new ArrayList<>();
        for (Note note : selected) {
            int slash = note.path().lastIndexOf('/');
            String dir = slash < 0 ? "." : note.path().substring(0, slash);
            for (String raw : embeds(note.text())) {
                String target = cleanTarget(raw);
                if (target.isEmpty() || SCHEME.matcher(target).find() || MD_SUFFIX.matcher(target).find()) {
                    continue;
                }
                String found = null;
                for (String candidate : new String[] {dir.equals(".") ? target : dir + "/" + target, target}) {
                    if (found == null) {
                        found = assetsByPath.get(lower(candidate.replaceFirst("^/", "")));
                    }
                }
                if (found == null) {
                    found = assetsByName.get(lower(target.substring(target.lastIndexOf('/') + 1)));
                }
                if (found != null) {
                    wanted.put(found, Boolean.TRUE);
                } else {
                    missingAssets.add(note.path() + " -> " + target);
                }
            }
        }

        // links to a note that is not itself selected
        Set<String> published = new HashSet<>();
        for (Note note : selected) {
            String name = MD_SUFFIX.matcher(note.path()).replaceFirst("");
            published.add(lower(name));
            published.add(lower(name.substring(name.lastIndexOf('/') + 1)));
        }
        List<String> brokenLinks = new ArrayList<>();
        for (Note note : selected) {
            Matcher links = NOTE_LINK.matcher(note.text());
            while (links.find()) {
                String clean = cleanTarget(links.group(2));
                String key = lower(MD_SUFFIX.matcher(clean).replaceFirst(""));
                if (key.isEmpty() || published.contains(key)
                        || published.contains(key.substring(key.lastIndexOf('/') + 1))) {
                    continue;
                }
                brokenLinks.add(note.path() + " -> [[" + clean + "]]");
            }
        }

        // the plan: compare first, write after, and only what is new or different
        List<String> added = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        int unchanged = 0;
        Map<String, byte[]> toWrite = new TreeMap<>();
        for (Note note : selected) {
            Presence presence = mirror.compare(note.path(), note.bytes());
            unchanged += plan(presence, note.path(), note.bytes(), added, updated, toWrite);
        }
        List<String> attachments = new ArrayList<>(wanted.keySet());
        for (String path : attachments) {
            byte[] bytes = vaultReader.readFile(path);
            unchanged += plan(mirror.compare(path, bytes), path, bytes, added, updated, toWrite);
        }
        return new SyncPlan(new SyncReport(traceId, SyncReport.OK, null, null, dryRun, vault.path(), scanned,
                selected.size(), added, updated, unchanged, attachments, missingAssets, brokenLinks), toWrite);
    }

    private static int plan(Presence presence, String path, byte[] bytes, List<String> added,
            List<String> updated, Map<String, byte[]> toWrite) {
        switch (presence) {
            case ABSENT -> added.add(path);
            case DIFFERENT -> updated.add(path);
            case SAME -> {
                return 1;
            }
        }
        toWrite.put(path, bytes);
        return 0;
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalise(String raw) {
        return raw.replaceFirst("^﻿", "").replaceAll("\r\n?", "\n");
    }

    static String cleanTag(String value) {
        String s = Js.trim(value).replaceAll("^['\"]|['\"]$", "").replaceFirst("^#", "");
        return lower(Js.trim(s));
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
    static List<String> readFrontMatterTags(String text) {
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
                if (!lower(key.group(1)).equals("tags")) {
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

    private static List<String> embeds(String text) {
        List<String> targets = new ArrayList<>();
        Matcher wiki = WIKI_EMBED.matcher(text);
        while (wiki.find()) {
            targets.add(wiki.group(1));
        }
        Matcher markdown = MARKDOWN_EMBED.matcher(text);
        while (markdown.find()) {
            targets.add(markdown.group(1));
        }
        return targets;
    }

    private static String cleanTarget(String target) {
        int hash = target.indexOf('#');
        String s = hash < 0 ? target : target.substring(0, hash);
        return Js.decodeSafe(Js.trim(s.replaceFirst("^\\./", "")));
    }
}
