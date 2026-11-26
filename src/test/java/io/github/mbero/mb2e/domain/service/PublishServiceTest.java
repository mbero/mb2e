package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GeneratedSite;
import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublishServiceTest {

    private static final String PAGES = "git@github.com:x/x.github.io.git";
    private static final ConfiguredPaths PATHS = new ConfiguredPaths("/v", "/tpl", "/t");

    /** Returns the assembly that the test sets. */
    private static final class FakeBuild implements BuildUseCase {
        private Assembly next;

        @Override
        public BuildReport build(boolean dryRun) {
            throw new UnsupportedOperationException("publish assembles, it never asks for a report alone");
        }

        @Override
        public Assembly assemble(boolean dryRun) {
            return next;
        }
    }

    /** Holds the vault attachments by path. */
    private static final class FakeVault implements VaultReader {
        private final Map<String, byte[]> files = new HashMap<>();

        @Override
        public Vault read() {
            throw new UnsupportedOperationException("publish reads attachments only");
        }

        @Override
        public byte[] readFile(String path) {
            return files.get(path);
        }
    }

    /** Holds the template files and lists, and records every read. */
    private static final class FakeSite implements SiteSource {
        private final Map<String, byte[]> contents = new HashMap<>();
        private final Map<String, List<String>> listings = new HashMap<>();
        private final List<String> reads = new ArrayList<>();

        @Override
        public String location() {
            return "/tpl";
        }

        @Override
        public boolean isFile(String path) {
            return false;
        }

        @Override
        public boolean isDirectory(String path) {
            return false;
        }

        @Override
        public byte[] read(String path) {
            reads.add(path);
            return contents.get(path);
        }

        @Override
        public List<String> files(String directory) {
            return listings.getOrDefault(directory, List.of());
        }
    }

    /** A publication target in memory that records every write and delete. */
    private static final class FakeTarget implements PublishTarget {
        private Kind kind = Kind.USABLE;
        private Optional<String> overlap = Optional.empty();
        private final Map<String, List<String>> listings = new HashMap<>();
        private final Set<String> existing = new HashSet<>();
        private final Set<String> sameFiles = new HashSet<>();
        private boolean holdsEveryContent;
        private StoreFailure writableFailure;
        private final Map<String, byte[]> written = new LinkedHashMap<>();
        private final List<String> deleted = new ArrayList<>();
        private final List<String> deletedTrees = new ArrayList<>();

        @Override
        public String location() {
            return "/t";
        }

        @Override
        public Kind inspect() {
            return kind;
        }

        @Override
        public Optional<String> overlap() {
            return overlap;
        }

        @Override
        public boolean sameFile(String first, String second) {
            return sameFiles.contains(first + "|" + second);
        }

        @Override
        public void verifyWritable(String path) {
            if (writableFailure != null) {
                throw writableFailure;
            }
        }

        @Override
        public boolean exists(String path) {
            return existing.contains(path);
        }

        @Override
        public boolean hasContent(String path, byte[] content) {
            return holdsEveryContent;
        }

        @Override
        public List<String> files(String directory) {
            return listings.getOrDefault(directory, List.of());
        }

        @Override
        public void write(String path, byte[] content) {
            written.put(path, content);
        }

        @Override
        public void delete(String path) {
            deleted.add(path);
        }

        @Override
        public void deleteTree(String directory) {
            deletedTrees.add(directory);
        }
    }

    /** The git view of the target: origin, branch and status that the test sets. */
    private static final class FakeGit implements GitRepository {
        private Optional<String> origin = Optional.empty();
        private String branch;
        private List<GitChange> status = List.of();

        @Override
        public Optional<String> originUrl() {
            return origin;
        }

        @Override
        public String branch() {
            return branch;
        }

        @Override
        public List<GitChange> status() {
            return status;
        }
    }

    private final FakeBuild build = new FakeBuild();
    private final FakeVault vault = new FakeVault();
    private final FakeSite site = new FakeSite();
    private final FakeTarget target = new FakeTarget();
    private final FakeGit git = new FakeGit();

    private PublishService service(boolean force, ConfiguredPaths paths) {
        return new PublishService(build, vault, site, target, git, paths, force, "chore(content): refresh Knowledge Hub");
    }

    private PublishService service(boolean force) {
        return service(force, PATHS);
    }

    private static Assembly built(String script, List<String> images, List<String> missing) {
        BuildReport report = new BuildReport("b", BuildReport.OK, null, null, false, "/v", "/tpl", 1, 1, images, 0,
                List.of(), missing, script.length(), "x", List.of());
        return new Assembly(report, new GeneratedSite(script, images));
    }

    private static Assembly built() {
        return built("window.BLOG_DATA = {};\n", List.of(), List.of());
    }

    private void nothingWasChanged() {
        assertThat(target.written).isEmpty();
        assertThat(target.deleted).isEmpty();
        assertThat(target.deletedTrees).isEmpty();
    }

    private void aPublishableTemplate() {
        git.origin = Optional.of(PAGES);
        site.contents.put("index.html", "<html></html>".getBytes(StandardCharsets.UTF_8));
        site.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js"));
        site.contents.put("assets/js/app.js", "case 'html'".getBytes(StandardCharsets.UTF_8));
        git.branch = "main";
        git.status = List.of();
    }

    @Test
    void aWrongRemoteIsRefusedBeforeAnyWriteOrDelete() {
        git.origin = Optional.of("git@github.com:x/other.git");
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target is not the expected repository");
        assertThat(report.detail()).startsWith("origin is git@github.com:x/other.git; expected");
        nothingWasChanged();
    }

    @Test
    void forceTurnsAWrongRemoteIntoAWarning() {
        aPublishableTemplate();
        git.origin = Optional.of("git@github.com:x/other.git");
        PublishReport report = service(true).publish(built(), true);
        assertThat(report.isRefused()).isFalse();
        assertThat(report.warnings()).anyMatch(w -> w.contains("git@github.com:x/other.git"));
    }

    @Test
    void aTargetWithNoOriginIsRefusedEvenWithForce() {
        git.origin = Optional.empty();
        PublishReport report = service(true).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target has no origin remote");
        nothingWasChanged();
    }

    @Test
    void theFilesystemRootIsRefusedEvenWithForce() {
        PublishService service = service(true);
        target.kind = PublishTarget.Kind.FILESYSTEM_ROOT;
        PublishReport report = service.publish(built(), false);
        assertThat(report.title()).isEqualTo("Illegal publication target");
        assertThat(report.detail()).isEqualTo("The filesystem root is never a publication target.");
        nothingWasChanged();
    }

    @Test
    void aVaultOrTemplateNestedWithTheTargetIsRefusedBeforeTheKindOfTargetIsChecked() {
        PublishService service = service(false);
        target.kind = PublishTarget.Kind.NOT_A_GIT_WORKING_COPY;
        target.overlap = Optional.of("The vault '/t/v' is the publication target '/t' or inside it.");
        PublishReport report = service.publish(built(), false);
        assertThat(report.title()).isEqualTo("Illegal publication target");
        assertThat(report.detail()).contains("'/t/v'");
        nothingWasChanged();
    }

    @Test
    void anUnsetTargetPathIsRefusedByItsPropertyName() {
        PublishReport report = service(false, new ConfiguredPaths("/v", "/tpl", null)).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("mb2e.target.path");
        nothingWasChanged();
    }

    @Test
    void aRefusedBuildRefusesThePublicationWithTheSameReasonAndWritesNothing() {
        PublishService service = service(false);
        BuildReport refused = BuildReport.refused("b", "No note carries the selection tag",
                "No note in the vault carries the tag 't'.", false, PATHS);
        build.next = new Assembly(refused, null);
        PublishReport report = service.publish(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("No note carries the selection tag");
        assertThat(report.detail()).contains("'t'");
        nothingWasChanged();
    }

    @Test
    void aDryRunPlansTheDeletionsAndDoesNotPerformThem() {
        aPublishableTemplate();
        target.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js",
                "assets/js/old.js", "assets/media/keep.png", "assets/img/notes/Old/gone.png", "assets/.DS_Store"));

        PublishReport report = service(false).publish(built(), true);

        assertThat(report.deleted()).containsExactly("assets/js/old.js", "assets/media/keep.png");
        assertThat(report.mediaDeleted()).containsExactly("Old/gone.png");
        nothingWasChanged();
    }

    @Test
    void theSiteIsThePageTheTemplateAssetsTheGeneratedScriptTheImagesAndNojekyll() {
        aPublishableTemplate();
        site.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js", "assets/notes.MD",
                "assets/.DS_Store"));
        vault.files.put("Java/a b.png", new byte[] {1});
        target.listings.put("assets", List.of());

        PublishReport report = service(false).publish(
                built("window.BLOG_DATA = {};\n", List.of("Java/a b.png"), List.of("n -> [[x.png]]")), false);

        assertThat(target.written.get("index.html")).isEqualTo("<html></html>".getBytes(StandardCharsets.UTF_8));
        assertThat(target.written.get(".nojekyll")).isEqualTo(new byte[0]);
        assertThat(target.written.get("assets/js/app.js")).isEqualTo("case 'html'".getBytes(StandardCharsets.UTF_8));
        assertThat(target.written.get("assets/js/data.js")).isEqualTo("window.BLOG_DATA = {};\n".getBytes(StandardCharsets.UTF_8));
        assertThat(target.written.get("assets/img/notes/Java/a b.png")).isEqualTo(new byte[] {1});
        assertThat(target.written.keySet()).noneMatch(path -> path.endsWith("notes.MD"));
        assertThat(target.written.keySet()).noneMatch(path -> path.endsWith(".DS_Store"));
        assertThat(site.reads).doesNotContain("assets/js/data.js");
        assertThat(report.media()).containsExactly("Java/a b.png");
        assertThat(report.missingMedia()).containsExactly("n -> [[x.png]]");
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void aStaleDatasourceDirectoryIsRemovedAndReported() {
        aPublishableTemplate();
        target.listings.put("assets", List.of());
        target.existing.add("datasource");
        target.listings.put("datasource", List.of("datasource/Java/x.md"));
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.deleted()).containsExactly("datasource/Java/x.md");
        assertThat(target.deletedTrees).containsExactly("datasource");
    }

    @Test
    void aTargetFileThatIsTheSameFileAsASiteFileIsRewrittenNotDeleted() {
        aPublishableTemplate();
        site.listings.put("assets", List.of("assets/js/app.js", "assets/css/styles.css"));
        site.contents.put("assets/css/styles.css", "css".getBytes(StandardCharsets.UTF_8));
        target.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js",
                "assets/css/STYLES.CSS", "assets/js/zaźółć.js"));
        target.holdsEveryContent = true;
        target.sameFiles.add("assets/css/STYLES.CSS" + "|" + "assets/css/styles.css");

        PublishReport report = service(false).publish(built(), true);

        assertThat(report.deleted()).doesNotContain("assets/css/STYLES.CSS");
        assertThat(report.changedFiles()).contains("assets/css/styles.css");
        assertThat(report.warnings()).anyMatch(w -> w.contains("STYLES.CSS") && w.contains("not deleted"));
        nothingWasChanged();
    }

    @Test
    void aCaseTwinThatIsADifferentFileIsStillDeleted() {
        aPublishableTemplate();
        site.listings.put("assets", List.of("assets/js/app.js", "assets/css/styles.css"));
        site.contents.put("assets/css/styles.css", "css".getBytes(StandardCharsets.UTF_8));
        target.listings.put("assets", List.of("assets/css/STYLES.CSS"));
        target.holdsEveryContent = true;
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.deleted()).containsExactly("assets/css/STYLES.CSS");
    }

    @Test
    void aWriteThatWouldPassThroughALinkRefusesThePublicationBeforeAnyChange() {
        aPublishableTemplate();
        target.listings.put("assets", List.of());
        target.writableFailure = new StoreFailure("Refusing to go through the symbolic link /t/assets", null);
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("symbolic link");
        nothingWasChanged();
    }

    @Test
    void anUntrackedFileThatWasAlreadyThereIsNotAChangeOfThePublication() {
        aPublishableTemplate();
        target.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js"));
        target.holdsEveryContent = true;
        git.status = List.of(new GitChange("??", ".DS_Store"), new GitChange(" M", "a.txt"));
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.changedFiles()).containsExactly("a.txt");
    }

    @Test
    void thePrintedCommitCommandKeepsShellCharactersOfTheMessageLiteral() {
        aPublishableTemplate();
        PublishService service = new PublishService(build, vault, site, target, git, PATHS, false,
                "say \"hi\" $(id) `id` \\n");
        PublishReport report = service.publish(built(), true);
        assertThat(report.commands()).contains("git -C /t commit -m \"say \\\"hi\\\" \\$(id) \\`id\\` \\\\n\"");
    }

    @Test
    void aTargetThatAlreadyHoldsTheSiteIsUpToDate() {
        aPublishableTemplate();
        target.listings.put("assets", List.of("assets/js/app.js", "assets/js/data.js"));
        target.holdsEveryContent = true;
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.message()).isEqualTo("The site is already up to date.");
        assertThat(report.commands()).isEmpty();
        assertThat(report.changedFiles()).isEmpty();
    }
}
