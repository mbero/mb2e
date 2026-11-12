package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;

/** Use case that selects the tagged notes of the vault and assembles the site in memory; it writes nothing. */
public interface BuildUseCase {

    /** Runs the build; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    BuildReport build(boolean dryRun);

    /** Runs the build and also returns the assembled site, which is null when the build was refused. */
    Assembly assemble(boolean dryRun);
}
