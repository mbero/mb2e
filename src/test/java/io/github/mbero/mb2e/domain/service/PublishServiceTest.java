package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GeneratedSite;
import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublishServiceTest {

    private static final String PAGES = "git@github.com:x/x.github.io.git";
    private static final ConfiguredPaths PATHS = new ConfiguredPaths("/v", "/tpl", "/t");

    private final BuildUseCase build = mock(BuildUseCase.class);
    private final VaultReader vault = mock(VaultReader.class);
    private final SiteSource site = mock(SiteSource.class);
    private final PublishTarget target = mock(PublishTarget.class);
    private final GitRepository git = mock(GitRepository.class);

    private PublishService service(boolean force, ConfiguredPaths paths) {
        when(target.location()).thenReturn("/t");
        when(target.inspect()).thenReturn(PublishTarget.Kind.USABLE);
        return new PublishService(build, vault, site, target, git, paths, force);
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
        verify(target, never()).write(anyString(), any());
        verify(target, never()).delete(anyString());
        verify(target, never()).deleteTree(anyString());
    }

    private void aPublishableTemplate() {
        when(git.originUrl()).thenReturn(Optional.of(PAGES));
        when(site.read("index.html")).thenReturn("<html></html>".getBytes(StandardCharsets.UTF_8));
        when(site.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js"));
        when(site.read("assets/js/app.js")).thenReturn("case 'html'".getBytes(StandardCharsets.UTF_8));
        when(git.branch()).thenReturn("main");
        when(git.status()).thenReturn(List.of());
    }

    @Test
    void aWrongRemoteIsRefusedBeforeAnyWriteOrDelete() {
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/other.git"));
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target is not the expected repository");
        assertThat(report.detail()).startsWith("origin is git@github.com:x/other.git; expected");
        nothingWasChanged();
    }

    @Test
    void forceTurnsAWrongRemoteIntoAWarning() {
        aPublishableTemplate();
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/other.git"));
        PublishReport report = service(true).publish(built(), true);
        assertThat(report.isRefused()).isFalse();
        assertThat(report.warnings()).anyMatch(w -> w.contains("git@github.com:x/other.git"));
    }

    @Test
    void aTargetWithNoOriginIsRefusedEvenWithForce() {
        when(git.originUrl()).thenReturn(Optional.empty());
        PublishReport report = service(true).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target has no origin remote");
        nothingWasChanged();
    }

    @Test
    void theFilesystemRootIsRefusedEvenWithForce() {
        PublishService service = service(true);
        when(target.inspect()).thenReturn(PublishTarget.Kind.FILESYSTEM_ROOT);
        PublishReport report = service.publish(built(), false);
        assertThat(report.title()).isEqualTo("Illegal publication target");
        assertThat(report.detail()).isEqualTo("The filesystem root is never a publication target.");
        nothingWasChanged();
    }

    @Test
    void aVaultOrTemplateNestedWithTheTargetIsRefusedBeforeTheKindOfTargetIsChecked() {
        PublishService service = service(false);
        when(target.inspect()).thenReturn(PublishTarget.Kind.NOT_A_GIT_WORKING_COPY);
        when(target.overlap()).thenReturn(Optional.of("The vault '/t/v' is the publication target '/t' or inside it."));
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
        when(build.assemble(false)).thenReturn(new Assembly(refused, null));
        PublishReport report = service.publish(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("No note carries the selection tag");
        assertThat(report.detail()).contains("'t'");
        nothingWasChanged();
    }

    @Test
    void aDryRunPlansTheDeletionsAndDoesNotPerformThem() {
        aPublishableTemplate();
        when(target.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js",
                "assets/js/old.js", "assets/media/keep.png", "assets/img/notes/Old/gone.png", "assets/.DS_Store"));

        PublishReport report = service(false).publish(built(), true);

        assertThat(report.deleted()).containsExactly("assets/js/old.js", "assets/media/keep.png");
        assertThat(report.mediaDeleted()).containsExactly("Old/gone.png");
        nothingWasChanged();
    }

    @Test
    void theSiteIsThePageTheTemplateAssetsTheGeneratedScriptTheImagesAndNojekyll() {
        aPublishableTemplate();
        when(site.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js", "assets/notes.MD",
                "assets/.DS_Store"));
        when(vault.readFile("Java/a b.png")).thenReturn(new byte[] {1});
        when(target.files("assets")).thenReturn(List.of());

        PublishReport report = service(false).publish(
                built("window.BLOG_DATA = {};\n", List.of("Java/a b.png"), List.of("n -> [[x.png]]")), false);

        verify(target).write("index.html", "<html></html>".getBytes(StandardCharsets.UTF_8));
        verify(target).write(".nojekyll", new byte[0]);
        verify(target).write("assets/js/app.js", "case 'html'".getBytes(StandardCharsets.UTF_8));
        verify(target).write("assets/js/data.js", "window.BLOG_DATA = {};\n".getBytes(StandardCharsets.UTF_8));
        verify(target).write("assets/img/notes/Java/a b.png", new byte[] {1});
        verify(target, never()).write(org.mockito.ArgumentMatchers.endsWith("notes.MD"), any());
        verify(target, never()).write(org.mockito.ArgumentMatchers.endsWith(".DS_Store"), any());
        verify(site, never()).read("assets/js/data.js");
        assertThat(report.media()).containsExactly("Java/a b.png");
        assertThat(report.missingMedia()).containsExactly("n -> [[x.png]]");
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void aStaleDatasourceDirectoryIsRemovedAndReported() {
        aPublishableTemplate();
        when(target.files("assets")).thenReturn(List.of());
        when(target.exists("datasource")).thenReturn(true);
        when(target.files("datasource")).thenReturn(List.of("datasource/Java/x.md"));
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.deleted()).containsExactly("datasource/Java/x.md");
        verify(target).deleteTree("datasource");
    }

    @Test
    void aTargetFileThatIsTheSameFileAsASiteFileIsRewrittenNotDeleted() {
        aPublishableTemplate();
        when(site.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/css/styles.css"));
        when(site.read("assets/css/styles.css")).thenReturn("css".getBytes(StandardCharsets.UTF_8));
        when(target.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js",
                "assets/css/STYLES.CSS", "assets/js/zaźółć.js"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        when(target.sameFile("assets/css/STYLES.CSS", "assets/css/styles.css")).thenReturn(true);

        PublishReport report = service(false).publish(built(), true);

        assertThat(report.deleted()).doesNotContain("assets/css/STYLES.CSS");
        assertThat(report.changedFiles()).contains("assets/css/styles.css");
        assertThat(report.warnings()).anyMatch(w -> w.contains("STYLES.CSS") && w.contains("not deleted"));
        nothingWasChanged();
    }

    @Test
    void aCaseTwinThatIsADifferentFileIsStillDeleted() {
        aPublishableTemplate();
        when(site.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/css/styles.css"));
        when(site.read("assets/css/styles.css")).thenReturn("css".getBytes(StandardCharsets.UTF_8));
        when(target.files("assets")).thenReturn(List.of("assets/css/STYLES.CSS"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.deleted()).containsExactly("assets/css/STYLES.CSS");
    }

    @Test
    void aWriteThatWouldPassThroughALinkRefusesThePublicationBeforeAnyChange() {
        aPublishableTemplate();
        when(target.files("assets")).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new StoreFailure("Refusing to go through the symbolic link /t/assets", null))
                .when(target).verifyWritable(anyString());
        PublishReport report = service(false).publish(built(), false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("symbolic link");
        nothingWasChanged();
    }

    @Test
    void anUntrackedFileThatWasAlreadyThereIsNotAChangeOfThePublication() {
        aPublishableTemplate();
        when(target.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        when(git.status()).thenReturn(List.of(new GitChange("??", ".DS_Store"), new GitChange(" M", "a.txt")));
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.changedFiles()).containsExactly("a.txt");
    }

    @Test
    void aTargetThatAlreadyHoldsTheSiteIsUpToDate() {
        aPublishableTemplate();
        when(target.files("assets")).thenReturn(List.of("assets/js/app.js", "assets/js/data.js"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        PublishReport report = service(false).publish(built(), true);
        assertThat(report.message()).isEqualTo("The site is already up to date.");
        assertThat(report.commands()).isEmpty();
        assertThat(report.changedFiles()).isEmpty();
    }
}
