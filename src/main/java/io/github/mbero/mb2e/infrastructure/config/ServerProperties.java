package io.github.mbero.mb2e.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the server mode under {@code mb2e.server}; the launcher fills it from validated
 * {@code MB2E_*} variables. Secrets have the type {@link Secret}, so no text form shows them.
 */
@ConfigurationProperties(prefix = "mb2e.server")
public record ServerProperties(
        @DefaultValue("/var/lib/mb2e") String workspace,
        @DefaultValue("true") boolean syncOnStart,
        @DefaultValue("0") int pollIntervalSeconds,
        @DefaultValue Source source,
        @DefaultValue Target target,
        @DefaultValue Webhook webhook,
        @DefaultValue Git git) {

    /** The repository that holds the notes. */
    public record Source(String url, @DefaultValue("main") String branch, @DefaultValue("") String subdir,
            Secret token) {
    }

    /** The repository that GitHub Pages serves. */
    public record Target(String url, @DefaultValue("main") String branch, Secret token) {
    }

    /** The webhook endpoint. */
    public record Webhook(Secret secret, @DefaultValue("26214400") long maxBodyBytes) {
    }

    /** The author of the target commits and the time budget of a network command. */
    public record Git(String userName, String userEmail, @DefaultValue("120") int networkTimeoutSeconds) {
    }
}
