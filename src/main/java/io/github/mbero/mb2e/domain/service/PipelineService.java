package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import java.util.UUID;

/**
 * Runs build and publish in that order; a refused build stops the run before anything is published.
 * <p>The site is built once, in memory, and the publish step mirrors that same site. A dry run
 * plans both steps and writes nothing.
 */
public class PipelineService implements PipelineUseCase {

    private final BuildUseCase build;
    private final PublishUseCase publish;

    public PipelineService(BuildUseCase build, PublishUseCase publish) {
        this.build = build;
        this.publish = publish;
    }

    @Override
    public RunReport run(boolean dryRun, boolean noPublish) {
        String traceId = UUID.randomUUID().toString();
        Assembly built = build.assemble(dryRun);
        BuildReport buildReport = built.report().withTraceId(traceId);
        StepResult<BuildReport> buildSection = StepResult.of(buildReport.outcome(), buildReport);
        StepResult<PublishReport> publishSection = StepResult.notRun();
        if (!buildReport.isRefused()) {
            if (noPublish) {
                publishSection = StepResult.skipped();
            } else {
                PublishReport report = publish.publish(built, dryRun).withTraceId(traceId);
                publishSection = StepResult.of(report.outcome(), report);
            }
        }
        return summary(traceId, dryRun, buildSection, publishSection);
    }

    private static RunReport summary(String traceId, boolean dryRun, StepResult<BuildReport> build,
            StepResult<PublishReport> publish) {
        String title = null;
        String detail = null;
        if (build.report() != null && build.isRefused()) {
            title = build.report().title();
            detail = build.report().detail();
        } else if (publish.report() != null && publish.isRefused()) {
            title = publish.report().title();
            detail = publish.report().detail();
        }
        return new RunReport(traceId, title == null ? "ok" : "refused", title, detail, dryRun, build, publish);
    }
}
