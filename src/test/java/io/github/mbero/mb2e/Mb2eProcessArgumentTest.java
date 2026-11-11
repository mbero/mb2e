package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that a process argument never becomes a property in any mode, with script mode
 * enabled as in application.yaml (other context tests switch it off). The real application
 * runs for each case.
 */
class Mb2eProcessArgumentTest {

    @TempDir
    Path dir;

    private record Run(int exit, String out, String err) {
    }

    private static Run run(String... args) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        System.setOut(new PrintStream(o, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(e, true, StandardCharsets.UTF_8));
        try {
            int exit = Mb2eApplication.run(args);
            return new Run(exit, o.toString(StandardCharsets.UTF_8), e.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    private Path script(String name, String... lines) throws IOException {
        return Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n");
    }

    @Test
    void aSpringPropertyWordIsJustARefusedWord() {
        for (String word : new String[] {"--spring.main.banner-mode=console", "--mb2e.vault.path=/x", "--tag=x",
                "--spring.shell.command.history.enabled=true"}) {
            Run run = run("usage", word);
            assertThat(run.exit()).as(word).isEqualTo(1);
            assertThat(run.out()).as("stdout of %s", word).isEmpty();
            assertThat(run.err()).as("stderr of %s", word).contains(word);
        }
    }

    @Test
    void scriptModeRefusesEveryWordBesideTheFileBeforeAnyLineRuns() throws IOException {
        Path file = script("s.txt", "usage");
        assertThat(run("@" + file).exit()).isZero();
        for (String word : new String[] {"extra", "--help", "-h", "--tag=x",
                "--spring.shell.command.history.enabled=true", "--spring.main.banner-mode=console"}) {
            Run run = run("@" + file, word);
            assertThat(run.exit()).as(word).isEqualTo(1);
            assertThat(run.out()).as("no line ran for %s", word).isEmpty();
            assertThat(run.err()).as("stderr of %s", word).contains(word);
        }
    }

    @Test
    void aSecondScriptFileIsRefusedAndNeitherRuns() throws IOException {
        Path a = script("a.txt", "usage");
        Path b = script("b.txt", "usage");
        Run run = run("@" + a, "@" + b);
        assertThat(run.exit()).isEqualTo(1);
        assertThat(run.out()).isEmpty();
        assertThat(run.err()).contains("@" + b);
    }

    @Test
    void historyStaysGoneEvenWhenAWordOrConfigurationSwitchesItOn() throws IOException {
        Path target = dir.resolve("h.txt");
        Path file = script("h.txt.script", "history --file " + target);
        assertThat(run("@" + file).exit()).isEqualTo(1);
        assertThat(run("@" + file, "--spring.shell.command.history.enabled=true").exit()).isEqualTo(1);
        assertThat(run("history", "--file", target.toString(), "--spring.shell.command.history.enabled=true")
                .exit()).isEqualTo(1);
        assertThat(target).doesNotExist();
    }

    @Test
    void theBuiltInsCannotBeSwitchedOnByConfiguration() {
        String key = "SPRING_APPLICATION_JSON";
        String json = "{\"spring\":{\"shell\":{\"command\":{\"history\":{\"enabled\":true},\"version\":{\"enabled\":true},"
                + "\"quit\":{\"enabled\":true},\"script\":{\"enabled\":true},\"clear\":{\"enabled\":true},"
                + "\"stacktrace\":{\"enabled\":true},\"help\":{\"enabled\":true}},\"help\":{\"enabled\":true}}}}";
        // Environment variables cannot be set in-process; the same property source is
        // reachable as a system property, which SpringApplication reads at the same level
        // of the framework's property resolution, and which an excluded auto-configuration
        // ignores equally.
        System.setProperty("spring.application.json", json);
        try {
            for (String word : new String[] {"version", "quit", "history", "script", "clear", "stacktrace"}) {
                Run run = run(word);
                assertThat(run.exit()).as(word).isEqualTo(1);
                assertThat(run.out()).as("stdout of %s", word).isEmpty();
            }
            Run help = run("usage", "--help");
            assertThat(help.exit()).isEqualTo(1);
            assertThat(help.out()).isEmpty();
        } finally {
            System.clearProperty("spring.application.json");
        }
        assertThat(key).isNotBlank();
    }

    @Test
    void oneWordWithASpaceAnEmptyWordAndExitAreRefusedInPlainText() {
        for (String[] args : new String[][] {{"usage extra"}, {" usage"}, {"exit"}, {""}, {"usage", ""}, {"usage", "--"}}) {
            Run run = run(args);
            String name = String.join("|", args);
            assertThat(run.exit()).as(name).isEqualTo(1);
            assertThat(run.out()).as("stdout of %s", name).isEmpty();
            assertThat(run.err()).as("stderr of %s", name).isNotBlank()
                    .doesNotContain("NullPointerException", "ExitRequest", "Cannot invoke");
        }
        assertThat(run("usage extra").err()).contains("usage extra");
        assertThat(run("exit").err()).contains("exit");
    }

    @Test
    void exitAsAScriptLineIsRefusedByName() throws IOException {
        Run run = run("@" + script("e.txt", "exit"));
        assertThat(run.exit()).isEqualTo(1);
        assertThat(run.err()).contains("exit").doesNotContain("ExitRequest");
    }
}
