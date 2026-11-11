package io.github.mbero.mb2e.domain.model;

import java.util.Map;

/**
 * Inputs a planned publication reads instead of the disk: the bundle a planned build would
 * write and the datasource files a planned sync would add or change. Both are absent for a
 * plain publication.
 */
public record PublishInputs(String bundleText, Map<String, byte[]> datasourceFiles) {

    public static final PublishInputs NONE = new PublishInputs(null, Map.of());

    public PublishInputs {
        datasourceFiles = Map.copyOf(datasourceFiles);
    }
}
