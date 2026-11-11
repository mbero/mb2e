package io.github.mbero.mb2e.domain.port.out;

import java.util.Optional;

/** Reads one datasource file by relative path; empty when there is no such file. */
public interface DatasourceFiles {

    Optional<byte[]> read(String path);
}
