package io.github.mbero.mb2e.domain.model;

/** The outcome of a build: its report, and the site it assembled, which is null when the build was refused. */
public record Assembly(BuildReport report, GeneratedSite site) {
}
