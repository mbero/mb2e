package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Bundle;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.Datasource;
import io.github.mbero.mb2e.domain.model.SourceNote;
import io.github.mbero.mb2e.domain.port.out.BundleStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildServiceTest {

    private final List<Bundle> written = new ArrayList<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-09T13:56:30.111Z"), ZoneOffset.UTC);

    private BuildService service(Datasource datasource, String defaultNote) {
        BundleStore store = new BundleStore() {
            @Override
            public long write(Bundle bundle, boolean dryRun) {
                if (!dryRun) {
                    written.add(bundle);
                }
                return 7;
            }

            @Override
            public String serialize(Bundle bundle) {
                return "bundle";
            }
        };
        return new BuildService(() -> datasource, store, new CompleteTemplate(), clock, defaultNote);
    }

    private static Datasource of(SourceNote... notes) {
        return new Datasource("ds", List.of(), List.of(notes), List.of("img/a b.png"));
    }

    @Test
    void anEmptyDatasourceIsRefusedBeforeAnyWrite() {
        BuildReport report = service(of(), "").build(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("ds");
        assertThat(written).isEmpty();
    }

    @Test
    void aDryRunWritesNothingAndReportsTheSameCounts() {
        BuildReport report = service(of(new SourceNote("x.md", "Tekst.")), "").build(true);
        assertThat(report.noteCount()).isEqualTo(1);
        assertThat(report.outputBytes()).isEqualTo(7);
        assertThat(written).isEmpty();
    }

    @Test
    void theStartNoteFallsBackWithAWarning() {
        BuildReport report = service(of(new SourceNote("x.md", "Tekst.")), "nope").build(false);
        assertThat(report.defaultNote()).isEqualTo("x");
        assertThat(report.warnings()).containsExactly("Configured start note 'nope' does not exist; using 'x'");
        assertThat(written.get(0).generated()).isEqualTo("2026-11-09T13:56:30.111Z");
    }

    @Test
    void anUnresolvedLinkAndAnEmbedAreCounted() {
        BuildReport report = service(of(new SourceNote("x.md", "[[nie ma]] ![[a b.png]] ![[brak.png]]")), "").build(false);
        assertThat(report.missingLinks()).containsExactly("x -> [[brak.png]]", "x -> [[nie ma]]");
        assertThat(report.missingAssets()).containsExactly("x -> [[brak.png]]");
        assertThat(written.get(0).notes().get("x").lead()).contains("src=\"assets/media/img/a%20b.png\"");
    }

    @Test
    void twoRunsCarryDifferentTraceIds() {
        BuildService service = service(of(new SourceNote("x.md", "T.")), "");
        assertThat(service.build(true).traceId()).isNotEqualTo(service.build(true).traceId());
    }

    /** A template directory that holds everything the page shape asks for. */
    private static final class CompleteTemplate implements io.github.mbero.mb2e.domain.port.out.SiteSource {
        @Override
        public String location() {
            return "site";
        }

        @Override
        public boolean isFile(String path) {
            return true;
        }

        @Override
        public boolean isDirectory(String path) {
            return true;
        }

        @Override
        public long size(String path) {
            return 1;
        }

        @Override
        public byte[] read(String path) {
            return "<script src=\"assets/js/content.js\"></script>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public List<String> files(String directory) {
            return List.of();
        }
    }
}
