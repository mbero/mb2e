package io.github.mbero.mb2e.adapter.in.web;

import java.util.Optional;

/** Knows the newest finished deployment. */
public interface LatestDeploy {

    /** Returns the newest finished deployment, empty before the first one. */
    Optional<DeployQueue.Last> last();
}
