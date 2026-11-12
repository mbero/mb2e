package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.NoteEntry;
import io.github.mbero.mb2e.domain.model.SiteData;
import io.github.mbero.mb2e.domain.model.TreeNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SiteDataJsonTest {

    @Test
    void theDocumentHasTheKeysOfThePageScriptInOrderAndOneHtmlBlockPerNote() {
        Map<String, NoteEntry> notes = new LinkedHashMap<>();
        notes.put("A/x", new NoteEntry("Tytuł", "A", "2026-11-05", List.of("t", "u"), "<p>ą \"x\"</p>"));
        String script = SiteDataJson.script(new SiteData("A/x",
                List.of(new TreeNode.Folder("A", List.of(new TreeNode.Leaf("A/x")))), notes));
        assertThat(script).isEqualTo("window.BLOG_DATA = {\"defaultNote\":\"A/x\",\"TREE\":[{\"name\":\"A\","
                + "\"children\":[{\"note\":\"A/x\"}]}],\"NOTES\":{\"A/x\":{\"title\":\"Tytuł\",\"category\":\"A\","
                + "\"date\":\"2026-11-05\",\"tags\":[\"t\",\"u\"],\"blocks\":[{\"type\":\"html\","
                + "\"html\":\"<p>ą \\\"x\\\"</p>\"}]}}};\n");
    }

    @Test
    void anEmptyNoteKeepsItsOneBlock() {
        Map<String, NoteEntry> notes = new LinkedHashMap<>();
        notes.put("x", new NoteEntry("x", "", "", List.of(), ""));
        assertThat(SiteDataJson.script(new SiteData("x", List.of(), notes)))
                .contains("\"tags\":[],\"blocks\":[{\"type\":\"html\",\"html\":\"\"}]");
    }
}
