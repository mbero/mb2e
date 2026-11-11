package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.Bundle;
import io.github.mbero.mb2e.domain.model.BuildPlan;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.Datasource;
import io.github.mbero.mb2e.domain.model.NoteEntry;
import io.github.mbero.mb2e.domain.model.SourceNote;
import io.github.mbero.mb2e.domain.model.TreeNode;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.out.BundleStore;
import io.github.mbero.mb2e.domain.port.out.DatasourceReader;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the content bundle: scans the datasource, renders every note and hands the bundle
 * to the store. The steps are scan, tree, two passes over the notes, the start note, the report.
 */
public class BuildService implements BuildUseCase {

    static final String ABOUT_ID = "about";
    static final int WORDS_PER_MINUTE = 200;

    private static final DateTimeFormatter INSTANT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final DatasourceReader reader;
    private final BundleStore store;
    private final Clock clock;
    private final String configuredDefaultNote;
    private final SiteSource site;
    private final Comparator<String> order = new PolishOrder();

    public BuildService(DatasourceReader reader, BundleStore store, SiteSource site, Clock clock,
            String configuredDefaultNote) {
        this.reader = reader;
        this.site = site;
        this.store = store;
        this.clock = clock;
        this.configuredDefaultNote = configuredDefaultNote;
    }

    /** One scanned directory: sub-folders and notes in their sorted order, and the attachments. */
    private record Folder(String name, String path, Folder[] folders, List<Note> notes, List<String> assets) {
    }

    /** A note while the build works on it. */
    private static final class Note {
        final String id;
        final String folder;
        final String text;
        String title;
        String body;
        Map<String, Object> front;

        Note(String id, String folder, String text) {
            this.id = id;
            this.folder = folder;
            this.text = text;
        }
    }

    /** The report, and the bundle it describes (null when refused). */
    private record Built(BuildReport report, Bundle bundle) {
    }

    @Override
    public BuildReport build(boolean dryRun) {
        String traceId = UUID.randomUUID().toString();
        try {
            return run(traceId, dryRun, null).report();
        } catch (StoreFailure failure) {
            return BuildReport.refused(traceId, "The build could not complete", failure.getMessage());
        }
    }

    @Override
    public BuildPlan plan(Datasource datasource) {
        String traceId = UUID.randomUUID().toString();
        try {
            Built built = run(traceId, true, datasource);
            return new BuildPlan(built.report(), built.bundle() == null ? null : store.serialize(built.bundle()));
        } catch (StoreFailure failure) {
            return new BuildPlan(BuildReport.refused(traceId, "The build could not complete", failure.getMessage()),
                    null);
        }
    }

    private Built run(String traceId, boolean dryRun, Datasource given) {
        Optional<TemplateCheck.Refusal> notATemplate = TemplateCheck.shape(site);
        if (notATemplate.isPresent()) {
            return new Built(BuildReport.refused(traceId, notATemplate.get().title(),
                    notATemplate.get().detail()), null);
        }
        Datasource datasource = given != null ? given : reader.read();
        Folder root = scan(datasource);
        List<Note> notes = flattenNotes(root, new ArrayList<>());
        List<String> assets = flattenAssets(root, new ArrayList<>());

        if (notes.isEmpty()) {
            return new Built(BuildReport.refused(traceId, "Datasource holds no notes",
                    "No .md file was found in the datasource directory " + datasource.location()
                            + ", or the directory does not exist"), null);
        }

        // First pass: titles and front matter, needed to resolve [[links]].
        for (Note note : notes) {
            FrontMatter.Parsed parsed = FrontMatter.parse(note.text);
            note.front = parsed.data();
            note.body = parsed.body();
            note.title = titleOf(note);
        }

        NoteIndex index = new NoteIndex(notes.stream().map(n -> new NoteIndex.Entry(n.id, n.title)).toList(), assets);

        // Second pass: render every note.
        Map<String, NoteEntry> entries = new LinkedHashMap<>();
        int empty = 0;
        for (Note note : notes) {
            List<Block> blocks = new ArrayList<>(MarkdownRenderer.render(note.body, index.forNote(note.id)));

            if (!blocks.isEmpty() && blocks.get(0).type().equals("h1")) {
                String heading = Js.trim(MarkdownRenderer.stripTags(blocks.get(0).html()));
                if (heading.equals(note.title)) {
                    blocks.remove(0);
                }
            }

            String lead = "";
            if (!blocks.isEmpty() && blocks.get(0).type().equals("p")) {
                lead = MarkdownRenderer.unwrapParagraph(blocks.remove(0).html());
            }

            StringBuilder html = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                html.append(i > 0 ? "\n" : "").append(blocks.get(i).html());
            }
            if (lead.isEmpty() && html.length() == 0) {
                empty++;
            }

            entries.put(note.id, new NoteEntry(note.title, note.folder,
                    note.front.get("date") instanceof String date ? date : "", tagsOf(note.front),
                    Math.max(1, ceilDiv(countWords(lead + " " + html), WORDS_PER_MINUTE)),
                    lead, html.toString()));
        }

        List<TreeNode> tree = buildTree(root);
        List<String> flat = treeOrder(tree, new ArrayList<>());

        List<String> warnings = new ArrayList<>();
        boolean configured = configuredDefaultNote != null && !configuredDefaultNote.isEmpty();
        String defaultNote = configured && entries.containsKey(configuredDefaultNote)
                ? configuredDefaultNote
                : (flat.isEmpty() ? "" : flat.get(0));
        if (configured && !configuredDefaultNote.equals(defaultNote)) {
            warnings.add("Configured start note '" + configuredDefaultNote + "' does not exist; using '"
                    + defaultNote + "'");
        }

        Bundle bundle = new Bundle(INSTANT.format(clock.instant()), defaultNote,
                entries.containsKey(ABOUT_ID) ? ABOUT_ID : null, tree, flat, entries);
        long bytes = store.write(bundle, dryRun);

        return new Built(new BuildReport(traceId, BuildReport.OK, null, null, notes.size(), assets.size(), empty,
                index.missingLinks().size(), index.missingAssets().size(), bytes, defaultNote, warnings,
                index.missingLinks(), index.missingAssets()), bundle);
    }

    // ---------- scan ----------

    private static boolean hidden(String path) {
        for (String segment : path.split("/")) {
            if (segment.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private Folder scan(Datasource datasource) {
        Map<String, List<String>> subdirectories = new HashMap<>();
        Map<String, List<Note>> notesIn = new HashMap<>();
        Map<String, List<String>> assetsIn = new HashMap<>();

        java.util.Set<String> directories = new java.util.LinkedHashSet<>();
        for (String directory : datasource.directories()) {
            addWithParents(directories, directory);
        }
        for (SourceNote note : datasource.notes()) {
            if (!hidden(note.path())) {
                addWithParents(directories, parentOf(note.path()));
                String id = note.path().substring(0, note.path().length() - 3);
                notesIn.computeIfAbsent(parentOf(note.path()), k -> new ArrayList<>())
                        .add(new Note(id, parentOf(note.path()), note.text()));
            }
        }
        for (String asset : datasource.attachments()) {
            if (!hidden(asset)) {
                addWithParents(directories, parentOf(asset));
                assetsIn.computeIfAbsent(parentOf(asset), k -> new ArrayList<>()).add(asset);
            }
        }
        for (String directory : directories) {
            if (!directory.isEmpty() && !hidden(directory)) {
                subdirectories.computeIfAbsent(parentOf(directory), k -> new ArrayList<>()).add(directory);
            }
        }
        return folder("", "", subdirectories, notesIn, assetsIn);
    }

    private static void addWithParents(java.util.Set<String> directories, String directory) {
        String current = directory;
        while (!current.isEmpty() && directories.add(current)) {
            current = parentOf(current);
        }
    }

    private Folder folder(String name, String path, Map<String, List<String>> subdirectories,
            Map<String, List<Note>> notesIn, Map<String, List<String>> assetsIn) {
        List<String> children = new ArrayList<>(subdirectories.getOrDefault(path, List.of()));
        children.sort((a, b) -> order.compare(NoteIndex.lastSegment(a), NoteIndex.lastSegment(b)));
        Folder[] folders = children.stream()
                .map(child -> folder(NoteIndex.lastSegment(child), child, subdirectories, notesIn, assetsIn))
                .toArray(Folder[]::new);
        List<Note> notes = new ArrayList<>(notesIn.getOrDefault(path, List.of()));
        notes.sort((a, b) -> order.compare(a.id, b.id));
        List<String> assets = new ArrayList<>(assetsIn.getOrDefault(path, List.of()));
        assets.sort((a, b) -> order.compare(a, b));
        return new Folder(name, path, folders, notes, assets);
    }

    private static List<Note> flattenNotes(Folder node, List<Note> output) {
        Arrays.stream(node.folders()).forEach(f -> flattenNotes(f, output));
        output.addAll(node.notes());
        return output;
    }

    private static List<String> flattenAssets(Folder node, List<String> output) {
        Arrays.stream(node.folders()).forEach(f -> flattenAssets(f, output));
        output.addAll(node.assets());
        return output;
    }

    // ---------- tree ----------

    private static List<TreeNode> buildTree(Folder node) {
        List<TreeNode> children = new ArrayList<>();
        for (Folder folder : node.folders()) {
            children.add(new TreeNode.Folder(folder.name(), folder.path(), buildTree(folder)));
        }
        for (Note note : node.notes()) {
            if (!note.id.equals(ABOUT_ID)) {
                children.add(new TreeNode.Leaf(note.id));
            }
        }
        return children;
    }

    private static List<String> treeOrder(List<TreeNode> nodes, List<String> output) {
        for (TreeNode node : nodes) {
            switch (node) {
                case TreeNode.Leaf leaf -> output.add(leaf.note());
                case TreeNode.Folder folder -> treeOrder(folder.children(), output);
            }
        }
        return output;
    }

    // ---------- note ----------

    private static String titleOf(Note note) {
        if (note.front.get("title") instanceof String title && !Js.isBlank(title)) {
            return Js.trim(title);
        }
        return NoteIndex.lastSegment(note.id);
    }

    private static List<Object> tagsOf(Map<String, Object> front) {
        Object tags = front.get("tags");
        if (tags instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        List<Object> out = new ArrayList<>();
        if (tags instanceof String text && !Js.isBlank(text)) {
            for (String tag : Js.regex("[\\s,]+").split(text)) {
                if (!tag.isEmpty()) {
                    out.add(tag);
                }
            }
        }
        return out;
    }

    private static int countWords(String html) {
        int count = 0;
        for (String word : Js.regex("\\s+").split(MarkdownRenderer.stripTags(html))) {
            if (!word.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
