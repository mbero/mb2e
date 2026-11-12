package io.github.mbero.mb2e.domain.model;

import java.util.List;

/** A node of the side-panel tree: a folder with children, or a leaf naming a note. */
public sealed interface TreeNode {

    record Folder(String name, List<TreeNode> children) implements TreeNode {
    }

    record Leaf(String note) implements TreeNode {
    }
}
