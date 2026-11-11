package io.github.mbero.mb2e.domain.port.out;

import io.github.mbero.mb2e.domain.model.Datasource;

/** Reads the configured datasource. A directory that does not exist reads as an empty datasource. */
public interface DatasourceReader {

    Datasource read();
}
