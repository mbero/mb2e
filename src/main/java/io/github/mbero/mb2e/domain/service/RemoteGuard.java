package io.github.mbero.mb2e.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether an origin URL names a GitHub Pages repository of its owner.
 * <p>Two URL forms count, each with or without a trailing {@code .git}:
 * {@code https://github.com/<owner>/<owner>.github.io} and
 * {@code git@github.com:<owner>/<owner>.github.io}. Letter case is ignored.
 */
final class RemoteGuard {

    private static final String OWNER = "([A-Za-z0-9][A-Za-z0-9._-]*)";
    private static final Pattern HTTPS = Pattern.compile(
            "https://github\\.com/" + OWNER + "/" + OWNER + "\\.github\\.io(?:\\.git)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SSH = Pattern.compile(
            "git@github\\.com:" + OWNER + "/" + OWNER + "\\.github\\.io(?:\\.git)?", Pattern.CASE_INSENSITIVE);

    private RemoteGuard() {
    }

    /** Returns whether the URL is a Pages repository whose name carries the owner's own name. */
    static boolean isPagesRepository(String url) {
        String trimmed = url.trim();
        return ownedByName(HTTPS.matcher(trimmed)) || ownedByName(SSH.matcher(trimmed));
    }

    private static boolean ownedByName(Matcher matcher) {
        return matcher.matches() && matcher.group(1).equalsIgnoreCase(matcher.group(2));
    }
}
