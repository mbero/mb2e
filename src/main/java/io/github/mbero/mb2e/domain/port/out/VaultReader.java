package io.github.mbero.mb2e.domain.port.out;

import io.github.mbero.mb2e.domain.model.Vault;

/** Reads the configured vault, which is opened read-only and never written. */
public interface VaultReader {

    /**
     * Scans the vault.
     * @throws StoreFailure when a directory or a note cannot be read
     */
    Vault read();

    /** Returns the bytes of one non-Markdown file of the vault, by a path {@link #read()} returned. */
    byte[] readFile(String path);
}
