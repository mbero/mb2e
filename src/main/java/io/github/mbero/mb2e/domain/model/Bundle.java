package io.github.mbero.mb2e.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Content bundle with the keys generated, defaultNote, about, tree, order and notes, in this order.
 * <p>{@code about} is null when there is no about note and stays present in the serialized form.
 */
public record Bundle(String generated, String defaultNote, String about, List<TreeNode> tree,
        List<String> order, Map<String, NoteEntry> notes) {
}
