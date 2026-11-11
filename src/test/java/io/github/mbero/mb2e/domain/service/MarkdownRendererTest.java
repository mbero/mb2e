package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    private static final LinkResolver NOTHING = new LinkResolver() {
        @Override
        public Optional<NoteLink> resolveNote(String target) {
            return Optional.empty();
        }

        @Override
        public Optional<String> resolveAsset(String target) {
            return Optional.empty();
        }
    };

    private static String html(String markdown) {
        return MarkdownRenderer.render(markdown, NOTHING).stream().map(Block::html)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @Test
    void anUnderscoreNeverMakesEmphasis() {
        assertThat(html("pg_stat_activity")).isEqualTo("<p>pg_stat_activity</p>");
    }

    @Test
    void rawMarkupIsEscaped() {
        assertThat(html("<script>x</script>")).isEqualTo("<p>&lt;script&gt;x&lt;/script&gt;</p>");
    }

    @Test
    void aJavascriptUrlBecomesAHashAndKeepsTheReferencesStrayParenthesis() {
        assertThat(html("[x](javascript:alert(1))")).isEqualTo("<p><a href=\"#\">x</a>)</p>");
    }

    @Test
    void aBareUrlHasItsAmpersandEscapedTwiceInTheHref() {
        assertThat(html("https://e.org/?a=1&b=2")).contains("href=\"https://e.org/?a=1&amp;amp;b=2\"");
    }

    @Test
    void anOrderedListAfterAnUnorderedOneIsAbsorbed() {
        assertThat(html("- a\n1. b")).isEqualTo("<ul><li>a</li><li>b</li></ul>");
    }

    @Test
    void aRightAlignedColumnCarriesItsStyle() {
        assertThat(html("| a | b |\n|---|--:|\n| 1 | 2 |"))
                .contains("<th style=\"text-align:right\">b</th>").contains("<td style=\"text-align:right\">2</td>");
    }

    @Test
    void aFencedBlockKeepsItsLanguageAndEscapesItsContent() {
        assertThat(html("```sql\nSELECT 'a<b';\n```"))
                .isEqualTo("<pre><code class=\"language-sql\">SELECT 'a&lt;b';</code></pre>");
    }

    @Test
    void percentEncodingFollowsEncodeUriComponent() {
        assertThat(NoteIndex.encodePath("Java/diagram ą.png")).isEqualTo("Java/diagram%20%C4%85.png");
        assertThat(Js.decodeSafe("a%20b")).isEqualTo("a b");
        assertThat(Js.decodeSafe("100%")).isEqualTo("100%");
    }
}
