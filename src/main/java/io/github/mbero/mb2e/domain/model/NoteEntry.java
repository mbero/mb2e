package io.github.mbero.mb2e.domain.model;

import java.util.List;

/** A note as the page script reads it: exactly these seven fields, in this order. */
public record NoteEntry(String title, String category, String date, List<Object> tags, int minutes,
        String lead, String html) {
}
