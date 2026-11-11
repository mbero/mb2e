package io.github.mbero.mb2e.adapter.in.shell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArgumentGuardTest {

    private static String refusal(String... args) {
        return ArgumentGuard.refusal(args).orElse(null);
    }

    @Test
    void plainWordsPass() {
        assertThat(refusal()).isNull();
        assertThat(refusal("usage")).isNull();
        assertThat(refusal("usage", "--json")).isNull();
    }

    @Test
    void anEmptyWordIsRefused() {
        for (String[] args : new String[][] {{""}, {" "}, {"usage", ""}}) {
            assertThat(refusal(args)).as(String.join("|", args)).contains("empty word");
        }
    }

    @Test
    void aBareDoubleDashIsRefused() {
        assertThat(refusal("usage", "--")).endsWith("--");
    }

    @Test
    void aWordWithASpaceIsRefusedByName() {
        assertThat(refusal("usage extra")).contains("'usage extra'");
        assertThat(refusal(" usage")).contains("' usage'");
    }

    @Test
    void scriptModeTakesTheOneFileAndNothingElse() {
        assertThat(ArgumentGuard.isScriptMode(new String[] {"@s.txt"})).isTrue();
        assertThat(refusal("@a file.txt")).isEqualTo("The script file cannot be read: a file.txt");
        assertThat(refusal("@s.txt", "extra")).endsWith("extra");
        assertThat(refusal("@s.txt", "--help")).endsWith("--help");
        assertThat(refusal("@s.txt", "--spring.shell.command.history.enabled=true")).contains("history.enabled");
    }

    @Test
    void aSecondScriptFileIsRefusedByName() {
        assertThat(refusal("@a.txt", "@b.txt")).endsWith("@b.txt");
    }

    @Test
    void aLoneAtSignIsNotScriptMode() {
        assertThat(ArgumentGuard.isScriptMode(new String[] {"@"})).isFalse();
    }

    @Test
    void aMissingOrDirectoryScriptFileIsRefusedInOneSentenceNamingIt(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws java.io.IOException {
        assertThat(refusal("@" + dir.resolve("missing"))).isEqualTo("The script file cannot be read: " + dir.resolve("missing"));
        assertThat(refusal("@" + dir)).isEqualTo("The script file cannot be read: " + dir);
        java.nio.file.Path file = java.nio.file.Files.writeString(dir.resolve("s"), "usage\n");
        assertThat(refusal("@" + file)).isNull();
    }

    @Test
    void wordRefusalJudgesOneWord() {
        assertThat(ArgumentGuard.wordRefusal("usage")).isEmpty();
        assertThat(ArgumentGuard.wordRefusal("")).get().asString().contains("empty word");
        assertThat(ArgumentGuard.wordRefusal("--")).get().asString().endsWith("--");
    }
}
