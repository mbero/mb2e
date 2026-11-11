package io.github.mbero.mb2e.domain.port.out;

import java.util.List;

/** Reads the configured template directory. Paths are relative with "/" separators. */
public interface SiteSource {

    String location();

    boolean isFile(String path);

    boolean isDirectory(String path);

    /** Returns the size in bytes of a file, or 0 when it does not exist. */
    long size(String path);

    /**
     * Returns the bytes of a file.
     * @throws StoreFailure when the file cannot be read
     */
    byte[] read(String path);

    /** Returns every file below this directory, recursively and sorted; empty when it does not exist. */
    List<String> files(String directory);
}
