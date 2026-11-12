package io.github.mbero.mb2e.domain.model;

import java.util.List;

/** A note as the page script reads it; the rendered body is its one html block. */
public record NoteEntry(String title, String category, String date, List<Object> tags, String html) {
}
