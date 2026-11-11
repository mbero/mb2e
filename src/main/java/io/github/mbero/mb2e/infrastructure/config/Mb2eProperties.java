package io.github.mbero.mb2e.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties under {@code mb2e}; command-line arguments never set them.
 * <p>Paths come from {@code application_local_paths.yaml}; an unset path becomes a
 * non-existent marker naming its key, so the command that needs it refuses with that name.
 */
@ConfigurationProperties(prefix = "mb2e")
public record Mb2eProperties(
        @DefaultValue Datasource datasource,
        @DefaultValue Site site,
        @DefaultValue Vault vault,
        @DefaultValue Sync sync,
        @DefaultValue Build build,
        @DefaultValue Publish publish,
        @DefaultValue Log log) {

    /** Prefix of the marker path that stands in for an unset path. */
    public static final String NOT_SET = "/mb2e-path-not-set/";

    public record Datasource(String path) {
        public Datasource {
            path = orMarker(path, "mb2e.datasource.path");
        }
    }

    public record Site(String templatePath) {
        public Site {
            templatePath = orMarker(templatePath, "mb2e.site.template-path");
        }
    }

    public record Vault(String path) {
        public Vault {
            path = orMarker(path, "mb2e.vault.path");
        }
    }

    public record Sync(@DefaultValue("blog-ready") String tag, @DefaultValue("false") boolean inlineTags) {
    }

    /** The optional log file; empty switches it off. */
    public record Log(@DefaultValue("") String file) {
    }

    public record Build(@DefaultValue("") String defaultNote) {
    }

    /** The publication target and its guard; {@code force} can be set in configuration only. */
    public record Publish(String targetPath,
            @DefaultValue("mbero.github.io") String expectedRemote, @DefaultValue("false") boolean force) {
        public Publish {
            targetPath = orMarker(targetPath, "mb2e.publish.target-path");
        }
    }

    private static String orMarker(String value, String key) {
        return value == null || value.isBlank() ? NOT_SET + key : value;
    }
}
