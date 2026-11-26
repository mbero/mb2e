package io.github.mbero.mb2e.domain.port.out;

import io.github.mbero.mb2e.domain.model.Deployment;
import java.util.List;

/** Keeps the newest finished deployments across restarts. */
public interface DeploymentHistory {

    /**
     * Stores one finished deployment and drops the entries beyond the retention limit.
     *
     * @throws StoreFailure when the history cannot be written
     */
    void record(Deployment deployment);

    /**
     * Returns at most {@code limit} deployments, the newest first.
     *
     * @throws StoreFailure when the history cannot be read
     */
    List<Deployment> newest(int limit);
}
