package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Bundle;
import io.github.mbero.mb2e.domain.model.NoteEntry;
import io.github.mbero.mb2e.domain.model.TreeNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BundleJsonTest {

    @Test
    void theFileIsWrittenAsJsonStringifyWritesIt() {
        Map<String, NoteEntry> notes = new LinkedHashMap<>();
        notes.put("a/b", new NoteEntry("Zażółć \"x\"", "a", "", List.of("t"), 1, "l/\u0001", ""));
        Bundle bundle = new Bundle("G", "a/b", null, List.of(new TreeNode.Folder("a", "a",
                List.of(new TreeNode.Leaf("a/b")))), List.of("a/b"), notes);
        assertThat(BundleJson.file(bundle)).isEqualTo("window.BLOG_DATA = {\"generated\":\"G\","
                + "\"defaultNote\":\"a/b\",\"about\":null,\"tree\":[{\"name\":\"a\",\"path\":\"a\","
                + "\"children\":[{\"note\":\"a/b\"}]}],\"order\":[\"a/b\"],\"notes\":{\"a/b\":{"
                + "\"title\":\"Zażółć \\\"x\\\"\",\"category\":\"a\",\"date\":\"\",\"tags\":[\"t\"],"
                + "\"minutes\":1,\"lead\":\"l/\\u0001\",\"html\":\"\"}}};\n");
    }
}
