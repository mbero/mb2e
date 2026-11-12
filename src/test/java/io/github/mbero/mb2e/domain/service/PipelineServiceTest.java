package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.GeneratedSite;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {

    private final BuildUseCase build = mock(BuildUseCase.class);
    private final PublishUseCase publish = mock(PublishUseCase.class);
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
        when(build.assemble(false)).thenReturn(built);
        when(publish.publish(built, false)).thenReturn(published(PublishReport.OK));
        RunReport report = pipeline.run(false, false);
        assertThat(report.outcome()).isEqualTo("ok");
        assertThat(report.build().outcome()).isEqualTo("ok");
        assertThat(report.publish().outcome()).isEqualTo("ok");
        assertThat(report.build().report().traceId()).isEqualTo(report.traceId());
        verify(build).assemble(false);
    }

    @Test
    void aRefusedBuildStopsTheRunBeforePublishing() {
        when(build.assemble(false)).thenReturn(refused());
        RunReport report = pipeline.run(false, false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("No note carries the selection tag");
        assertThat(report.publish().outcome()).isEqualTo("not_run");
        verify(publish, never()).publish(any(Assembly.class), anyBoolean());
    }

    @Test
    void aRefusedPublicationRefusesTheRun() {
        Assembly built = ok();
        when(build.assemble(true)).thenReturn(built);
        when(publish.publish(built, true)).thenReturn(published(PublishReport.REFUSED));
        RunReport report = pipeline.run(true, false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.dryRun()).isTrue();
        assertThat(report.title()).isEqualTo("Publication target not found");
        assertThat(report.build().outcome()).isEqualTo("ok");
    }

    @Test
    void noPublishBuildsAndSkipsThePublishSection() {
        when(build.assemble(false)).thenReturn(ok());
        RunReport report = pipeline.run(false, true);
        assertThat(report.outcome()).isEqualTo("ok");
        assertThat(report.publish().outcome()).isEqualTo("skipped");
        assertThat(report.publish().report()).isNull();
        verify(publish, never()).publish(any(Assembly.class), anyBoolean());
    }
}
