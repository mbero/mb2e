package io.github.mbero.mb2e.domain.port.out;

/**
 * Write side of a sync: it compares and writes files, and deliberately has no
 * operation that removes anything.
 */
public interface DatasourceMirror {

    enum Presence { ABSENT, SAME, DIFFERENT }

    /** Compares the datasource copy at this relative path with these bytes, by size and then content. */
    Presence compare(String path, byte[] content);

    /** Creates the missing directories and writes the file. */
    void write(String path, byte[] content);
}
