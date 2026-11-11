package io.github.mbero.mb2e.domain.model;

/** A build that wrote nothing: its report, and the text of assets/js/content.js it would write (null when refused). */
public record BuildPlan(BuildReport report, String bundleText) {
}
