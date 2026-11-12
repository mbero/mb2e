package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.port.out.SiteSource;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Checks what a directory must hold to be the site template. */
final class TemplateCheck {

    static final String PAGE = "index.html";
    static final String APP_SCRIPT = "assets/js/app.js";
    static final String HTML_BLOCK_MARKER = "case 'html'";
    static final String NOTE_IMAGES = "assets/img/notes";

    private static final String INCOMPLETE = "The site is incomplete";
    private static final String NOT_A_TEMPLATE = "The configured directory is not a site template";

    record Refusal(String title, String detail) {
    }

    private TemplateCheck() {
    }

    /**
     * Checks the page, the page script, the marker that the script renders an html block, and
     * that the directory mb2e writes the note images to is empty.
     */
    static Optional<Refusal> shape(SiteSource site) {
        if (!site.isFile(PAGE)) {
            return missing(PAGE);
        }
        if (!site.isFile(APP_SCRIPT)) {
            return missing(APP_SCRIPT);
        }
        String script = new String(site.read(APP_SCRIPT), StandardCharsets.UTF_8);
        if (!script.contains(HTML_BLOCK_MARKER)) {
            return Optional.of(new Refusal(NOT_A_TEMPLATE, APP_SCRIPT + " does not contain " + HTML_BLOCK_MARKER
                    + ", so its page cannot show a note rendered as one html block"));
        }
        if (!site.files(NOTE_IMAGES).isEmpty()) {
            return Optional.of(new Refusal(NOT_A_TEMPLATE, "The template holds files under " + NOTE_IMAGES
                    + ", the directory where mb2e writes the note images"));
        }
        return Optional.empty();
    }

    private static Optional<Refusal> missing(String part) {
        return Optional.of(new Refusal(INCOMPLETE, part + " is missing from the configured template directory"));
    }
}
