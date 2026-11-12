package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePublishTargetTest {

    @TempDir
    Path temp;

    private FilePublishTarget target(Path root) throws IOException {
        Files.createDirectories(root);
        return new FilePublishTarget(root.toString(), temp.resolve("template"), temp.resolve("elsewhere/vault"));
    }

    @Test
    void theFilesystemRootIsNeverATarget() {
        assertThat(new FilePublishTarget("/", temp.resolve("template"), temp.resolve("v")).inspect())
                .isEqualTo(PublishTarget.Kind.FILESYSTEM_ROOT);
    }

    @Test
    void aDirectoryWithoutDotGitIsNotAWorkingCopy() throws IOException {
        assertThat(target(temp.resolve("t")).inspect()).isEqualTo(PublishTarget.Kind.NOT_A_GIT_WORKING_COPY);
        Files.createDirectories(temp.resolve("t/.git"));
        assertThat(target(temp.resolve("t")).inspect()).isEqualTo(PublishTarget.Kind.USABLE);
    }

    @Test
    void deleteRefusesAnythingButAssetsAndDatasource() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        Files.writeString(temp.resolve("t/README.md"), "keep");
        assertThatThrownBy(() -> target.delete("README.md")).isInstanceOf(StoreFailure.class);
        assertThatThrownBy(() -> target.delete("../outside.txt")).isInstanceOf(StoreFailure.class);
        assertThatThrownBy(() -> target.deleteTree("assets")).isInstanceOf(StoreFailure.class);
        assertThat(temp.resolve("t/README.md")).exists();
    }

    @Test
    void deletingTheLastFileOfAFolderRemovesTheFolderButNotAssets() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        target.write("assets/media/x/a.png", new byte[] {1});
        target.delete("assets/media/x/a.png");
        assertThat(temp.resolve("t/assets")).exists();
        assertThat(temp.resolve("t/assets/media")).doesNotExist();
    }

    @Test
    void theStaleDatasourceDirectoryGoesWithEverythingUnderIt() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        target.write("datasource/A/b.md", new byte[] {1});
        target.deleteTree("datasource");
        assertThat(temp.resolve("t/datasource")).doesNotExist();
    }

    private FilePublishTarget overlapping(Path root, Path template, Path vault) throws IOException {
        Files.createDirectories(root);
        return new FilePublishTarget(root.toString(), template, vault);
    }

    @Test
    void aVaultOrTemplateEqualToInsideOrAroundTheTargetOverlaps() throws IOException {
        Path root = temp.resolve("pages");
        assertThat(overlapping(root, temp.resolve("tpl"), temp.resolve("vault")).overlap()).isEmpty();
        assertThat(overlapping(root, temp.resolve("tpl"), root.resolve("vault")).overlap().orElseThrow())
                .contains(root.toRealPath().toString()).contains("vault");
        assertThat(overlapping(root, temp.resolve("tpl"), root).overlap()).isPresent();
        assertThat(overlapping(root, root.resolve("tpl"), temp.resolve("vault")).overlap().orElseThrow())
                .contains("template directory");
        assertThat(overlapping(temp, temp.resolve("tpl"), temp.resolve("vault")).overlap()).isPresent();
    }

    @Test
    void aTargetInsideTheTemplateOrTheVaultOverlaps() throws IOException {
        Path tpl = temp.resolve("tpl");
        assertThat(overlapping(tpl.resolve("site"), tpl, temp.resolve("vault")).overlap().orElseThrow())
                .contains("template directory");
        Path vault = temp.resolve("vault");
        assertThat(overlapping(vault.resolve("site"), tpl, vault).overlap().orElseThrow()).contains("vault");
    }

    @Test
    void theOverlapComparesRealPaths() throws IOException {
        Path root = temp.resolve("pages");
        Files.createDirectories(root.resolve("vault"));
        Path link = Files.createSymbolicLink(temp.resolve("alias"), root.resolve("vault"));
        assertThat(overlapping(root, temp.resolve("tpl"), link).overlap()).isPresent();
    }

    @Test
    void aWriteThroughASymbolicLinkedAssetsDirectoryIsRefused() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.createSymbolicLink(temp.resolve("t/assets"), outside);
        assertThatThrownBy(() -> target.write("assets/css/styles.css", new byte[] {1}))
                .isInstanceOf(StoreFailure.class).hasMessageContaining("symbolic link");
        assertThatThrownBy(() -> target.verifyWritable("assets/css/styles.css"))
                .isInstanceOf(StoreFailure.class).hasMessageContaining(temp.resolve("t").toRealPath() + "/assets");
        assertThatThrownBy(() -> target.delete("assets/x.txt")).isInstanceOf(StoreFailure.class);
        try (var listing = Files.list(outside)) {
            assertThat(listing).isEmpty();
        }
    }

    @Test
    void aSymbolicLinkedFileOrParentBelowAssetsIsRefusedForAWrite() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.createDirectories(temp.resolve("t/assets"));
        Files.createSymbolicLink(temp.resolve("t/assets/js"), outside);
        Files.createSymbolicLink(temp.resolve("t/assets/f.txt"), outside.resolve("f.txt"));
        assertThatThrownBy(() -> target.write("assets/js/content.js", new byte[] {1}))
                .isInstanceOf(StoreFailure.class);
        assertThatThrownBy(() -> target.write("assets/f.txt", new byte[] {1})).isInstanceOf(StoreFailure.class);
        assertThat(outside.resolve("content.js")).doesNotExist();
        assertThat(outside.resolve("f.txt")).doesNotExist();
    }

    @Test
    void aLinkedDatasourceCanStillBeRemovedAsALink() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.writeString(outside.resolve("keep.txt"), "k");
        Files.createSymbolicLink(temp.resolve("t/datasource"), outside);
        target.deleteTree("datasource");
        assertThat(temp.resolve("t/datasource")).doesNotExist();
        assertThat(outside.resolve("keep.txt")).exists();
    }

    @Test
    void sameFileSeesACaseTwinOnACaseInsensitiveFileSystem() throws IOException {
        FilePublishTarget target = target(temp.resolve("t"));
        Files.createDirectories(temp.resolve("t/assets"));
        Files.writeString(temp.resolve("t/assets/X.CSS"), "x");
        boolean insensitive = Files.exists(temp.resolve("t/assets/x.css"));
        assertThat(target.sameFile("assets/X.CSS", "assets/x.css")).isEqualTo(insensitive);
        assertThat(target.sameFile("assets/X.CSS", "assets/other.css")).isFalse();
    }
}
