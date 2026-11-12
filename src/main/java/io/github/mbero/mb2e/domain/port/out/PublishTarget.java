package io.github.mbero.mb2e.domain.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Working tree of the publication target; paths are relative with "/" separators.
 * <p>It writes and deletes files, and the adapter refuses a delete anywhere outside
 * {@code assets} and the stale {@code datasource} directory of an earlier layout.
 */
public interface PublishTarget {

    enum Kind { USABLE, FILESYSTEM_ROOT, MISSING, NOT_A_DIRECTORY, NOT_A_GIT_WORKING_COPY }

    /** Returns the configured path, as it was configured. */
    String location();

    Kind inspect();

    /**
     * Returns a sentence naming both paths when the target contains, equals or lies inside the
     * vault or the template, by real path; empty when they are apart.
     */
    Optional<String> overlap();

    /** Returns whether the two relative paths name the same file on this file system (a case or Unicode form twin). */
    boolean sameFile(String first, String second);

    /**
     * Checks that a write to this path stays inside the target.
     * @throws StoreFailure when a symbolic link lies on the way
     */
    void verifyWritable(String path);

    boolean exists(String path);

    /** Returns whether the file exists and holds exactly these bytes. */
    boolean hasContent(String path, byte[] content);

    /** Returns every file below this directory, recursively and sorted; empty when it does not exist. */
    List<String> files(String directory);

    void write(String path, byte[] content);

    /** Deletes one file, then the directories it leaves empty (below assets). */
    void delete(String path);

    /** Deletes a directory and everything under it. */
    void deleteTree(String directory);
}
