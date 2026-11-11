package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class Mb2eExitCodeTest {

    private record Run(int exit, String out, String err) {
    }

    private static Run run(String... args) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
        try {
            int exit = Mb2eApplication.run(args);
            return new Run(exit, capturedOut.toString(StandardCharsets.UTF_8),
                    capturedErr.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    private static Set<String> workingDirectory() {
        String[] names = new File(".").list();
        return new TreeSet<>(Arrays.asList(names == null ? new String[0] : names));
    }

    @Test
    void aListingExitsZero() {
        assertThat(run("usage").exit()).isZero();
        assertThat(run("help").exit()).isZero();
    }

    @Test
    void aRejectedParameterExitsOneNotTheFrameworkCode() {
        assertThat(run("usage", "--json").exit()).isEqualTo(1);
        assertThat(run("help", "--json").exit()).isEqualTo(1);
    }

    /** No --help, no -h and no word on either listing. */
    @Test
    void aListingRefusesEveryParameterAndEveryWord() {
        String[][] refused = {
                {"usage", "--json"}, {"usage", "--help"}, {"usage", "-h"}, {"usage", "extra"},
                {"help", "usage"}, {"help", "--json"}, {"help", "--help"}, {"help", "-h"},
                {"help", "--command", "usage"}, {"exit", "now"}};
        for (String[] args : refused) {
            Run run = run(args);
            assertThat(run.exit()).as(String.join(" ", args)).isEqualTo(1);
            assertThat(run.out()).as("stdout of %s", String.join(" ", args)).isEmpty();
        }
    }

    /** An unknown command, a disabled built-in included, exits 1. */
    @Test
    void anUnknownCommandExitsOne() {
        for (String word : new String[] {"nosuchcommand", "quit", "version", "history", "script",
                "clear", "stacktrace", "completion"}) {
            Run run = run(word);
            assertThat(run.exit()).as(word).isEqualTo(1);
            assertThat(run.err()).as("stderr of %s", word).contains(word);
        }
    }

    /** A refusal is plain text on stderr, and stdout stays clean. */
    @Test
    void aRefusalIsPlainTextOnStandardErrorOnly() {
        for (String[] args : new String[][] {{"usage", "--json"}, {"usage", "extra"}, {"nosuchcommand"}}) {
            Run run = run(args);
            String name = String.join(" ", args);
            assertThat(run.out()).as("stdout of %s", name).isEmpty();
            assertThat(run.err()).as("stderr of %s", name).isNotBlank().doesNotContain("\u001b");
            assertThat(run.out()).as("stdout of %s", name).doesNotContain("\tat ").doesNotContain("Exception");
        }
    }

    @Test
    void aRefusalNamesWhatItRefused() {
        assertThat(run("usage", "--json").err()).contains("--json");
        assertThat(run("usage", "--help").err()).contains("--help");
        assertThat(run("help", "usage").err()).contains("usage");
        assertThat(run("nosuchcommand").err()).contains("nosuchcommand");
    }

    /** Neither a listing nor a refusal creates a file in the working directory. */
    @Test
    void nothingIsWrittenToTheWorkingDirectory() {
        Set<String> before = workingDirectory();
        for (String[] args : new String[][] {{"usage"}, {"help"}, {"usage", "--json"}, {"usage", "-h"},
                {"help", "usage"}, {"history", "--file", "h.txt"}, {"script", "--file", "x"},
                {"version"}, {"quit"}, {"nosuchcommand"}}) {
            run(args);
        }
        assertThat(workingDirectory()).isEqualTo(before);
        assertThat(new File("h.txt")).doesNotExist();
        assertThat(new File("spring-shell.log")).doesNotExist();
    }
}
