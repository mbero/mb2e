package io.github.mbero.mb2e;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * Environment whose sources are the packaged {@code application.yaml}, the local paths file
 * and the {@code mb2e.*} keys of {@code SPRING_APPLICATION_JSON}, in rising precedence.
 * System properties, other OS variables and profiles cannot reach it.
 */
final class Mb2eEnvironment extends StandardEnvironment {

    static final String JSON_VARIABLE = "SPRING_APPLICATION_JSON";
    static final String PREFIX = "mb2e.";
    /** Property that selects the mode of the run; only this class sets it, so no other source can. */
    static final String MODE = "mb2e.mode";
    static final String CLI_MODE = "cli";
    static final String SERVER_MODE = "server";
    /** Variable that names the local paths file; unset means the file in the working directory. */
    static final String LOCAL_FILE_VARIABLE = "MB2E_CONFIG";
    static final String LOCAL_FILE = "application_local_paths.yaml";
    static final String EXCLUDE = "spring.autoconfigure.exclude";
    private static final String BOOT_SECURITY = "org.springframework.boot.autoconfigure.security.";
    /** The login beans that mb2e builds itself, or never needs: no password user, no Boot OAuth client. */
    static final String LOGIN_AUTO_CONFIGURATION = BOOT_SECURITY + "servlet.UserDetailsServiceAutoConfiguration,"
            + BOOT_SECURITY + "oauth2.client.OAuth2ClientAutoConfiguration,"
            + BOOT_SECURITY + "oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration";
    /** All web security: the CLI and a server without the dashboard start none of it. */
    static final String SECURITY_AUTO_CONFIGURATION = BOOT_SECURITY + "servlet.SecurityAutoConfiguration,"
            + BOOT_SECURITY + "servlet.SecurityFilterAutoConfiguration," + LOGIN_AUTO_CONFIGURATION;

    /** A configuration that mb2e refuses to start with. */
    static final class Refused extends RuntimeException {
        Refused(String message) {
            super(message);
        }
    }

    private Mb2eEnvironment() {
    }

    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        // Deliberately empty: no system properties, no OS environment.
    }

    /** Creates the environment with the local paths file of the working directory. */
    static Mb2eEnvironment create(String json) {
        return create(json, null);
    }

    /**
     * Creates the environment from the {@code SPRING_APPLICATION_JSON} value and the
     * {@code MB2E_CONFIG} value; either may be null or blank when unset.
     */
    static Mb2eEnvironment create(String json, String localFile) {
        // The packaged file, then the optional local paths file, which wins over it.
        String local = localFile == null || localFile.isBlank() ? LOCAL_FILE : localFile;
        Map<String, Object> fixed = new LinkedHashMap<>();
        fixed.put(MODE, CLI_MODE);
        fixed.put(EXCLUDE, SECURITY_AUTO_CONFIGURATION);
        fixed.put("spring.config.location", "classpath:/application.yaml,optional:file:" + local);
        return build(json, fixed);
    }

    /**
     * Creates the environment of the server mode: the packaged file only, the validated server
     * properties, and the fixed properties of the web server. The server mode never reads the local paths file.
     * <p>Spring Shell stays on the classpath; its runners are switched off so that no terminal is read.
     */
    static Mb2eEnvironment forServer(String json, Map<String, Object> serverProperties) {
        Map<String, Object> fixed = new LinkedHashMap<>(serverProperties);
        fixed.put(MODE, SERVER_MODE);
        fixed.put("spring.config.location", "classpath:/application.yaml");
        fixed.put("spring.main.web-application-type", "servlet");
        fixed.put("spring.shell.interactive.enabled", false);
        fixed.put("spring.shell.noninteractive.enabled", false);
        fixed.put("spring.shell.script.enabled", false);
        fixed.put("server.tomcat.threads.max", 4);
        fixed.put("server.tomcat.accept-count", 16);
        fixed.put("server.shutdown", "graceful");
        fixed.put("spring.lifecycle.timeout-per-shutdown-phase", "60s");
        fixed.put("server.error.whitelabel.enabled", false);
        fixed.put("server.error.include-stacktrace", "never");
        fixed.put("server.error.include-message", "never");
        fixed.put("logging.level.io.github.mbero.mb2e", "info");
        fixed.put("logging.level.mb2e.step", "info");
        boolean dashboard = Boolean.TRUE.equals(serverProperties.get("mb2e.server.dashboard.enabled"));
        fixed.put(EXCLUDE, dashboard ? LOGIN_AUTO_CONFIGURATION : SECURITY_AUTO_CONFIGURATION);
        // The session of the dashboard login: a cookie only, idle limit 30 minutes; DashboardSecurityConfig
        // adds the total limit. SameSite=Lax, because GitHub's redirect back is a cross-site navigation.
        fixed.put("server.servlet.session.timeout", "30m");
        fixed.put("server.servlet.session.tracking-modes", "cookie");
        fixed.put("server.servlet.session.cookie.name", "mb2e_session");
        fixed.put("server.servlet.session.cookie.http-only", true);
        fixed.put("server.servlet.session.cookie.secure", true);
        fixed.put("server.servlet.session.cookie.same-site", "lax");
        // The reverse proxy terminates TLS; its X-Forwarded-* headers make the login redirect use https.
        fixed.put("server.forward-headers-strategy", "native");
        return build(json, fixed);
    }

    private static Mb2eEnvironment build(String json, Map<String, Object> fixed) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (json != null && !json.isBlank()) {
            Map<String, Object> parsed;
            try {
                parsed = JsonParserFactory.getJsonParser().parseMap(json);
            } catch (RuntimeException failure) {
                throw new Refused(JSON_VARIABLE + " is not a JSON object.");
            }
            flatten("", parsed, properties);
            for (String key : properties.keySet()) {
                if (!key.startsWith(PREFIX)) {
                    throw new Refused(JSON_VARIABLE + " accepts only keys under '" + PREFIX + "'. Refused: " + key);
                }
            }
        }
        Mb2eEnvironment environment = new Mb2eEnvironment();
        // The JSON keys outrank the packaged file; the fixed properties outrank both.
        environment.getPropertySources().addLast(new MapPropertySource(JSON_VARIABLE, properties));
        environment.getPropertySources().addFirst(new MapPropertySource("mb2e-fixed", fixed));
        return environment;
    }

    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        source.forEach((name, value) -> flattenValue(prefix.isEmpty() ? name : prefix + "." + name, value, target));
    }

    @SuppressWarnings("unchecked")
    private static void flattenValue(String key, Object value, Map<String, Object> target) {
        if (value instanceof Map<?, ?> map) {
            flatten(key, (Map<String, Object>) map, target);
        } else if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                flattenValue(key + "[" + i + "]", list.get(i), target);
            }
        } else {
            target.put(key, value);
        }
    }
}
