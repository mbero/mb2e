package io.github.mbero.mb2e.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.port.out.GitFailure;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitRunnerTest {

    private final GitRunner runner = new GitRunner();

    @TempDir
    Path dir;

    @Test
    void aCommandThatExceedsTheBudgetFailsInTimeAndLeavesNoChildProcess() {
        long start = System.nanoTime();
        assertThatThrownBy(() -> runner.run(dir, Duration.ofMillis(500), Map.of(), List.of(),
                "-c", "alias.nap=!sleep 30", "nap"))
                .isInstanceOf(GitFailure.class).hasMessageContaining("did not answer within 500 ms");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(500 + 2000));
        assertThat(ProcessHandle.current().descendants()
                .filter(p -> p.info().command().map(c -> c.endsWith("sleep")).orElse(false))).isEmpty();
    }

    @Test
    void aSecretInStandardErrorIsMasked() {
        GitRunner.Result result = runner.run(dir, Duration.ofSeconds(5), Map.of(), List.of("s3cr3t-value"),
                "s3cr3t-value");
        assertThat(result.exit()).isNotZero();
        assertThat(result.err()).contains(GitRunner.MASK).doesNotContain("s3cr3t-value");
    }

    @Test
    void aSecretInAFailureMessageIsMasked() {
        assertThatThrownBy(() -> runner.run(dir.resolve("nowhere"), Duration.ofSeconds(5), Map.of(),
                List.of("s3cr3t-value"), "s3cr3t-value"))
                .isInstanceOf(GitFailure.class).hasMessageContaining(GitRunner.MASK)
                .hasMessageNotContaining("s3cr3t-value");
    }

    @Test
    void aGitProcessSeesNoVariableOfThisApplicationButKeepsTheRest() {
        Map<String, String> child = GitRunner.childEnvironment(Map.of("PATH", "/bin", "MB2E_TARGET_TOKEN", "t",
                "MB2E_WEBHOOK_SECRET", "s", "mb2e_lower", "x", "SPRING_APPLICATION_JSON", "{}", "HOME", "/h"),
                Map.of("GIT_CONFIG_COUNT", "1"));
        assertThat(child).containsEntry("PATH", "/bin").containsEntry("HOME", "/h")
                .containsEntry("GIT_CONFIG_COUNT", "1").containsEntry("GIT_TERMINAL_PROMPT", "0")
                .doesNotContainKeys("MB2E_TARGET_TOKEN", "MB2E_WEBHOOK_SECRET", "mb2e_lower",
                        "SPRING_APPLICATION_JSON");
    }

    @Test
    void eachInvocationSetsTheNoPromptAndNoLockVariables() {
        GitRunner.Result result = runner.run(dir, Duration.ofSeconds(5),
                Map.of("GIT_TERMINAL_PROMPT", "1", "GIT_OPTIONAL_LOCKS", "1"), List.of(),
                "-c", "alias.env=!echo $GIT_TERMINAL_PROMPT $GIT_OPTIONAL_LOCKS", "env");
        assertThat(result.out().trim()).isEqualTo("0 0");
    }
}
