package io.github.mbero.mb2e.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the server mode under {@code mb2e.server}; the launcher fills it from validated
 * {@code MB2E_*} variables. Secrets have the type {@link Secret}, so no text form shows them.
 */
@ConfigurationProperties(prefix = "mb2e.server")
public record ServerProperties(
        @DefaultValue(ServerProperties.DEFAULT_WORKSPACE) String workspace,
        @DefaultValue("true") boolean syncOnStart,
        @DefaultValue("0") int pollIntervalSeconds,
        @DefaultValue Source source,
        @DefaultValue Target target,
        @DefaultValue Webhook webhook,
        @DefaultValue Git git,
        @DefaultValue Dashboard dashboard) {

    /** The default of each setting that the launcher also needs; one literal for both. */
    public static final String DEFAULT_WORKSPACE = "/var/lib/mb2e";
    public static final String DEFAULT_DASHBOARD_FILES = "/opt/mb2e/dashboard";
    public static final String DEFAULT_BRANCH = "main";
    public static final String DEFAULT_MAX_BODY_BYTES = "26214400";
    public static final String DEFAULT_NETWORK_TIMEOUT_SECONDS = "120";

    /** The repository that holds the notes; {@code repository} is its {@code owner/name}, which webhooks carry. */
    public record Source(String url, String repository, @DefaultValue(DEFAULT_BRANCH) String branch,
            @DefaultValue("") String subdir, Secret token) {
    }

    /** The repository that GitHub Pages serves; {@code repository} is its {@code owner/name}. */
    public record Target(String url, String repository, @DefaultValue(DEFAULT_BRANCH) String branch, Secret token) {
    }

    /** The webhook endpoint. */
    public record Webhook(Secret secret, @DefaultValue(DEFAULT_MAX_BODY_BYTES) long maxBodyBytes) {
    }

    /**
     * The dashboard: the one GitHub login that may sign in, the OAuth App and the folder of the built UI.
     * The two GitHub addresses have fixed defaults; only a test points them at a fake server.
     */
    public record Dashboard(@DefaultValue("false") boolean enabled, String allowedLogin, String clientId,
            Secret clientSecret, @DefaultValue("https://github.com") String githubUrl,
            @DefaultValue("https://api.github.com") String githubApiUrl,
            @DefaultValue(DEFAULT_DASHBOARD_FILES) String files) {
    }

    /** The author of the target commits and the time budget of a network command. */
    public record Git(String userName, String userEmail,
            @DefaultValue(DEFAULT_NETWORK_TIMEOUT_SECONDS) int networkTimeoutSeconds) {
    }
}
