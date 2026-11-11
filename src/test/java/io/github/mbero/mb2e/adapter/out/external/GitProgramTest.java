package io.github.mbero.mb2e.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitProgramTest {

    @TempDir
    Path repo;

    private void git(String... arguments) throws IOException, InterruptedException {
        java.util.List<String> command = new java.util.ArrayList<>(List.of("git", "-c", "user.name=t",
                "-c", "user.email=t@example.com", "-c", "commit.gpgsign=false"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor()).isZero();
    }

    @Test
    void itReadsTheOriginTheBranchAndTheStatusOfAWorkingCopy() throws Exception {
        git("init", "-q", "-b", "main");
        git("remote", "add", "origin", "git@github.com:x/mbero.github.io.git");
        Files.writeString(repo.resolve("a.txt"), "a");
        git("add", "-A");
        git("commit", "-q", "-m", "first");
        Files.createDirectories(repo.resolve("assets/ś"));
        Files.writeString(repo.resolve("assets/ś/n.txt"), "n");
        Files.writeString(repo.resolve("a.txt"), "changed");

        GitProgram program = new GitProgram(repo);
        assertThat(program.originUrl()).contains("git@github.com:x/mbero.github.io.git");
        assertThat(program.branch()).isEqualTo("main");
        assertThat(program.status()).containsExactlyInAnyOrder(
                new GitChange(" M", "a.txt"), new GitChange("??", "assets/ś/n.txt"));
    }

    @Test
    void aRepositoryWithoutACommitYieldsItsBranchAndItsStatus() throws Exception {
        git("init", "-q", "-b", "pages");
        git("remote", "add", "origin", "git@github.com:x/mbero.github.io.git");
        Files.writeString(repo.resolve("new.txt"), "n");

        GitProgram program = new GitProgram(repo);
        assertThat(program.branch()).isEqualTo("pages");
        assertThat(program.status()).containsExactly(new GitChange("??", "new.txt"));
    }

    @Test
    void aMissingDirectoryIsAGitFailureNotAnException() {
        assertThatThrownBy(() -> new GitProgram(repo.resolve("nowhere")).branch()).isInstanceOf(GitFailure.class);
    }
}
