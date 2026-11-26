package io.github.mbero.mb2e.adapter.in.web;

import java.util.List;
import java.util.Locale;

/**
 * What the dashboard API shows about the service: the two repositories as {@code owner/name}, the
 * attempt limit of a deployment, and the local paths that a failure cause must never reveal.
 */
public record DashboardSettings(String sourceRepo, String targetRepo, int maxAttempts, List<String> hiddenPaths) {

    public DashboardSettings {
        hiddenPaths = List.copyOf(hiddenPaths);
    }

    /**
     * Returns the address that GitHub Pages serves for the target: {@code https://owner.github.io} for the
     * repository {@code owner/owner.github.io}, otherwise {@code https://owner.github.io/name}; null without a target.
     */
    public String siteUrl() {
        int slash = targetRepo == null ? -1 : targetRepo.indexOf('/');
        if (slash < 1) {
            return null;
        }
        String owner = targetRepo.substring(0, slash).toLowerCase(Locale.ROOT);
        String name = targetRepo.substring(slash + 1);
        String root = "https://" + owner + ".github.io";
        return name.equalsIgnoreCase(owner + ".github.io") ? root : root + "/" + name;
    }
}
