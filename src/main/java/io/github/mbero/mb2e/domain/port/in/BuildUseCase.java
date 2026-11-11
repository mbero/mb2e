package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.BuildPlan;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.Datasource;

/** Use case that renders the datasource into the content bundle. */
public interface BuildUseCase {

    /** Runs the build; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    BuildReport build(boolean dryRun);

    /** Plans a dry run over the given datasource instead of the one on disk, with the bundle text it would write. */
    BuildPlan plan(Datasource datasource);
}
