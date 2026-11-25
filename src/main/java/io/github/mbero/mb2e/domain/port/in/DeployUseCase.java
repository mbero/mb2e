package io.github.mbero.mb2e.domain.port.in;

import io.github.mbero.mb2e.domain.model.DeployReport;

/** Use case that brings the target repository up to date with the source repository. */
public interface DeployUseCase {

    /**
     * Syncs both repositories, builds the site and pushes it when it changed.
     *
     * @param reason why the run started, for the log
     * @return the report; a failure or a refusal is a report, never an exception
     */
    DeployReport deploy(String reason);
}
