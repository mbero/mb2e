package io.github.mbero.mb2e;

import io.github.mbero.mb2e.infrastructure.config.ServerProperties;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the {@code MB2E_*} variables into the validated property map of the server mode.
 * <p>Only the listed names are read; any other variable has no effect. A secret may come from
 * {@code NAME} or from the file named by {@code NAME_FILE}, never from both. A refusal names the
 * variable and never shows a secret or a URL.
 */
final class ServerEnvironment {

    private static final Pattern REPOSITORY =
            Pattern.compile("^https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(\\.git)?$");
    /** The largest body the controller can hold in one array. */
    static final long MAX_BODY_BYTES = Integer.MAX_VALUE - 8L;
    private static final Pattern BRANCH = Pattern.compile("^[A-Za-z0-9_][A-Za-z0-9._/-]*$");
    private static final int MIN_SECRET_LENGTH = 32;
    /** A GitHub login: letters, digits and single inner hyphens, at most 39 characters. */
    private static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$");
    private static final String[] DASHBOARD = {"MB2E_DASHBOARD_ALLOWED_LOGIN", "MB2E_DASHBOARD_OAUTH_CLIENT_ID",
        "MB2E_DASHBOARD_OAUTH_CLIENT_SECRET"};

    private final Map<String, String> environment;
    private final Function<Path, String> fileReader;

    private ServerEnvironment(Map<String, String> environment, Function<Path, String> fileReader) {
        this.environment = environment;
        this.fileReader = fileReader;
    }

    /**
     * Validates the variables and returns the properties of the server mode.
     *
     * @param environment the variables, usually {@code System.getenv()}
     * @param fileReader reads a secret file; it throws when the file cannot be read
     * @throws Mb2eEnvironment.Refused when a variable is missing or wrong
     */
    static Map<String, Object> properties(Map<String, String> environment, Function<Path, String> fileReader) {
        return new ServerEnvironment(environment, fileReader).build();
    }

    private Map<String, Object> build() {
        Map<String, Object> out = new LinkedHashMap<>();
        String sourceUrl = repository("MB2E_SOURCE_REPO_URL");
        String targetUrl = repository("MB2E_TARGET_REPO_URL");
        if (identity(sourceUrl).equals(identity(targetUrl))) {
            throw refused("MB2E_SOURCE_REPO_URL and MB2E_TARGET_REPO_URL name the same repository.");
        }
        String workspace = absolute("MB2E_WORKSPACE", ServerProperties.DEFAULT_WORKSPACE);
        if (Path.of(workspace).getParent() == null) {
            throw refused("MB2E_WORKSPACE must not be the file-system root.");
        }
        String template = absolute("MB2E_TEMPLATE_PATH", "/opt/mb2e/template/mbs-blog");
        if (Path.of(template).startsWith(Path.of(workspace))) {
            throw refused("MB2E_TEMPLATE_PATH must not lie inside MB2E_WORKSPACE.");
        }
        String subdir = subdir();
        String fallback = secret("MB2E_GITHUB_TOKEN", false);
        String sourceToken = tokenOr("MB2E_SOURCE_TOKEN", fallback);
        String targetToken = tokenOr("MB2E_TARGET_TOKEN", fallback);
        String webhook = secret("MB2E_WEBHOOK_SECRET", true);
        if (webhook.length() < MIN_SECRET_LENGTH) {
            throw refused("MB2E_WEBHOOK_SECRET must have at least " + MIN_SECRET_LENGTH + " characters.");
        }
        out.put("mb2e.server.source.url", sourceUrl);
        out.put("mb2e.server.source.repository", identity(sourceUrl));
        out.put("mb2e.server.source.branch", branch("MB2E_SOURCE_BRANCH"));
        out.put("mb2e.server.source.subdir", subdir);
        out.put("mb2e.server.source.token", sourceToken);
        out.put("mb2e.server.target.url", targetUrl);
        out.put("mb2e.server.target.repository", identity(targetUrl));
        out.put("mb2e.server.target.branch", branch("MB2E_TARGET_BRANCH"));
        out.put("mb2e.server.target.token", targetToken);
        out.put("mb2e.server.webhook.secret", webhook);
        out.put("mb2e.server.webhook.max-body-bytes", number("MB2E_WEBHOOK_MAX_BODY_BYTES",
                Long.parseLong(ServerProperties.DEFAULT_MAX_BODY_BYTES), 1, MAX_BODY_BYTES));
        out.put("mb2e.server.git.user-name", required("MB2E_GIT_USER_NAME"));
        out.put("mb2e.server.git.user-email", required("MB2E_GIT_USER_EMAIL"));
        out.put("mb2e.server.git.network-timeout-seconds", number("MB2E_GIT_NETWORK_TIMEOUT_SECONDS",
                Long.parseLong(ServerProperties.DEFAULT_NETWORK_TIMEOUT_SECONDS), 10, 900));
        out.put("mb2e.server.workspace", workspace);
        out.put("mb2e.server.sync-on-start", bool("MB2E_SYNC_ON_START", true));
        out.put("mb2e.server.poll-interval-seconds", poll());
        out.put("server.port", number("MB2E_HTTP_PORT", 8080, 1, 65535));
        out.put("mb2e.template.path", template);
        out.put("mb2e.tag", optional("MB2E_TAG", "blog-ready"));
        out.put("mb2e.inline-tags", bool("MB2E_INLINE_TAGS", false));
        out.put("mb2e.default-note", optional("MB2E_DEFAULT_NOTE", "about"));
        out.put("mb2e.force", bool("MB2E_FORCE", false));
        commitMessage().ifPresent(message -> out.put("mb2e.commit-message", message));
        out.put("mb2e.log.file", optional("MB2E_LOG_FILE", ""));
        out.put("mb2e.vault.path", subdir.isEmpty() ? Path.of(workspace, "source").toString()
                : Path.of(workspace, "source", subdir).toString());
        out.put("mb2e.target.path", Path.of(workspace, "target").toString());
        dashboard(out);
        return out;
    }

    /** The dashboard is on only when all three of its variables are set; a partial set is refused. */
    private void dashboard(Map<String, Object> out) {
        String login = raw(DASHBOARD[0]);
        String clientId = raw(DASHBOARD[1]);
        String clientSecret = secret(DASHBOARD[2], false);
        if (login == null && clientId == null && clientSecret == null) {
            out.put("mb2e.server.dashboard.enabled", false);
            return;
        }
        String[] values = {login, clientId, clientSecret};
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                throw refused("The dashboard needs all of " + String.join(", ", DASHBOARD) + ". Missing: "
                        + DASHBOARD[i]);
            }
        }
        if (!LOGIN.matcher(login).matches()) {
            throw refused(DASHBOARD[0] + " must be one GitHub login.");
        }
        out.put("mb2e.server.dashboard.enabled", true);
        out.put("mb2e.server.dashboard.allowed-login", login);
        out.put("mb2e.server.dashboard.client-id", clientId);
        out.put("mb2e.server.dashboard.client-secret", clientSecret);
        out.put("mb2e.server.dashboard.files", absolute("MB2E_DASHBOARD_FILES", ServerProperties.DEFAULT_DASHBOARD_FILES));
    }

    private static Mb2eEnvironment.Refused refused(String message) {
        return new Mb2eEnvironment.Refused(message);
    }

    private String raw(String name) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? null : value;
    }

    private String optional(String name, String fallback) {
        String value = raw(name);
        return value == null ? fallback : value;
    }

    private String required(String name) {
        String value = raw(name);
        if (value == null) {
            throw refused("Required variable is missing: " + name);
        }
        return value;
    }

    /** Reads {@code NAME} or {@code NAME_FILE}; setting both is refused. */
    private String secret(String name, boolean required) {
        String direct = raw(name);
        String file = raw(name + "_FILE");
        if (direct != null && file != null) {
            throw refused("Set only one of " + name + " and " + name + "_FILE.");
        }
        if (file != null) {
            String content;
            try {
                content = fileReader.apply(Path.of(file));
            } catch (RuntimeException failure) {
                throw refused(name + "_FILE cannot be read.");
            }
            direct = content == null || content.strip().isEmpty() ? null : content.strip();
            if (direct == null) {
                throw refused(name + "_FILE is empty.");
            }
        }
        if (direct == null && required) {
            throw refused("Required variable is missing: " + name + " (or " + name + "_FILE)");
        }
        return direct;
    }

    private String tokenOr(String name, String fallback) {
        String token = secret(name, false);
        if (token != null) {
            return token;
        }
        if (fallback == null) {
            throw refused("Required variable is missing: " + name + " (or MB2E_GITHUB_TOKEN)");
        }
        return fallback;
    }

    private String repository(String name) {
        String value = required(name);
        if (!REPOSITORY.matcher(value).matches()) {
            throw refused(name + " must be an https://github.com/<owner>/<repository> URL with no credentials in it.");
        }
        return value;
    }

    private static String identity(String url) {
        Matcher matcher = REPOSITORY.matcher(url);
        matcher.matches();
        return (matcher.group(1) + "/" + matcher.group(2)).toLowerCase(Locale.ROOT);
    }

    private String branch(String name) {
        String value = optional(name, ServerProperties.DEFAULT_BRANCH);
        if (!BRANCH.matcher(value).matches() || value.contains("..") || value.startsWith("refs/")
                || value.endsWith("/") || value.endsWith(".lock")) {
            throw refused(name + " must be a branch name, not a full reference.");
        }
        return value;
    }

    private String subdir() {
        String value = optional("MB2E_SOURCE_SUBDIR", "");
        if (value.startsWith("/") || value.startsWith("\\") || value.contains("\\") || value.contains("\0")) {
            throw refused("MB2E_SOURCE_SUBDIR must be a relative path with no backslash.");
        }
        for (String part : value.split("/")) {
            if (part.equals("..")) {
                throw refused("MB2E_SOURCE_SUBDIR must not contain a '..' segment.");
            }
        }
        return value.replaceAll("/+$", "");
    }

    private String absolute(String name, String fallback) {
        String value = optional(name, fallback);
        if (!value.startsWith("/") || value.contains("\0")) {
            throw refused(name + " must be an absolute path.");
        }
        return Path.of(value).normalize().toString();
    }

    private boolean bool(String name, boolean fallback) {
        String value = raw(name);
        if (value == null) {
            return fallback;
        }
        if (!value.equals("true") && !value.equals("false")) {
            throw refused(name + " must be true or false.");
        }
        return Boolean.parseBoolean(value);
    }

    private long number(String name, long fallback, long min, long max) {
        String value = raw(name);
        if (value == null) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < min || parsed > max) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw refused(name + " must be a whole number from " + min + " to " + max + ".");
        }
    }

    private long poll() {
        long seconds = number("MB2E_POLL_INTERVAL_SECONDS", 0, 0, 86400);
        if (seconds != 0 && seconds < 60) {
            throw refused("MB2E_POLL_INTERVAL_SECONDS must be 0 or at least 60.");
        }
        return seconds;
    }

    /** Returns the commit message when it is set; the default lives in {@code Mb2eProperties} only. */
    private Optional<String> commitMessage() {
        String value = raw("MB2E_COMMIT_MESSAGE");
        if (value != null && (value.contains("\n") || value.contains("\r"))) {
            throw refused("MB2E_COMMIT_MESSAGE must be one line.");
        }
        return Optional.ofNullable(value);
    }
}
