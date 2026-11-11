package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.port.out.SiteSource;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/** Checks what a directory must hold to be the site template. */
final class TemplateCheck {

    static final String BUNDLE = "assets/js/content.js";

    private static final String INCOMPLETE = "The site is incomplete";
    private static final Pattern LOADS_BUNDLE = Pattern.compile(
            "<script\\b[^>]*\\bsrc\\s*=\\s*[\"']assets/js/content\\.js(\\?[^\"']*)?[\"']", Pattern.CASE_INSENSITIVE);

    record Refusal(String title, String detail) {
    }

    private TemplateCheck() {
    }

    /** The page shape: index.html, .nojekyll, assets/js, assets/css, and a script tag for the bundle. */
    static Optional<Refusal> shape(SiteSource site) {
        for (String file : new String[] {"index.html", ".nojekyll"}) {
            if (!site.isFile(file)) {
                return missing(file);
            }
        }
        for (String directory : new String[] {"assets", "assets/js", "assets/css"}) {
            if (!site.isDirectory(directory)) {
                return missing(directory);
            }
        }
        String page = new String(site.read("index.html"), StandardCharsets.UTF_8);
        if (!LOADS_BUNDLE.matcher(page).find()) {
            return Optional.of(new Refusal("The configured directory is not a site template",
                    "index.html does not load " + BUNDLE));
        }
        return Optional.empty();
    }

    /** Runs the whole check a publication makes; the bundle is checked before the shape of assets. */
    static Optional<Refusal> publishable(SiteSource site, String bundleOverride) {
        for (String file : new String[] {"index.html", ".nojekyll"}) {
            if (!site.isFile(file)) {
                return missing(file);
            }
        }
        if (!site.isDirectory("assets")) {
            return missing("assets");
        }
        boolean bundleThere = bundleOverride != null ? !bundleOverride.isEmpty() : site.size(BUNDLE) > 0;
        if (!bundleThere) {
            return Optional.of(new Refusal(INCOMPLETE, BUNDLE + " is missing or empty; run the build first"));
        }
        return shape(site);
    }

    private static Optional<Refusal> missing(String part) {
        return Optional.of(new Refusal(INCOMPLETE, part + " is missing from the configured template directory"));
    }
}
