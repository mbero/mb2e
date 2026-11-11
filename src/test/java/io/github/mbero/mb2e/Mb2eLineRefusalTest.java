package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.adapter.in.shell.RefusalReporter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;

/**
 * Verifies that an empty word and a bare "--" are refused on a script line and at the prompt as
 * on the command line, that process arguments are never tokenized twice, and that a bad script
 * file is reported in one plain sentence.
 */
@SpringBootTest(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.shell.script.enabled=false"})
class Mb2eLineRefusalTest {

    private static final String[] REFUSED_LINES = {"usage \"\"", "usage --", "help \"\"", "help --", "usage ''"};

    @TempDir
    Path dir;

    @Autowired
    Shell shell;

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

    @Test
    void anEmptyWordAndABareDoubleDashAreRefusedOnAScriptLine() throws IOException {
        for (String line : REFUSED_LINES) {
            Path file = Files.writeString(dir.resolve("s"), line + "\n");
            Run run = run("@" + file);
            assertThat(run.exit()).as(line).isEqualTo(1);
            assertThat(run.out()).as("stdout of %s", line).isEmpty();
            assertThat(run.err()).as("stderr of %s", line).isNotBlank().doesNotContain("\tat ");
        }
        Path ok = Files.writeString(dir.resolve("ok"), "usage\n");
        assertThat(run("@" + ok).exit()).isZero();
    }

    /** Feeds one typed line through the shell and returns what it wrote to standard error. */
    private String type(String raw, String... words) {
        ByteArrayOutputStream e = new ByteArrayOutputStream();
        PrintStream err = System.err;
        System.setErr(new PrintStream(e, true, StandardCharsets.UTF_8));
        try {
            shell.run(new InputProvider() {
                private boolean given;

                public Input readInput() {
                    if (given) {
                        return null;
                    }
                    given = true;
                    return new Input() {
                        public String rawText() {
                            return raw;
                        }

                        public List<String> words() {
                            return List.of(words);
                        }
                    };
                }
            });
        } catch (Exception ignored) {
            // outside an interactive session the shell rethrows a refusal
        } finally {
            System.setErr(err);
        }
        return e.toString(StandardCharsets.UTF_8);
    }

    @Test
    void anEmptyWordAndABareDoubleDashAreRefusedAtThePrompt() {
        assertThat(type("usage \"\"", "usage", "")).contains("empty word");
        assertThat(type("usage --", "usage", "--")).contains("--");
        assertThat(type("help \"\"", "help", "")).contains("empty word");
        assertThat(type("help --", "help", "--")).contains("--");
        assertThat(type("\"\"", "")).contains("empty word");
    }

    @Test
    void aBlankLineAtThePromptIsNoInputNotARefusal() {
        assertThat(type("", "")).isEmpty();
        assertThat(type("   ", "")).isEmpty();
    }

    @Test
    void processArgumentsAreNeverTokenizedASecondTime() {
        for (String[] args : new String[][] {{"\"\""}, {"''"}, {"usage", "\"\""}, {"usage", "\\"}, {"us\\age"},
                {"\"usage\""}, {"help", "''"}}) {
            Run run = run(args);
            String name = String.join("|", args);
            assertThat(run.exit()).as(name).isEqualTo(1);
            assertThat(run.out()).as("stdout of %s", name).isEmpty();
            assertThat(run.err()).as("stderr of %s", name).isNotBlank();
        }
        assertThat(run("usage").exit()).isZero();
    }

    @Test
    void aMissingScriptFileOrADirectoryIsOnePlainSentenceNamingIt() {
        for (String target : new String[] {dir.resolve("missing").toString(), dir.toString()}) {
            Run run = run("@" + target);
            assertThat(run.exit()).isEqualTo(1);
            assertThat(run.out()).isEmpty();
            assertThat(run.err().strip()).isEqualTo("The script file cannot be read: " + target);
        }
    }

    @Test
    void aRefusalPrintsNoFailureLogAndNoTrace() {
        for (String[] args : new String[][] {{"exit"}, {"nosuchcommand"}, {"usage", "extra"}, {"usage", "--json"}}) {
            Run run = run(args);
            String name = String.join(" ", args);
            assertThat(run.exit()).as(name).isEqualTo(1);
            assertThat(run.err()).as("stderr of %s", name)
                    .doesNotContain("Application run failed", "\tat ", "Exception", "ERROR");
            assertThat(run.err().strip().lines().count()).as("lines of %s", name).isEqualTo(1);
        }
        assertThat(run("exit").err().strip()).endsWith("exit");
        assertThat(RefusalReporter.reported()).isTrue();
    }
}
