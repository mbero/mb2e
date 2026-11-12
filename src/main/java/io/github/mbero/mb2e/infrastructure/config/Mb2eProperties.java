package io.github.mbero.mb2e.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties under {@code mb2e}; command-line arguments never set them.
 * <p>The three paths come from {@code application_local_paths.yaml} and have no default. A path
 * that is not set stays null, and the command that needs it refuses with the property's name.
 */
@ConfigurationProperties(prefix = "mb2e")
public record Mb2eProperties(
        @DefaultValue Location vault,
        @DefaultValue Location template,
        @DefaultValue Location target,
        @DefaultValue("blog-ready") String tag,
        @DefaultValue("false") boolean inlineTags,
        @DefaultValue("about") String defaultNote,
        @DefaultValue("false") boolean force,
        @DefaultValue Log log) {

    /** One configured directory. */
    public record Location(String path) {
    }

    /** The optional log file; empty switches it off. */
    public record Log(@DefaultValue("") String file) {
    }
}
