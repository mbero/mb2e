package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.PublishReport;

/** Use case that mirrors the assembled site into the publication target's working tree. */
public interface PublishUseCase {

    /** Builds the site, then publishes it; a refusal is returned as a report with outcome {@code refused}, never thrown. */
    PublishReport publish(boolean dryRun);

    /** Publishes a site that a successful build already assembled. */
    PublishReport publish(Assembly built, boolean dryRun);
}
