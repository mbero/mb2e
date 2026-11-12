package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * What a successful build assembled in memory: the text of {@code assets/js/data.js} and the
 * vault-relative paths of the images the selected notes embed, each once, in the order they are first embedded.
 */
public record GeneratedSite(String dataScript, List<String> images) {

    public GeneratedSite {
        images = List.copyOf(images);
    }
}
