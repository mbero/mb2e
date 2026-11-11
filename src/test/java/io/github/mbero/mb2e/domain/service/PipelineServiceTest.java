package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Datasource;
import io.github.mbero.mb2e.domain.model.SourceNote;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {

    @Test
    void theDatasourceIsReadAsItWillBeAfterThePlannedSync() {
        Datasource base = new Datasource("ds", List.of(), List.of(new SourceNote("a.md", "old")), List.of("x.png"));
        Datasource after = PipelineService.overlay(base, Map.of(
                "a.md", "new".getBytes(StandardCharsets.UTF_8),
                "b.md", "second".getBytes(StandardCharsets.UTF_8),
                "y.png", new byte[] {1}));
        assertThat(after.notes()).extracting(SourceNote::path, SourceNote::text)
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("a.md", "new"),
                        org.assertj.core.groups.Tuple.tuple("b.md", "second"));
        assertThat(after.attachments()).containsExactlyInAnyOrder("x.png", "y.png");
    }

    @Test
    void anEmptyPlanLeavesTheDatasourceAlone() {
        Datasource base = new Datasource("ds", List.of(), List.of(), List.of());
        assertThat(PipelineService.overlay(base, Map.of())).isSameAs(base);
    }
}
