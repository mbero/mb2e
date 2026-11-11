package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Everything the build reads: the directories (so an empty folder reaches the
 * tree), the Markdown files and the other files, as relative paths with forward
 * slashes. {@code location} names the datasource in a message.
 */
public record Datasource(String location, List<String> directories, List<SourceNote> notes,
        List<String> attachments) {

    public Datasource {
        directories = List.copyOf(directories);
        notes = List.copyOf(notes);
        attachments = List.copyOf(attachments);
    }
}
