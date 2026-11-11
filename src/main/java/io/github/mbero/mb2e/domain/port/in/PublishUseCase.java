package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.PublishInputs;
import io.github.mbero.mb2e.domain.model.PublishReport;
import java.util.Optional;

/** Use case that copies the site into the publication target's working tree. */
public interface PublishUseCase {

    /** Publishes the site; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    PublishReport publish(boolean dryRun);

    /** Plans a publication that reads the given inputs instead of the disk, for the pipeline's dry run. */
    PublishReport plan(PublishInputs inputs);

    /** Checks the target's preconditions alone; returns empty when the target is usable, else the refusal. */
    Optional<PublishReport> checkTarget();
}
