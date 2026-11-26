package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GeneratedSite;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {

    /** Returns the assembly that the test sets and counts the calls. */
    private static final class FakeBuild implements BuildUseCase {
        private Assembly next;
        private final List<Boolean> calls = new ArrayList<>();

        @Override
        public BuildReport build(boolean dryRun) {
            throw new UnsupportedOperationException("the pipeline assembles, it never builds a report alone");
        }

        @Override
        public Assembly assemble(boolean dryRun) {
            calls.add(dryRun);
            return next;
        }
    }

    /** Publishes the given assembly with the report that the test sets, and records what it received. */
    private static final class FakePublish implements PublishUseCase {
        private PublishReport next;
        private final List<Assembly> received = new ArrayList<>();
        private final List<Boolean> dryRuns = new ArrayList<>();

        @Override
        public PublishReport publish(boolean dryRun) {
            throw new UnsupportedOperationException("the pipeline publishes the assembly it built");
        }

        @Override
        public PublishReport publish(Assembly built, boolean dryRun) {
            received.add(built);
            dryRuns.add(dryRun);
            return next;
        }
    }

    private final FakeBuild build = new FakeBuild();
    private final FakePublish publish = new FakePublish();
    private final PipelineService pipeline = new PipelineService(build, publish);

    private static Assembly ok() {
        BuildReport report = new BuildReport("b", BuildReport.OK, null, null, false, "/v", "/tpl", 1, 1, List.of(),
                0, List.of(), List.of(), 1, "x", List.of());
        return new Assembly(report, new GeneratedSite("s", List.of()));
    }

    private static Assembly refused() {
        return new Assembly(BuildReport.refused("b", "No note carries the selection tag", "none", false,
                new ConfiguredPaths("/v", "/tpl", "/t")), null);
    }

    private static PublishReport published(String outcome) {
        return outcome.equals(PublishReport.OK)
                ? new PublishReport("p", outcome, null, null, false, "/t", "main", null, List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of())
                : PublishReport.refused("p", "Publication target not found", "gone", false, "/t");
    }

    @Test
    void theBuildIsMadeOnceAndItsSiteIsWhatIsPublished() {
        Assembly built = ok();
        build.next = built;
        publish.next = published(PublishReport.OK);
        RunReport report = pipeline.run(false, false);
        assertThat(report.outcome()).isEqualTo("ok");
        assertThat(report.build().outcome()).isEqualTo("ok");
        assertThat(report.publish().outcome()).isEqualTo("ok");
        assertThat(report.build().report().traceId()).isEqualTo(report.traceId());
        assertThat(build.calls).containsExactly(false);
        assertThat(publish.received).containsExactly(built);
        assertThat(publish.dryRuns).containsExactly(false);
    }

    @Test
    void aRefusedBuildStopsTheRunBeforePublishing() {
        build.next = refused();
        RunReport report = pipeline.run(false, false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("No note carries the selection tag");
        assertThat(report.publish().outcome()).isEqualTo("not_run");
        assertThat(publish.received).isEmpty();
    }

    @Test
    void aRefusedPublicationRefusesTheRun() {
        build.next = ok();
        publish.next = published(PublishReport.REFUSED);
        RunReport report = pipeline.run(true, false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.dryRun()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target not found");
        assertThat(report.build().outcome()).isEqualTo("ok");
        assertThat(build.calls).containsExactly(true);
        assertThat(publish.dryRuns).containsExactly(true);
    }

    @Test
    void noPublishBuildsAndSkipsThePublishSection() {
        build.next = ok();
        RunReport report = pipeline.run(false, true);
        assertThat(report.outcome()).isEqualTo("ok");
        assertThat(report.publish().outcome()).isEqualTo("skipped");
        assertThat(report.publish().report()).isNull();
        assertThat(publish.received).isEmpty();
    }
}
