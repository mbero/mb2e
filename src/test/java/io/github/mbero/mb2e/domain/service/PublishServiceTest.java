package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.out.DatasourceFiles;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublishServiceTest {

    private final SiteSource site = mock(SiteSource.class);
    private final PublishTarget target = mock(PublishTarget.class);
    private final GitRepository git = mock(GitRepository.class);
    private final DatasourceFiles datasource = mock(DatasourceFiles.class);

    private PublishService service(boolean force) {
        when(target.location()).thenReturn("/t");
        when(target.inspect()).thenReturn(PublishTarget.Kind.USABLE);
        return new PublishService(site, target, git, datasource, "mbero.github.io", force);
    }

    private void nothingWasChanged() {
        verify(target, never()).write(anyString(), any());
        verify(target, never()).delete(anyString());
        verify(target, never()).deleteTree(anyString());
    }

    @Test
    void aWrongRemoteIsRefusedBeforeAnyWriteOrDelete() {
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/other.git"));
        PublishReport report = service(false).publish(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).isEqualTo(
                "origin is git@github.com:x/other.git; expected a remote containing mbero.github.io");
        nothingWasChanged();
    }

    @Test
    void theFilesystemRootIsRefusedEvenWithForce() {
        PublishService service = service(true);
        when(target.inspect()).thenReturn(PublishTarget.Kind.FILESYSTEM_ROOT);
        PublishReport report = service.publish(false);
        assertThat(report.title()).isEqualTo("Illegal publication target");
        nothingWasChanged();
    }

    @Test
    void anIncompleteSiteIsRefusedBeforeAnyWriteOrDelete() {
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/mbero.github.io.git"));
        when(site.isFile(anyString())).thenReturn(false);
        PublishReport report = service(false).publish(false);
        assertThat(report.title()).isEqualTo("The site is incomplete");
        assertThat(report.detail()).isEqualTo("index.html is missing from the configured template directory");
        nothingWasChanged();
    }

    @Test
    void aDryRunPlansTheDeletionsAndDoesNotPerformThem() {
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/mbero.github.io.git"));
        when(site.isFile(anyString())).thenReturn(true);
        when(site.isDirectory(anyString())).thenReturn(true);
        when(site.size(anyString())).thenReturn(1L);
        when(site.read("index.html")).thenReturn("<script src=\"assets/js/content.js\"></script>".getBytes());
        when(site.read(".nojekyll")).thenReturn(new byte[0]);
        when(site.read("assets/js/content.js")).thenReturn("window.BLOG_DATA = {\"notes\":{}};".getBytes());
        when(site.files("assets")).thenReturn(List.of("assets/js/content.js"));
        when(target.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/js/old.js",
                "assets/media/keep.png", "assets/.DS_Store"));
        when(target.files("assets/media")).thenReturn(List.of("assets/media/keep.png"));
        when(git.branch()).thenReturn("main");
        when(git.status()).thenReturn(List.of());

        PublishReport report = service(false).publish(true);

        assertThat(report.deleted()).containsExactly("assets/js/old.js");
        assertThat(report.mediaDeleted()).containsExactly("keep.png");
        nothingWasChanged();
    }

    private void aPublishableSite(String contentJs) {
        when(git.originUrl()).thenReturn(Optional.of("git@github.com:x/mbero.github.io.git"));
        when(site.isFile(anyString())).thenReturn(true);
        when(site.isDirectory(anyString())).thenReturn(true);
        when(site.size(anyString())).thenReturn(1L);
        when(site.read("index.html")).thenReturn("<script src=\"assets/js/content.js\"></script>".getBytes());
        when(site.read(".nojekyll")).thenReturn(new byte[0]);
        when(site.read("assets/js/content.js")).thenReturn(contentJs.getBytes());
        when(site.files("assets")).thenReturn(List.of("assets/js/content.js"));
        when(git.branch()).thenReturn("main");
        when(git.status()).thenReturn(List.of());
    }

    @Test
    void aDatasourceOrTemplateInsideTheTargetIsRefusedBeforeAnyWriteOrDelete() {
        aPublishableSite("window.BLOG_DATA = {\"notes\":{}};");
        PublishService service = service(false);
        when(target.overlap()).thenReturn(Optional.of("The datasource '/t/datasource' is the publication target '/t' or inside it."));
        PublishReport report = service.publish(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Illegal publication target");
        assertThat(report.detail()).contains("/t/datasource").contains("'/t'");
        assertThat(service.checkTarget()).isPresent();
        nothingWasChanged();
    }

    @Test
    void aTargetFileThatIsTheSameFileAsASourceFileIsRewrittenNotDeleted() {
        aPublishableSite("window.BLOG_DATA = {\"notes\":{}};");
        when(site.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/css/styles.css"));
        when(site.read("assets/css/styles.css")).thenReturn("css".getBytes());
        when(target.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/css/STYLES.CSS",
                "assets/js/zaz\u0301o\u0301\u0142c\u0301.js"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        when(target.sameFile("assets/css/STYLES.CSS", "assets/css/styles.css")).thenReturn(true);
        PublishService service = service(false);

        PublishReport report = service.publish(true);

        assertThat(report.deleted()).doesNotContain("assets/css/STYLES.CSS");
        assertThat(report.changedFiles()).contains("assets/css/styles.css");
        assertThat(report.warnings()).anyMatch(w -> w.contains("STYLES.CSS") && w.contains("not deleted"));
        assertThat(report.message()).isNull();
        nothingWasChanged();
    }

    @Test
    void aCaseTwinThatIsADifferentFileIsStillDeleted() {
        aPublishableSite("window.BLOG_DATA = {\"notes\":{}};");
        when(site.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/css/styles.css"));
        when(site.read("assets/css/styles.css")).thenReturn("css".getBytes());
        when(target.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/css/STYLES.CSS"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        PublishReport report = service(false).publish(true);
        assertThat(report.deleted()).containsExactly("assets/css/STYLES.CSS");
    }

    @Test
    void aMediaFileThatIsTheSameFileAsAReferencedOneIsRewrittenNotDeleted() {
        String bundle = "window.BLOG_DATA = {\"notes\":{\"a\":{\"html\":\"<img src=\\\"assets/media/Pic.png\\\">\"}}};";
        aPublishableSite(bundle);
        when(datasource.read("Pic.png")).thenReturn(Optional.of(new byte[] {1}));
        when(target.files("assets/media")).thenReturn(List.of("assets/media/PIC.PNG"));
        when(target.hasContent(anyString(), any())).thenReturn(true);
        when(target.sameFile("assets/media/PIC.PNG", "assets/media/Pic.png")).thenReturn(true);
        PublishReport report = service(false).publish(true);
        assertThat(report.mediaDeleted()).isEmpty();
        assertThat(report.changedFiles()).contains("assets/media/Pic.png");
    }

    @Test
    void aMarkdownSourceIsNeverCopiedIntoTheTarget() {
        String bundle = "window.BLOG_DATA = {\"notes\":{\"a\":{\"html\":"
                + "\"<img src=\\\"assets/media/Notes/secret.md\\\"><img src=\\\"assets/media/ok.png\\\">\"}}};";
        aPublishableSite(bundle);
        when(site.files("assets")).thenReturn(List.of("assets/js/content.js", "assets/README.MD"));
        when(datasource.read("Notes/secret.md")).thenReturn(Optional.of("SECRET".getBytes()));
        when(datasource.read("ok.png")).thenReturn(Optional.of(new byte[] {1}));
        when(target.files("assets")).thenReturn(List.of());
        when(target.files("assets/media")).thenReturn(List.of());
        when(target.hasContent(anyString(), any())).thenReturn(false);

        PublishReport report = service(false).publish(false);

        assertThat(report.media()).containsExactly("ok.png");
        assertThat(report.warnings()).anyMatch(w -> w.contains("secret.md") && w.contains("Markdown"))
                .anyMatch(w -> w.contains("README.MD"));
        verify(target, never()).write(org.mockito.ArgumentMatchers.endsWith("secret.md"), any());
        verify(target, never()).write(org.mockito.ArgumentMatchers.endsWith("README.MD"), any());
    }

    @Test
    void aWriteThatWouldPassThroughALinkRefusesThePublicationBeforeAnyChange() {
        aPublishableSite("window.BLOG_DATA = {\"notes\":{}};");
        when(target.files("assets")).thenReturn(List.of());
        when(target.files("assets/media")).thenReturn(List.of());
        when(target.hasContent(anyString(), any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new StoreFailure("Refusing to go through the symbolic link /t/assets", null))
                .when(target).verifyWritable(anyString());
        PublishReport report = service(false).publish(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("symbolic link");
        nothingWasChanged();
    }

    @Test
    void anUntrackedFileThatWasAlreadyThereIsNotAChangeOfThePublication() {
        aPublishableSite("window.BLOG_DATA = {\"notes\":{}};");
        when(target.files("assets")).thenReturn(List.of("assets/js/content.js"));
        when(target.files("assets/media")).thenReturn(List.of());
        when(target.hasContent(anyString(), any())).thenReturn(true);
        when(git.status()).thenReturn(List.of(new GitChange("??", ".DS_Store"), new GitChange(" M", "a.txt")));
        PublishReport report = service(false).publish(true);
        assertThat(report.changedFiles()).containsExactly("a.txt");
    }
}
