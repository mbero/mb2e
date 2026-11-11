package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.SyncPlan;
import io.github.mbero.mb2e.domain.model.SyncReport;

/** Use case that mirrors the tagged notes of the vault into the datasource. */
public interface SyncUseCase {

    /** Runs the sync; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    SyncReport sync(boolean dryRun);

    /** Plans the sync as a dry run, returning the bytes it would write; writes nothing. */
    SyncPlan plan();
}
