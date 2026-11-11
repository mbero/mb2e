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
    /** Variable that names the local paths file; unset means the file in the working directory. */
    static final String LOCAL_FILE_VARIABLE = "MB2E_CONFIG";
    static final String LOCAL_FILE = "application_local_paths.yaml";

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
        // The packaged file, then the optional local paths file, which wins over it.
        String local = localFile == null || localFile.isBlank() ? LOCAL_FILE : localFile;
        Map<String, Object> fixed = new LinkedHashMap<>();
        fixed.put("spring.config.location", "classpath:/application.yaml,optional:file:" + local);
        Mb2eEnvironment environment = new Mb2eEnvironment();
        // The JSON keys outrank the packaged file; the fixed location outranks both.
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
