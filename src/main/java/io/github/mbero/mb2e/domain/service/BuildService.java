package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GeneratedSite;
import io.github.mbero.mb2e.domain.model.NoteEntry;
import io.github.mbero.mb2e.domain.model.SiteData;
import io.github.mbero.mb2e.domain.model.TreeNode;
import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.VaultNote;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Selects the tagged notes of the vault, renders them and assembles the site in memory.
 * <p>The steps are the configuration and template checks, the vault scan, the selection, the tree,
 * two passes over the notes, the start note and the report. Nothing is written anywhere.
 */
public class BuildService implements BuildUseCase {

    static final String ABOUT_ID = "about";

    private final VaultReader vaultReader;
    private final SiteSource site;
    private final ConfiguredPaths paths;
    private final String tag;
    private final boolean inlineTags;
    private final String configuredDefaultNote;
    private final Comparator<String> order = new PolishOrder();

    public BuildService(VaultReader vaultReader, SiteSource site, ConfiguredPaths paths, String tag,
            boolean inlineTags, String configuredDefaultNote) {
        this.vaultReader = vaultReader;
        this.site = site;
        this.paths = paths;
        this.tag = Selection.cleanTag(tag);
        this.inlineTags = inlineTags;
        this.configuredDefaultNote = configuredDefaultNote;
    }

    /** A folder of selected notes: its sub-folders and its notes, both in their sorted order. */
    private record Folder(String name, List<Folder> folders, List<Note> notes) {
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

    @Override
    public BuildReport build(boolean dryRun) {
        return assemble(dryRun).report();
    }

    @Override
    public Assembly assemble(boolean dryRun) {
        String traceId = UUID.randomUUID().toString();
        try {
            return run(traceId, dryRun);
        } catch (StoreFailure failure) {
            return refused(traceId, dryRun, "The build could not complete", failure.getMessage());
        }
    }

    private Assembly refused(String traceId, boolean dryRun, String title, String detail) {
        return new Assembly(BuildReport.refused(traceId, title, detail, dryRun, paths), null);
    }

    private Assembly run(String traceId, boolean dryRun) {
        Optional<String> unset = paths.unsetKey(false);
        if (unset.isPresent()) {
            return refused(traceId, dryRun, "Required path is not set", "The property " + unset.get()
                    + " is not set.");
        }
        Optional<TemplateCheck.Refusal> notATemplate = TemplateCheck.shape(site);
        if (notATemplate.isPresent()) {
            return refused(traceId, dryRun, notATemplate.get().title(), notATemplate.get().detail());
        }
        Vault vault = vaultReader.read();
        if (vault.status() == Vault.Status.MISSING) {
            return refused(traceId, dryRun, "Vault directory not found",
                    "The vault directory '" + vault.path() + "' does not exist.");
        }
        if (vault.status() == Vault.Status.NOT_A_DIRECTORY) {
            return refused(traceId, dryRun, "Vault path is not a directory",
                    "The vault path '" + vault.path() + "' is not a directory.");
        }

        List<Note> selected = select(vault);
        if (selected.isEmpty()) {
            return refused(traceId, dryRun, "No note carries the selection tag",
                    "No note in the vault carries the tag '" + tag + "'.");
        }
        Folder root = group(selected);
        List<Note> notes = flattenNotes(root, new ArrayList<>());

        // First pass: titles and front matter, needed to resolve [[links]].
        for (Note note : notes) {
            FrontMatter.Parsed parsed = FrontMatter.parse(note.text);
            note.front = parsed.data();
            note.body = parsed.body();
            note.title = titleOf(note);
        }
        List<String> files = vault.files().stream().sorted().toList();
        NoteIndex index = new NoteIndex(notes.stream().map(n -> new NoteIndex.Entry(n.id, n.title)).toList(), files);

        // Second pass: render every note into its one html block.
        Map<String, NoteEntry> entries = new LinkedHashMap<>();
        int empty = 0;
        for (Note note : notes) {
            List<Block> blocks = new ArrayList<>(MarkdownRenderer.render(note.body, index.forNote(note.id)));
            if (!blocks.isEmpty() && blocks.get(0).type().equals("h1")
                    && Js.trim(MarkdownRenderer.stripTags(blocks.get(0).html())).equals(note.title)) {
                blocks.remove(0);
            }
            StringBuilder html = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                html.append(i > 0 ? "\n" : "").append(blocks.get(i).html());
            }
            if (html.length() == 0) {
                empty++;
            }
            entries.put(note.id, new NoteEntry(note.title, note.folder,
                    note.front.get("date") instanceof String date ? date : "", tagsOf(note.front), html.toString()));
        }

        List<TreeNode> tree = buildTree(root);
        List<String> flat = treeOrder(tree, new ArrayList<>());

        List<String> warnings = new ArrayList<>();
        if (!entries.containsKey(ABOUT_ID)) {
            warnings.add("The about page is missing: no " + ABOUT_ID + ".md at the vault root carries the tag '"
                    + tag + "'.");
        }
        boolean configured = configuredDefaultNote != null && !configuredDefaultNote.isEmpty();
        String defaultNote = configured && entries.containsKey(configuredDefaultNote)
                ? configuredDefaultNote
                : (flat.isEmpty() ? "" : flat.get(0));
        if (configured && !configuredDefaultNote.equals(defaultNote)) {
            warnings.add("Configured start note '" + configuredDefaultNote + "' does not exist; using '"
                    + defaultNote + "'");
        }

        String script = SiteDataJson.script(new SiteData(defaultNote, tree, entries));
        List<String> images = index.attachments();
        BuildReport report = new BuildReport(traceId, BuildReport.OK, null, null, dryRun, paths.vault(),
                paths.template(), vault.notes().size(), notes.size(), images, empty, index.missingLinks(),
                index.missingAssets(), script.getBytes(StandardCharsets.UTF_8).length, defaultNote, warnings);
        return new Assembly(report, new GeneratedSite(script, images));
    }

    // ---------- selection ----------

    private List<Note> select(Vault vault) {
        List<Note> selected = new ArrayList<>();
        for (VaultNote note : vault.notes()) {
            String text = Selection.normalise(new String(note.bytes(), StandardCharsets.UTF_8));
            if (Selection.selects(text, tag, inlineTags)) {
                String path = note.path();
                selected.add(new Note(path.substring(0, path.length() - 3), parentOf(path), text));
            }
        }
        return selected;
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    // ---------- folders ----------

    /** Groups the selected notes by folder; a folder exists only because a selected note lies in or below it. */
    private Folder group(List<Note> notes) {
        Folder root = new Folder("", new ArrayList<>(), new ArrayList<>());
        for (Note note : notes) {
            Folder current = root;
            if (!note.folder.isEmpty()) {
                for (String name : note.folder.split("/")) {
                    current = child(current, name);
                }
            }
            current.notes().add(note);
        }
        sort(root);
        return root;
    }

    private static Folder child(Folder parent, String name) {
        for (Folder folder : parent.folders()) {
            if (folder.name().equals(name)) {
                return folder;
            }
        }
        Folder created = new Folder(name, new ArrayList<>(), new ArrayList<>());
        parent.folders().add(created);
        return created;
    }

    private void sort(Folder folder) {
        folder.folders().sort((a, b) -> order.compare(a.name(), b.name()));
        folder.notes().sort((a, b) -> order.compare(a.id, b.id));
        folder.folders().forEach(this::sort);
    }

    /** The notes in publication order: the notes of the sub-folders first, then the folder's own. */
    private static List<Note> flattenNotes(Folder node, List<Note> output) {
        node.folders().forEach(folder -> flattenNotes(folder, output));
        output.addAll(node.notes());
        return output;
    }

    // ---------- tree ----------

    private static List<TreeNode> buildTree(Folder node) {
        List<TreeNode> children = new ArrayList<>();
        for (Folder folder : node.folders()) {
            children.add(new TreeNode.Folder(folder.name(), buildTree(folder)));
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
}
