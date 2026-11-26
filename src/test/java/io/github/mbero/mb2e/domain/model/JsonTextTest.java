package io.github.mbero.mb2e.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonTextTest {

    @Test
    void nullIsTheJsonNull() {
        assertThat(JsonText.quote(null)).isEqualTo("null");
    }

    @Test
    void quotesBackslashesAndTheNamedControlCharactersAreEscaped() {
        assertThat(JsonText.quote("a\"b\\c\b\f\n\r\t")).isEqualTo("\"a\\\"b\\\\c\\b\\f\\n\\r\\t\"");
    }

    @Test
    void otherControlCharactersUseLowercaseUnicodeEscapes() {
        assertThat(JsonText.quote("\u0001\u001f")).isEqualTo("\"\\u0001\\u001f\"");
    }

    @Test
    void nonAsciiTextAndSlashesStayAsTheyAre() {
        assertThat(JsonText.quote("zażółć/gęślą")).isEqualTo("\"zażółć/gęślą\"");
    }

    @Test
    void aPairedSurrogateStaysAndALoneOneIsEscaped() {
        assertThat(JsonText.quote("\uD83D\uDE00")).isEqualTo("\"\uD83D\uDE00\"");
        assertThat(JsonText.quote("\uD83Dx")).isEqualTo("\"\\ud83dx\"");
        assertThat(JsonText.quote("x\uDE00")).isEqualTo("\"x\\ude00\"");
        assertThat(JsonText.quote("\uD83D")).isEqualTo("\"\\ud83d\"");
    }
}
