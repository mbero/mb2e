package io.github.mbero.mb2e.domain.port.out;

import io.github.mbero.mb2e.domain.model.Bundle;

/** Serializes the bundle and replaces the one output file once the new content is complete. */
public interface BundleStore {

    /**
     * Serializes the bundle and writes it, unless this is a dry run.
     * @return the size in bytes of the file written, or that a dry run would write
     * @throws StoreFailure when the datasource cannot be read or the bundle cannot be written
     */
    long write(Bundle bundle, boolean dryRun);

    /** Returns the exact text {@link #write} would put in the file; writes nothing. */
    String serialize(Bundle bundle);
}
