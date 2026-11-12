package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.RunReport;

/** Use case that runs build and publish, in that order. */
public interface PipelineUseCase {

    /** Runs the pipeline; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    RunReport run(boolean dryRun, boolean noPublish);
}
