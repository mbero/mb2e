package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers the inspection, reading and writing methods of the target that the link and overlap tests skip. */
class FilePublishTargetAccessTest {

    @TempDir
    Path temp;

    private FilePublishTarget target(String configured) {
        return new FilePublishTarget(configured, temp.resolve("template"), temp.resolve("elsewhere/vault"));
    }

    @Test
    void anUnsetOrBlankTargetIsMissingAndHasNoOverlap() {
        for (String configured : new String[] {null, "", "  "}) {
            assertThat(target(configured).inspect()).isEqualTo(PublishTarget.Kind.MISSING);
            assertThat(target(configured).overlap()).isEqualTo(Optional.empty());
        }
        assertThat(target("x").location()).isEqualTo("x");
    }

    @Test
    void aMissingPathIsMissingAndAFileIsNotADirectory() throws IOException {
        assertThat(target(temp.resolve("nowhere").toString()).inspect()).isEqualTo(PublishTarget.Kind.MISSING);
        Path file = Files.writeString(temp.resolve("file"), "x");
        assertThat(target(file.toString()).inspect()).isEqualTo(PublishTarget.Kind.NOT_A_DIRECTORY);
    }

    @Test
    void writeThenReadBackTheContentAndTheListing() throws IOException {
        Path root = Files.createDirectories(temp.resolve("site"));
        FilePublishTarget target = target(root.toString());
        byte[] content = "hello".getBytes();
        assertThat(target.exists("assets/a.txt")).isFalse();
        assertThat(target.hasContent("assets/a.txt", content)).isFalse();
        target.verifyWritable("assets/a.txt");
        target.write("assets/a.txt", content);
        assertThat(target.exists("assets/a.txt")).isTrue();
        assertThat(target.hasContent("assets/a.txt", content)).isTrue();
        assertThat(target.hasContent("assets/a.txt", "other".getBytes())).isFalse();
        assertThat(target.hasContent("assets/a.txt", "hellO".getBytes())).isFalse();
        assertThat(target.files("assets")).containsExactly("assets/a.txt");
    }

    @Test
    void aWriteIntoAPathBlockedByAFileIsAStoreFailure() throws IOException {
        Path root = Files.createDirectories(temp.resolve("site"));
        Files.writeString(root.resolve("blocker"), "x");
        assertThatThrownBy(() -> target(root.toString()).write("blocker/a.txt", new byte[0]))
                .isInstanceOf(StoreFailure.class);
    }

    @Test
    void sameFileIsFalseWhenOneOfThePathsIsMissingAndTrueForTheSameFile() throws IOException {
        Path root = Files.createDirectories(temp.resolve("site"));
        Files.writeString(root.resolve("a.txt"), "x");
        FilePublishTarget target = target(root.toString());
        assertThat(target.sameFile("a.txt", "b.txt")).isFalse();
        assertThat(target.sameFile("a.txt", "a.txt")).isTrue();
    }
}
