package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class Mb2eServeArgumentTest {

    private record Outcome(int code, String out, String err) {
    }

    private static Outcome capture(Supplier<Integer> call) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBytes, true));
        System.setErr(new PrintStream(errBytes, true));
        try {
            int code = call.get();
            return new Outcome(code, outBytes.toString(), errBytes.toString());
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

    @Test
    void aWordAfterServeIsRefusedInOneLineThatNamesIt() {
        Outcome outcome = capture(() -> Mb2eApplication.run("serve", "extra"));
        assertThat(outcome.code()).isEqualTo(1);
        assertThat(outcome.out()).isEmpty();
        assertThat(outcome.err().strip().lines()).hasSize(1);
        assertThat(outcome.err()).contains("extra");
    }

    @Test
    void serveWithoutVariablesIsRefusedAndNamesTheFirstMissingOne() {
        Outcome outcome = capture(() -> ServerLauncher.start(new String[] {"serve"}, Map.of(), path -> null));
        assertThat(outcome.code()).isEqualTo(1);
        assertThat(outcome.out()).isEmpty();
        assertThat(outcome.err()).contains("MB2E_SOURCE_REPO_URL");
        assertThat(outcome.err().strip().lines()).hasSize(1);
    }

    @Test
    void onlyAFirstArgumentOfServeSelectsTheServer() {
        assertThat(ServerLauncher.isServe(new String[] {"serve"})).isTrue();
        assertThat(ServerLauncher.isServe(new String[] {"run", "serve"})).isFalse();
        assertThat(ServerLauncher.isServe(new String[0])).isFalse();
        assertThat(ServerLauncher.isServe(new String[] {"SERVE"})).isFalse();
    }
}
