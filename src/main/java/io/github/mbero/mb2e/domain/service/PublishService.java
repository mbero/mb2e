package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.model.PublishInputs;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import io.github.mbero.mb2e.domain.port.out.DatasourceFiles;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies index.html, .nojekyll and assets into the target's working tree, fills assets/media
 * from the datasource, and lists the git commands without running them. Every guard runs and the
 * whole plan, deletions included, is complete before the first write; a dry run applies none of it.
 */
public class PublishService implements PublishUseCase {

    private static final String MEDIA_DIRECTORY = "assets/media";
    private static final String STALE_DATASOURCE = "datasource";
    private static final String DS_STORE = ".DS_Store";
    private static final String COMMIT_MESSAGE = "chore(content): odśwież Knowledge Hub";
    private static final Pattern WIKI_EMBED = Js.regex("!\\[\\[([^\\]|]+?)(?:\\|[^\\]]*)?\\]\\]");

    private final SiteSource site;
    private final PublishTarget target;
    private final GitRepository git;
    private final DatasourceFiles datasource;
    private final String expectedRemote;
    private final boolean force;

    public PublishService(SiteSource site, PublishTarget target, GitRepository git, DatasourceFiles datasource,
            String expectedRemote, boolean force) {
        this.site = site;
        this.target = target;
        this.git = git;
        this.datasource = datasource;
        this.expectedRemote = expectedRemote;
        this.force = force;
    }

    /** What a publication will do, complete before anything is done. */
    private record Plan(Map<String, byte[]> writes, List<String> deleted, boolean staleDirectory,
            List<String> media, List<String> mediaDeleted, List<String> missingMedia, List<String> notes) {

        boolean changesNothing() {
            return writes.isEmpty() && deleted.isEmpty() && !staleDirectory && mediaDeleted.isEmpty();
        }
    }

    @Override
    public PublishReport publish(boolean dryRun) {
        return run(UUID.randomUUID().toString(), dryRun, PublishInputs.NONE);
    }

    @Override
    public PublishReport plan(PublishInputs inputs) {
        return run(UUID.randomUUID().toString(), true, inputs);
    }

    @Override
    public Optional<PublishReport> checkTarget() {
        String traceId = UUID.randomUUID().toString();
        try {
            return guard(traceId, false, new ArrayList<>());
        } catch (GitFailure failure) {
            return Optional.of(gitRefusal(traceId, false, failure));
        }
    }

    private PublishReport run(String traceId, boolean dryRun, PublishInputs inputs) {
        try {
            List<String> warnings = new ArrayList<>();
            Optional<PublishReport> refused = guard(traceId, dryRun, warnings);
            if (refused.isPresent()) {
                return refused.get();
            }
            Optional<TemplateCheck.Refusal> incomplete = TemplateCheck.publishable(site, inputs.bundleText());
            if (incomplete.isPresent()) {
                return refusal(traceId, dryRun, incomplete.get().title(), incomplete.get().detail());
            }
            String branch = git.branch();
            List<GitChange> before = git.status();
            if (!before.isEmpty()) {
                warnings.add("The publication target has uncommitted changes: "
                        + String.join(", ", before.stream().map(GitChange::path).toList())
                        + ". Site files will be overwritten; other files are left alone.");
            }

            Plan plan = planFor(inputs);
            warnings.addAll(plan.notes());
            boolean upToDate;
            List<String> changed;
            if (dryRun) {
                upToDate = before.isEmpty() && plan.changesNothing();
                changed = plannedChanges(before, plan);
            } else {
                apply(plan);
                List<GitChange> after = git.status();
                upToDate = after.isEmpty();
                Set<String> alreadyThere = untracked(before);
                changed = after.stream().filter(change -> !change.isDeletion())
                        .filter(change -> !alreadyThere.contains(change.path())
                                || plan.writes().containsKey(change.path()))
                        .map(GitChange::path).sorted().toList();
            }
            return report(traceId, dryRun, branch, warnings, upToDate, changed, plan);
        } catch (GitFailure failure) {
            return gitRefusal(traceId, dryRun, failure);
        } catch (StoreFailure | IllegalArgumentException failure) {
            return refusal(traceId, dryRun, "The publication could not complete", failure.getMessage());
        }
    }

    // ---------- guards ----------

    private Optional<PublishReport> guard(String traceId, boolean dryRun, List<String> warnings) {
        String path = target.location();
        switch (target.inspect()) {
            case FILESYSTEM_ROOT -> {
                return Optional.of(refusal(traceId, dryRun, "Illegal publication target",
                        "The filesystem root is never a publication target."));
            }
            case TEMPLATE_DIRECTORY -> {
                return Optional.of(refusal(traceId, dryRun, "Illegal publication target",
                        "The publication target is the template directory: " + path));
            }
            case MISSING -> {
                return Optional.of(refusal(traceId, dryRun, "Publication target not found",
                        "The publication target '" + path + "' does not exist."));
            }
            case NOT_A_DIRECTORY -> {
                return Optional.of(refusal(traceId, dryRun, "Publication target is not a directory",
                        "The publication target '" + path + "' is not a directory."));
            }
            case NOT_A_GIT_WORKING_COPY -> {
                return Optional.of(refusal(traceId, dryRun, "Publication target is not a git working copy",
                        "The publication target '" + path + "' is not a git working copy."));
            }
            case USABLE -> {
                // fall through to the remote check
            }
        }
        Optional<String> overlap = target.overlap();
        if (overlap.isPresent()) {
            return Optional.of(refusal(traceId, dryRun, "Illegal publication target", overlap.get()));
        }
        String origin = git.originUrl().orElse("(none)");
        if (!origin.contains(expectedRemote)) {
            if (!force) {
                return Optional.of(refusal(traceId, dryRun, "Publication target is not the expected repository",
                        "origin is " + origin + "; expected a remote containing " + expectedRemote));
            }
            warnings.add("Remote " + origin + " does not contain " + expectedRemote
                    + "; continuing because force is configured");
        }
        return Optional.empty();
    }

    private PublishReport refusal(String traceId, boolean dryRun, String title, String detail) {
        return PublishReport.refused(traceId, title, detail, dryRun, target.location());
    }

    private PublishReport gitRefusal(String traceId, boolean dryRun, GitFailure failure) {
        return refusal(traceId, dryRun, "Git did not answer", failure.getMessage());
    }

    // ---------- the plan ----------

    private Plan planFor(PublishInputs inputs) {
        Map<String, byte[]> writes = new TreeMap<>();
        List<String> notes = new ArrayList<>();
        for (String file : new String[] {"index.html", ".nojekyll"}) {
            copyIfDifferent(file, site.read(file), writes);
        }

        Map<String, byte[]> source = new HashMap<>();
        List<String> sourceFiles = new ArrayList<>(site.files("assets"));
        if (inputs.bundleText() != null && !sourceFiles.contains(TemplateCheck.BUNDLE)) {
            sourceFiles.add(TemplateCheck.BUNDLE);
        }
        for (String file : sourceFiles) {
            if (!isMirrored(file)) {
                continue;
            }
            if (isMarkdown(file)) {
                notes.add("Not published: " + file + " is a Markdown source.");
                continue;
            }
            byte[] bytes = file.equals(TemplateCheck.BUNDLE) && inputs.bundleText() != null
                    ? inputs.bundleText().getBytes(StandardCharsets.UTF_8) : site.read(file);
            source.put(file, bytes);
            copyIfDifferent(file, bytes, writes);
        }
        List<String> deleted = new ArrayList<>();
        Map<String, String> sourceByForm = byForm(source.keySet());
        for (String file : target.files("assets")) {
            if (isMirrored(file) && !source.containsKey(file)) {
                if (!rewriteTwin(file, sourceByForm, source, "", writes, notes)) {
                    deleted.add(file);
                }
            }
        }

        String bundle = inputs.bundleText() != null ? inputs.bundleText()
                : new String(site.read(TemplateCheck.BUNDLE), StandardCharsets.UTF_8);
        Map<String, byte[]> media = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (BundleMedia.Ref ref : BundleMedia.parse(bundle)) {
            if (isMarkdown(ref.path())) {
                notes.add("Not published: " + MEDIA_DIRECTORY + "/" + ref.path() + " is a Markdown source.");
                continue;
            }
            Optional<byte[]> bytes = readDatasource(ref.path(), inputs);
            if (bytes.isPresent()) {
                media.put(ref.path(), bytes.get());
                copyIfDifferent(MEDIA_DIRECTORY + "/" + ref.path(), bytes.get(), writes);
            } else {
                missing.add(missingEntry(ref, inputs));
            }
        }
        List<String> mediaDeleted = new ArrayList<>();
        Map<String, String> mediaByForm = byForm(media.keySet());
        for (String file : target.files(MEDIA_DIRECTORY)) {
            String relative = file.substring(MEDIA_DIRECTORY.length() + 1);
            if (!media.containsKey(relative) && !relative.endsWith(DS_STORE)
                    && !rewriteTwin(relative, mediaByForm, media, MEDIA_DIRECTORY + "/", writes, notes)) {
                mediaDeleted.add(relative);
            }
        }

        boolean stale = target.exists(STALE_DATASOURCE);
        if (stale) {
            target.files(STALE_DATASOURCE).forEach(deleted::add);
        }
        deleted.sort(null);
        writes.keySet().forEach(target::verifyWritable);
        return new Plan(writes, deleted, stale, new ArrayList<>(media.keySet()), mediaDeleted, missing, notes);
    }

    /**
     * A stale target file that is the very file of a source file on this file system, whose name
     * differs only in case or Unicode form, is not deleted: the source file is written over it.
     */
    private boolean rewriteTwin(String stale, Map<String, String> sourceByForm, Map<String, byte[]> bytes,
            String prefix, Map<String, byte[]> writes, List<String> notes) {
        String twin = sourceByForm.get(form(stale));
        if (twin == null || !target.sameFile(prefix + stale, prefix + twin)) {
            return false;
        }
        writes.put(prefix + twin, bytes.get(twin));
        notes.add(prefix + stale + " differs from " + prefix + twin
                + " only in letter case or Unicode form; it is rewritten from the source, not deleted.");
        return true;
    }

    private static Map<String, String> byForm(Set<String> names) {
        Map<String, String> forms = new HashMap<>();
        names.forEach(name -> forms.putIfAbsent(form(name), name));
        return forms;
    }

    /** A name as a case-insensitive, normalization-insensitive file system sees it. */
    private static String form(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    private static boolean isMarkdown(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".md");
    }

    private static Set<String> untracked(List<GitChange> changes) {
        Set<String> paths = new HashSet<>();
        changes.stream().filter(GitChange::isUntracked).map(GitChange::path).forEach(paths::add);
        return paths;
    }

    /** A file the assets mirror owns: not .DS_Store (never copied, never deleted) and not under assets/media. */
    private static boolean isMirrored(String file) {
        return !file.startsWith(MEDIA_DIRECTORY + "/")
                && !file.substring(file.lastIndexOf('/') + 1).equals(DS_STORE);
    }

    private void copyIfDifferent(String path, byte[] bytes, Map<String, byte[]> writes) {
        if (!target.hasContent(path, bytes)) {
            writes.put(path, bytes);
        }
    }

    private Optional<byte[]> readDatasource(String path, PublishInputs inputs) {
        byte[] planned = inputs.datasourceFiles().get(path);
        return planned != null ? Optional.of(planned) : datasource.read(path);
    }

    /** Describes a missing medium: the note, the author's wikilink when the note is still readable, and the published path. */
    private String missingEntry(BundleMedia.Ref ref, PublishInputs inputs) {
        String published = MEDIA_DIRECTORY + "/" + ref.path();
        Optional<byte[]> note = readDatasource(ref.noteId() + ".md", inputs);
        if (note.isPresent()) {
            Optional<String> link = wikilinkFor(new String(note.get(), StandardCharsets.UTF_8), ref.path());
            if (link.isPresent()) {
                return ref.noteId() + " -> [[" + link.get() + "]] (" + published + ")";
            }
        }
        return ref.noteId() + " -> " + published;
    }

    private static Optional<String> wikilinkFor(String noteText, String path) {
        String wanted = path.toLowerCase(Locale.ROOT);
        String name = wanted.substring(wanted.lastIndexOf('/') + 1);
        Matcher embeds = WIKI_EMBED.matcher(noteText);
        while (embeds.find()) {
            String written = Js.trim(embeds.group(1));
            int hash = written.indexOf('#');
            String clean = (hash < 0 ? written : written.substring(0, hash)).toLowerCase(Locale.ROOT);
            if (clean.equals(wanted) || clean.equals(name) || wanted.endsWith("/" + clean)) {
                return Optional.of(written);
            }
        }
        return Optional.empty();
    }

    // ---------- applying and reporting ----------

    private void apply(Plan plan) {
        plan.deleted().stream().filter(file -> !file.startsWith(STALE_DATASOURCE + "/")).forEach(target::delete);
        plan.mediaDeleted().forEach(file -> target.delete(MEDIA_DIRECTORY + "/" + file));
        if (plan.staleDirectory()) {
            target.deleteTree(STALE_DATASOURCE);
        }
        plan.writes().forEach(target::write);
    }

    private static List<String> plannedChanges(List<GitChange> before, Plan plan) {
        Set<String> changed = new TreeSet<>(plan.writes().keySet());
        Set<String> alreadyThere = untracked(before);
        before.stream().filter(change -> !change.isDeletion()).map(GitChange::path)
                .filter(path -> !alreadyThere.contains(path) || plan.writes().containsKey(path))
                .forEach(changed::add);
        plan.deleted().forEach(changed::remove);
        plan.mediaDeleted().forEach(file -> changed.remove(MEDIA_DIRECTORY + "/" + file));
        return new ArrayList<>(changed);
    }

    private PublishReport report(String traceId, boolean dryRun, String branch, List<String> warnings,
            boolean upToDate, List<String> changed, Plan plan) {
        List<String> commands = upToDate ? List.of() : commands(branch);
        String message = upToDate ? "The site is already up to date." : null;
        return new PublishReport(traceId, PublishReport.OK, null, null, dryRun, target.location(), branch,
                message, warnings, upToDate ? List.of() : changed, commands, plan.deleted(), plan.media(),
                plan.mediaDeleted(), plan.missingMedia());
    }

    private List<String> commands(String branch) {
        String directory = shellWord(target.location());
        return List.of("git -C " + directory + " add -A",
                "git -C " + directory + " commit -m \"" + COMMIT_MESSAGE + "\"",
                "git -C " + directory + " push origin " + branch);
    }

    /** The path as typed, quoted only when a shell would split or expand it. */
    private static String shellWord(String path) {
        return path.matches("[A-Za-z0-9_@%+=:,./-]+") ? path : "'" + path.replace("'", "'\\''") + "'";
    }
}
