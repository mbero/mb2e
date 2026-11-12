package io.github.mbero.mb2e.domain.model;

import java.util.Optional;

/**
 * The three configured directories, each null when its property is not set.
 * <p>The vault and the template are read; only the target is written.
 */
public record ConfiguredPaths(String vault, String template, String target) {

    public static final String VAULT_KEY = "mb2e.vault.path";
    public static final String TEMPLATE_KEY = "mb2e.template.path";
    public static final String TARGET_KEY = "mb2e.target.path";

    public ConfiguredPaths {
        vault = blankToNull(vault);
        template = blankToNull(template);
        target = blankToNull(target);
    }

    /** Returns the key of the first required path that is not set, checking the target only when asked. */
    public Optional<String> unsetKey(boolean needsTarget) {
        if (vault == null) {
            return Optional.of(VAULT_KEY);
        }
        if (template == null) {
            return Optional.of(TEMPLATE_KEY);
        }
        return needsTarget && target == null ? Optional.of(TARGET_KEY) : Optional.empty();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
