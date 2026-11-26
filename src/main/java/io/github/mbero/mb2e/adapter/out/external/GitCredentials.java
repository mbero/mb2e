package io.github.mbero.mb2e.adapter.out.external;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * The extra git environment that authenticates one repository, and the secret values to mask in git output.
 * <p>A token reaches git only through the process environment, never through a URL, an argument or
 * {@code .git/config}. {@link #toString()} shows no secret.
 */
public final class GitCredentials {

    private static final GitCredentials NONE = new GitCredentials(Map.of(), List.of());

    private final Map<String, String> environment;
    private final List<String> secrets;

    private GitCredentials(Map<String, String> environment, List<String> secrets) {
        this.environment = environment;
        this.secrets = secrets;
    }

    /** Credentials for a repository that needs none, such as a local {@code file://} remote. */
    public static GitCredentials none() {
        return NONE;
    }

    /** Basic authentication with a personal access token for {@code https://github.com/}. */
    public static GitCredentials httpsToken(String token) {
        String encoded = Base64.getEncoder().encodeToString(("x-access-token:" + token).getBytes(StandardCharsets.UTF_8));
        return new GitCredentials(Map.of(
                "GIT_CONFIG_COUNT", "1",
                "GIT_CONFIG_KEY_0", "http.https://github.com/.extraheader",
                "GIT_CONFIG_VALUE_0", "AUTHORIZATION: basic " + encoded,
                "GIT_CONFIG_NOSYSTEM", "1",
                "GIT_CONFIG_GLOBAL", "/dev/null"), List.of(token, encoded));
    }

    Map<String, String> environment() {
        return environment;
    }

    List<String> secrets() {
        return secrets;
    }

    @Override
    public String toString() {
        return "GitCredentials[" + (secrets.isEmpty() ? "none" : "token hidden") + "]";
    }
}
