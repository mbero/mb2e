package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Fixes the block output for one document with every block type, so a restructuring cannot change it. */
class MarkdownBlocksCharacterizationTest {

    private static final LinkResolver NOTHING = new LinkResolver() {
        @Override
        public Optional<NoteLink> resolveNote(String target) {
            return Optional.empty();
        }

        @Override
        public Optional<String> resolveAsset(String target) {
            return Optional.empty();
        }
    };

    private static String resource(String name) throws IOException {
        try (InputStream in = MarkdownBlocksCharacterizationTest.class.getResourceAsStream("/markdown/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyBlockTypeRendersAsBefore() throws IOException {
        String rendered = MarkdownRenderer.render(resource("every-block.md"), NOTHING).stream()
                .map(block -> block.type() + ": " + block.html()).collect(Collectors.joining("\n")) + "\n";
        assertThat(rendered).isEqualTo(resource("every-block.html.txt"));
    }
}
