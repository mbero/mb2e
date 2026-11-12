package io.github.mbero.mb2e.domain.model;

import java.util.List;
import java.util.Map;

/** The document {@code window.BLOG_DATA} holds: the start note, the side-panel tree and every note by id. */
public record SiteData(String defaultNote, List<TreeNode> tree, Map<String, NoteEntry> notes) {
}
