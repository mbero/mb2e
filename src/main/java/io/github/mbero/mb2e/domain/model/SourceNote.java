package io.github.mbero.mb2e.domain.model;

/** One Markdown file of the datasource: its path relative to the root, extension included, and its text. */
public record SourceNote(String path, String text) {
}
