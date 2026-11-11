package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.model.Bundle;
import io.github.mbero.mb2e.domain.model.JsonText;
import io.github.mbero.mb2e.domain.model.NoteEntry;
import io.github.mbero.mb2e.domain.model.TreeNode;
import java.util.List;
import java.util.Map;

/**
 * Writes the bundle as JavaScript's {@code JSON.stringify} does: no white space, keys in
 * record order, wrapped in {@code window.BLOG_DATA = ...;\n}.
 * <p>Written by hand because a general JSON library differs in at least one escape by default.
 */
final class BundleJson {

    private BundleJson() {
    }

    static String file(Bundle bundle) {
        StringBuilder out = new StringBuilder("window.BLOG_DATA = ");
        out.append("{\"generated\":").append(JsonText.quote(bundle.generated()))
                .append(",\"defaultNote\":").append(JsonText.quote(bundle.defaultNote()))
                .append(",\"about\":").append(JsonText.quote(bundle.about()))
                .append(",\"tree\":");
        tree(out, bundle.tree());
        out.append(",\"order\":");
        strings(out, bundle.order());
        out.append(",\"notes\":{");
        boolean first = true;
        for (Map.Entry<String, NoteEntry> entry : bundle.notes().entrySet()) {
            out.append(first ? "" : ",").append(JsonText.quote(entry.getKey())).append(':');
            note(out, entry.getValue());
            first = false;
        }
        return out.append("}};\n").toString();
    }

    private static void tree(StringBuilder out, List<TreeNode> nodes) {
        out.append('[');
        for (int i = 0; i < nodes.size(); i++) {
            out.append(i > 0 ? "," : "");
            switch (nodes.get(i)) {
                case TreeNode.Leaf leaf -> out.append("{\"note\":").append(JsonText.quote(leaf.note())).append('}');
                case TreeNode.Folder folder -> {
                    out.append("{\"name\":").append(JsonText.quote(folder.name()))
                            .append(",\"path\":").append(JsonText.quote(folder.path()))
                            .append(",\"children\":");
                    tree(out, folder.children());
                    out.append('}');
                }
            }
        }
        out.append(']');
    }

    private static void strings(StringBuilder out, List<?> values) {
        out.append('[');
        for (int i = 0; i < values.size(); i++) {
            out.append(i > 0 ? "," : "");
            Object value = values.get(i);
            if (value instanceof List<?> nested) {
                strings(out, nested);
            } else {
                out.append(JsonText.quote(String.valueOf(value)));
            }
        }
        out.append(']');
    }

    private static void note(StringBuilder out, NoteEntry note) {
        out.append("{\"title\":").append(JsonText.quote(note.title()))
                .append(",\"category\":").append(JsonText.quote(note.category()))
                .append(",\"date\":").append(JsonText.quote(note.date()))
                .append(",\"tags\":");
        strings(out, note.tags());
        out.append(",\"minutes\":").append(note.minutes())
                .append(",\"lead\":").append(JsonText.quote(note.lead()))
                .append(",\"html\":").append(JsonText.quote(note.html()))
                .append('}');
    }
}
