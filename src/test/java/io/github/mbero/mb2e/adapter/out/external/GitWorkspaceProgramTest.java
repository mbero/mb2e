package io.github.mbero.mb2e.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitWorkspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the real git program against bare repositories in a temporary directory. */
class GitWorkspaceProgramTest {

    @TempDir
    Path temp;

    private Path bare;
    private String url;

    @BeforeEach
    void bareRemote() throws Exception {
        bare = temp.resolve("remote.git");
        git(temp, "init", "-q", "--bare", "-b", "main", bare.toString());
        url = bare.toUri().toString();
    }

    private static String git(Path in, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git", "-c", "user.name=seed", "-c", "user.email=s@example.com",
                "-c", "commit.gpgsign=false"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(in.toFile()).redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).as(String.join(" ", arguments) + ": " + out).isZero();
        return out.trim();
    }

    /** Pushes one commit that writes a file to the bare remote, through a throw-away clone. */
    private String seed(String file, String content) throws Exception {
        Path work = Files.createTempDirectory(temp, "seed");
        if (git(temp, "ls-remote", "--heads", url, "main").isEmpty()) {
            git(work, "init", "-q", "-b", "main");
            git(work, "remote", "add", "origin", url);
        } else {
            git(temp, "clone", "-q", url, work.toString());
        }
        Files.writeString(work.resolve(file), content);
        git(work, "add", "-A");
        git(work, "commit", "-q", "-m", "seed " + file);
        git(work, "push", "-q", "origin", "HEAD:refs/heads/main");
        return git(work, "rev-parse", "HEAD");
    }

    private GitWorkspaceProgram workspace(Path directory) {
        return workspace(directory, url);
    }

    /** Builds the clone for a directory named {@code source} or {@code target}, as the role of that name. */
    private GitWorkspaceProgram workspace(Path directory, String remote) {
        return workspace(directory, remote, "main");
    }

    private GitWorkspaceProgram workspace(Path directory, String remote, String branch) {
        GitWorkspaceProgram.Role role = GitWorkspaceProgram.Role.valueOf(
                directory.getFileName().toString().toUpperCase(java.util.Locale.ROOT));
        return new GitWorkspaceProgram(directory.getParent(), role,
                new GitWorkspaceProgram.Remote(remote, branch, GitCredentials.none()),
                new GitWorkspaceProgram.Author("Mb2e Bot", "bot@example.com"),
                new GitWorkspaceProgram.Budgets(Duration.ofSeconds(30), Duration.ofSeconds(60)));
    }

    private Path target(String owner) {
        return temp.resolve(owner).resolve("target");
    }

    @Test
    void theFirstSyncMakesAShallowCloneAndReturnsTheCommit() throws Exception {
        seed("a.txt", "1");
        String second = seed("b.txt", "2");
        Path dir = target("w1");
        assertThat(workspace(dir).sync()).isEqualTo(second);
        assertThat(git(dir, "rev-list", "--count", "HEAD")).isEqualTo("1");
        assertThat(dir.resolve("b.txt")).hasContent("2");
    }

    @Test
    void aLaterSyncReturnsTheNewRemoteCommit() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w2"));
        workspace.sync();
        String next = seed("c.txt", "3");
        assertThat(workspace.sync()).isEqualTo(next);
        assertThat(target("w2").resolve("c.txt")).exists();
    }

    @Test
    void syncRemovesUntrackedFilesAndRevertsModifiedOnes() throws Exception {
        seed("a.txt", "original");
        GitWorkspaceProgram workspace = workspace(target("w3"));
        workspace.sync();
        Files.writeString(target("w3").resolve("a.txt"), "edited");
        Files.writeString(target("w3").resolve("junk.txt"), "junk");
        Files.createDirectories(target("w3").resolve("deep/dir"));
        Files.writeString(target("w3").resolve("deep/dir/x"), "x");
        assertThat(workspace.hasChanges()).isTrue();
        workspace.sync();
        assertThat(target("w3").resolve("a.txt")).hasContent("original");
        assertThat(target("w3").resolve("junk.txt")).doesNotExist();
        assertThat(target("w3").resolve("deep")).doesNotExist();
        assertThat(workspace.hasChanges()).isFalse();
    }

    @Test
    void aStaleIndexLockDoesNotFailTheSync() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w4"));
        workspace.sync();
        Files.writeString(target("w4").resolve(".git/index.lock"), "");
        Files.writeString(target("w4").resolve(".git/shallow.lock"), "");
        String next = seed("b.txt", "2");
        assertThat(workspace.sync()).isEqualTo(next);
        assertThat(target("w4").resolve(".git/index.lock")).doesNotExist();
    }

    @Test
    void aCorruptCloneIsReplacedByANewOne() throws Exception {
        String commit = seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w5"));
        workspace.sync();
        Files.delete(target("w5").resolve(".git/HEAD"));
        assertThat(workspace.sync()).isEqualTo(commit);
        assertThat(git(target("w5"), "rev-parse", "--abbrev-ref", "HEAD")).isEqualTo("main");
    }

    @Test
    void aCloneWithAnotherOriginIsReplacedByANewOne() throws Exception {
        String commit = seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w6"));
        workspace.sync();
        git(target("w6"), "remote", "set-url", "origin", temp.resolve("elsewhere.git").toUri().toString());
        assertThat(workspace.sync()).isEqualTo(commit);
        assertThat(git(target("w6"), "remote", "get-url", "origin")).isEqualTo(url);
    }

    @Test
    void anEmptyRemoteGivesAnUnbornBranchAndTheFirstPushCreatesIt() throws Exception {
        GitWorkspaceProgram workspace = workspace(target("w7"));
        assertThat(workspace.sync()).isEmpty();
        assertThat(git(target("w7"), "symbolic-ref", "--short", "HEAD")).isEqualTo("main");
        Files.writeString(target("w7").resolve("index.html"), "<html>");
        assertThat(workspace.hasChanges()).isTrue();
        String commit = workspace.commitAll("first", "Source commit: abc");
        assertThat(workspace.push()).isEqualTo(GitWorkspace.PushOutcome.PUSHED);
        assertThat(git(bare, "rev-parse", "main")).isEqualTo(commit);
    }

    @Test
    void anUnbornCloneIsUsableAgainOnTheNextSyncWhileTheRemoteStaysEmpty() throws Exception {
        GitWorkspaceProgram workspace = workspace(target("w8"));
        workspace.sync();
        assertThat(workspace.sync()).isEmpty();
    }

    @Test
    void anEmptySourceRemoteIsAFailure() {
        assertThatThrownBy(() -> workspace(temp.resolve("w9").resolve("source")).sync())
                .isInstanceOf(GitFailure.class).hasMessageContaining("no branch 'main'");
    }

    @Test
    void aPushIsRejectedWhenAnotherCloneWasFirst() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram first = workspace(target("a"));
        GitWorkspaceProgram second = workspace(target("b"));
        first.sync();
        second.sync();
        Files.writeString(target("a").resolve("a1.txt"), "x");
        Files.writeString(target("b").resolve("b1.txt"), "y");
        String winner = first.commitAll("one", "body");
        second.commitAll("two", "body");
        assertThat(first.push()).isEqualTo(GitWorkspace.PushOutcome.PUSHED);
        assertThat(second.push()).isEqualTo(GitWorkspace.PushOutcome.REJECTED);
        assertThat(git(bare, "rev-parse", "main")).isEqualTo(winner);
    }

    @Test
    void aPushToAnUnreachableRemoteIsAFailureNotARejection() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w10"));
        workspace.sync();
        Files.writeString(target("w10").resolve("n.txt"), "n");
        workspace.commitAll("s", "b");
        git(target("w10"), "remote", "set-url", "origin", temp.resolve("gone.git").toUri().toString());
        assertThatThrownBy(workspace::push).isInstanceOf(GitFailure.class);
    }

    @Test
    void commitAllWritesTheConfiguredAuthorSubjectAndBody() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(target("w11"));
        workspace.sync();
        Files.writeString(target("w11").resolve("n.txt"), "n");
        workspace.commitAll("chore: refresh", "Source commit: abc123");
        assertThat(git(target("w11"), "log", "-1", "--format=%an%n%ae")).isEqualTo("Mb2e Bot\nbot@example.com");
        assertThat(git(target("w11"), "log", "-1", "--format=%s")).isEqualTo("chore: refresh");
        assertThat(git(target("w11"), "log", "-1", "--format=%b")).isEqualTo("Source commit: abc123");
    }

    @Test
    void theFileSystemRootIsRefusedAsAWorkspaceBeforeAnyDelete() {
        GitWorkspaceProgram workspace = new GitWorkspaceProgram(Path.of("/"), GitWorkspaceProgram.Role.TARGET,
                new GitWorkspaceProgram.Remote(url, "main", GitCredentials.none()),
                new GitWorkspaceProgram.Author("n", "e@example.com"),
                new GitWorkspaceProgram.Budgets(Duration.ofSeconds(30), Duration.ofSeconds(60)));
        assertThatThrownBy(workspace::sync).isInstanceOf(GitFailure.class).hasMessageContaining("Refused");
        assertThatThrownBy(workspace::hasChanges).isInstanceOf(GitFailure.class);
        assertThatThrownBy(() -> workspace.commitAll("s", "b")).isInstanceOf(GitFailure.class);
        assertThatThrownBy(workspace::push).isInstanceOf(GitFailure.class);
    }

    @Test
    void aCloneTouchesOnlyItsOwnDirectoryInTheWorkspace() throws Exception {
        seed("a.txt", "1");
        Path workspace = Files.createDirectories(temp.resolve("w13"));
        Files.writeString(workspace.resolve("keep.txt"), "keep");
        Files.createDirectories(workspace.resolve("source"));
        Files.writeString(workspace.resolve("source/keep.txt"), "keep");
        workspace(workspace.resolve("target")).sync();
        assertThat(workspace.resolve("keep.txt")).hasContent("keep");
        assertThat(workspace.resolve("source/keep.txt")).hasContent("keep");
        assertThat(workspace.resolve("target/a.txt")).hasContent("1");
    }

    @Test
    void aMissingRemoteBranchOfAnExistingCloneFallsBackToANewCloneAndFailsClearly() throws Exception {
        seed("a.txt", "1");
        GitWorkspaceProgram workspace = workspace(temp.resolve("w12").resolve("source"), url, "nope");
        assertThatThrownBy(workspace::sync).isInstanceOf(GitFailure.class).hasMessageContaining("no branch 'nope'");
    }
}
