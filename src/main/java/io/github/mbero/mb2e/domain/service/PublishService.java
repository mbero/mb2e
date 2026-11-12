package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Mirrors the assembled site into the target's working tree: index.html, .nojekyll, the template
 * assets, the generated data.js and the note images. It lists the git commands without running them.
 * Every guard runs and the whole plan, deletions included, is complete before the first write;
 * a dry run applies none of it.
 */
public class PublishService implements PublishUseCase {

    private static final String NOTE_IMAGES = TemplateCheck.NOTE_IMAGES;
    private static final String DATA_SCRIPT = "assets/js/data.js";
    private static final String STALE_DATASOURCE = "datasource";
    private static final String DS_STORE = ".DS_Store";
    private static final String COMMIT_MESSAGE = "chore(content): odśwież Knowledge Hub";

    private final BuildUseCase build;
    private final VaultReader vault;
    private final SiteSource site;
    private final PublishTarget target;
    private final GitRepository git;
    private final ConfiguredPaths paths;
    private final boolean force;

    public PublishService(BuildUseCase build, VaultReader vault, SiteSource site, PublishTarget target,
            GitRepository git, ConfiguredPaths paths, boolean force) {
        this.build = build;
        this.vault = vault;
        this.site = site;
        this.target = target;
        this.git = git;
        this.paths = paths;
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
        Assembly built = build.assemble(dryRun);
        if (built.report().isRefused()) {
            return PublishReport.refused(UUID.randomUUID().toString(), built.report().title(),
                    built.report().detail(), dryRun, target.location());
        }
        return publish(built, dryRun);
    }

    @Override
    public PublishReport publish(Assembly built, boolean dryRun) {
        return run(UUID.randomUUID().toString(), dryRun, built);
    }

    private PublishReport run(String traceId, boolean dryRun, Assembly built) {
        try {
            List<String> warnings = new ArrayList<>();
            Optional<PublishReport> refused = guard(traceId, dryRun, warnings);
            if (refused.isPresent()) {
                return refused.get();
            }
            String branch = git.branch();
            List<GitChange> before = git.status();
            if (!before.isEmpty()) {
                warnings.add("The publication target has uncommitted changes: "
                        + String.join(", ", before.stream().map(GitChange::path).toList())
                        + ". Site files will be overwritten; other files are left alone.");
            }

            Plan plan = planFor(built);
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
        if (paths.target() == null) {
            return Optional.of(refusal(traceId, dryRun, "Required path is not set",
                    "The property " + ConfiguredPaths.TARGET_KEY + " is not set."));
        }
        String path = target.location();
        PublishTarget.Kind kind = target.inspect();
        if (kind == PublishTarget.Kind.FILESYSTEM_ROOT) {
            return Optional.of(refusal(traceId, dryRun, "Illegal publication target",
                    "The filesystem root is never a publication target."));
        }
        Optional<String> overlap = target.overlap();
        if (overlap.isPresent()) {
            return Optional.of(refusal(traceId, dryRun, "Illegal publication target", overlap.get()));
        }
        switch (kind) {
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
            default -> {
                // fall through to the remote check
            }
        }
        Optional<String> origin = git.originUrl();
        if (origin.isEmpty()) {
            return Optional.of(refusal(traceId, dryRun, "Publication target has no origin remote",
                    "The git working copy '" + path + "' has no remote named origin."));
        }
        if (!RemoteGuard.isPagesRepository(origin.get())) {
            String expected = "origin is " + origin.get() + "; expected https://github.com/<owner>/<owner>.github.io"
                    + " or git@github.com:<owner>/<owner>.github.io, with or without .git";
            if (!force) {
                return Optional.of(refusal(traceId, dryRun, "Publication target is not the expected repository",
                        expected));
            }
            warnings.add("Remote " + origin.get() + " is not a GitHub Pages repository of its owner"
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

    private Plan planFor(Assembly built) {
        Map<String, byte[]> source = new TreeMap<>();
        source.put("index.html", site.read("index.html"));
        source.put(".nojekyll", new byte[0]);
        for (String file : site.files("assets")) {
            if (!isDsStore(file) && !isMarkdown(file) && !file.equals(DATA_SCRIPT)) {
                source.put(file, site.read(file));
            }
        }
        source.put(DATA_SCRIPT, built.site().dataScript().getBytes(StandardCharsets.UTF_8));
        List<String> media = new ArrayList<>(built.site().images());
        for (String image : media) {
            source.put(NOTE_IMAGES + "/" + image, vault.readFile(image));
        }

        Map<String, byte[]> writes = new TreeMap<>();
        List<String> notes = new ArrayList<>();
        source.forEach((path, bytes) -> copyIfDifferent(path, bytes, writes));

        Map<String, String> sourceByForm = byForm(source.keySet());
        List<String> deleted = new ArrayList<>();
        List<String> mediaDeleted = new ArrayList<>();
        for (String file : target.files("assets")) {
            if (isDsStore(file) || source.containsKey(file)) {
                continue;
            }
            if (rewriteTwin(file, sourceByForm, source, writes, notes)) {
                continue;
            }
            if (file.startsWith(NOTE_IMAGES + "/")) {
                mediaDeleted.add(file.substring(NOTE_IMAGES.length() + 1));
            } else {
                deleted.add(file);
            }
        }

        boolean stale = target.exists(STALE_DATASOURCE);
        if (stale) {
            target.files(STALE_DATASOURCE).forEach(deleted::add);
        }
        deleted.sort(null);
        writes.keySet().forEach(target::verifyWritable);
        return new Plan(writes, deleted, stale, media, mediaDeleted, built.report().missingAssets(), notes);
    }

    /**
     * A stale target file that is the very file of a site file on this file system, whose name
     * differs only in case or Unicode form, is not deleted: the site file is written over it.
     */
    private boolean rewriteTwin(String stale, Map<String, String> sourceByForm, Map<String, byte[]> source,
            Map<String, byte[]> writes, List<String> notes) {
        String twin = sourceByForm.get(form(stale));
        if (twin == null || !target.sameFile(stale, twin)) {
            return false;
        }
        writes.put(twin, source.get(twin));
        notes.add(stale + " differs from " + twin
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

    private static boolean isDsStore(String file) {
        return file.substring(file.lastIndexOf('/') + 1).equals(DS_STORE);
    }

    private static Set<String> untracked(List<GitChange> changes) {
        Set<String> paths = new HashSet<>();
        changes.stream().filter(GitChange::isUntracked).map(GitChange::path).forEach(paths::add);
        return paths;
    }

    private void copyIfDifferent(String path, byte[] bytes, Map<String, byte[]> writes) {
        if (!target.hasContent(path, bytes)) {
            writes.put(path, bytes);
        }
    }

    // ---------- applying and reporting ----------

    private void apply(Plan plan) {
        plan.deleted().stream().filter(file -> !file.startsWith(STALE_DATASOURCE + "/")).forEach(target::delete);
        plan.mediaDeleted().forEach(file -> target.delete(NOTE_IMAGES + "/" + file));
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
        plan.mediaDeleted().forEach(file -> changed.remove(NOTE_IMAGES + "/" + file));
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
