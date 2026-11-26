package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSiteSourceTest {

    @TempDir
    Path template;

    private FileSiteSource source() {
        return new FileSiteSource(template);
    }

    @Test
    void itReportsItsLocationAndTheKindOfEachPath() throws IOException {
        Files.createDirectories(template.resolve("assets/js"));
        Files.writeString(template.resolve("index.html"), "<html>");
        assertThat(source().location()).isEqualTo(template.toString());
        assertThat(source().isFile("index.html")).isTrue();
        assertThat(source().isFile("assets")).isFalse();
        assertThat(source().isDirectory("assets")).isTrue();
        assertThat(source().isDirectory("index.html")).isFalse();
        assertThat(source().isFile("missing")).isFalse();
    }

    @Test
    void itReadsAFileAndRefusesAMissingOne() throws IOException {
        Files.writeString(template.resolve("a.txt"), "zażółć", StandardCharsets.UTF_8);
        assertThat(new String(source().read("a.txt"), StandardCharsets.UTF_8)).isEqualTo("zażółć");
        assertThatThrownBy(() -> source().read("nope.txt")).isInstanceOf(StoreFailure.class)
                .hasMessageContaining("Cannot read");
    }

    @Test
    void itListsFilesRecursivelyAsSortedRelativePathsAndNothingForAMissingDirectory() throws IOException {
        Files.createDirectories(template.resolve("assets/js"));
        Files.createDirectories(template.resolve("assets/empty"));
        Files.writeString(template.resolve("assets/js/b.js"), "b");
        Files.writeString(template.resolve("assets/a.css"), "a");
        assertThat(source().files("assets")).containsExactly("assets/a.css", "assets/js/b.js");
        assertThat(source().files("missing")).isEmpty();
    }
}
