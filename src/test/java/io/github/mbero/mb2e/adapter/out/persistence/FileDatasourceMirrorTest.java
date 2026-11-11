package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.port.out.DatasourceMirror.Presence;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDatasourceMirrorTest {

    @TempDir
    Path temp;

    @Test
    void comparesAndWritesAndLeavesOtherFilesAlone() throws Exception {
        Files.writeString(temp.resolve("keep.md"), "stay");
        FileDatasourceMirror mirror = new FileDatasourceMirror(temp);
        byte[] one = {1, 2};

        assertThat(mirror.compare("a/b.md", one)).isEqualTo(Presence.ABSENT);
        mirror.write("a/b.md", one);
        assertThat(mirror.compare("a/b.md", one)).isEqualTo(Presence.SAME);
        assertThat(mirror.compare("a/b.md", new byte[] {1, 3})).isEqualTo(Presence.DIFFERENT);
        assertThat(Files.readString(temp.resolve("keep.md"))).isEqualTo("stay");
    }
}
