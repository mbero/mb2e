package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.port.out.SiteSource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateCheckTest {

    /** A template held in memory: path to text. */
    private static SiteSource template(Map<String, String> files) {
        return new SiteSource() {
            @Override
            public String location() {
                return "/template";
            }

            @Override
            public boolean isFile(String path) {
                return files.containsKey(path);
            }

            @Override
            public boolean isDirectory(String path) {
                return files.keySet().stream().anyMatch(name -> name.startsWith(path + "/"));
            }

            @Override
            public byte[] read(String path) {
                return files.get(path).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public List<String> files(String directory) {
                return files.keySet().stream().filter(name -> name.startsWith(directory + "/")).sorted().toList();
            }
        };
    }

    private static Map<String, String> good() {
        Map<String, String> files = new HashMap<>();
        files.put("index.html", "<html></html>");
        files.put("assets/js/app.js", "case 'html': return b.html;");
        return files;
    }

    @Test
    void aCompleteTemplatePasses() {
        assertThat(TemplateCheck.shape(template(good()))).isEmpty();
    }

    @Test
    void theMissingPartIsNamed() {
        Map<String, String> noPage = good();
        noPage.remove("index.html");
        assertThat(TemplateCheck.shape(template(noPage)).orElseThrow().detail()).contains("index.html");
        Map<String, String> noScript = good();
        noScript.remove("assets/js/app.js");
        assertThat(TemplateCheck.shape(template(noScript)).orElseThrow().detail()).contains("assets/js/app.js");
    }

    @Test
    void aScriptWithoutHtmlBlockSupportIsRefusedNamingTheMarker() {
        Map<String, String> files = good();
        files.put("assets/js/app.js", "case 'p': return b.text;");
        assertThat(TemplateCheck.shape(template(files)).orElseThrow().detail()).contains("case 'html'");
    }

    @Test
    void aTemplateHoldingNoteImagesIsRefused() {
        Map<String, String> files = good();
        files.put("assets/img/notes/a.png", "x");
        assertThat(TemplateCheck.shape(template(files)).orElseThrow().detail()).contains("assets/img/notes");
    }
}
