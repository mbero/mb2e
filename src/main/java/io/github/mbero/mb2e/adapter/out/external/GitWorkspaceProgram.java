package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitWorkspace;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A shallow clone of one branch in {@code <workspace>/source} or {@code <workspace>/target}, kept by the git
 * program and reset to the remote before each use. The role fixes the directory, so a delete can reach no
 * other path. A clone that is corrupt, has another origin, or cannot be updated is deleted and cloned again.
 * An empty remote gives an unborn branch for the target; the first push creates the branch.
 */
public class GitWorkspaceProgram implements GitWorkspace {

    /** What the clone is for; it names the directory inside the workspace. */
    public enum Role {
        SOURCE,
        TARGET;

        String directoryName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The repository to clone: its URL, its branch, and the credentials that reach it. */
    public record Remote(String url, String branch, GitCredentials credentials) {
    }

    /** The author of the commits that the clone makes. */
    public record Author(String name, String email) {
    }

    /** The time budgets of a local git command and of a command that uses the network. */
    public record Budgets(Duration local, Duration network) {
    }

    private final Path workspace;
    private final Role role;
    private final Path directory;
    private final String url;
    private final String branch;
    private final GitCredentials credentials;
    private final Author author;
    private final Duration localBudget;
    private final Duration networkBudget;
    private final GitRunner runner = new GitRunner();

    public GitWorkspaceProgram(Path workspace, Role role, Remote remote, Author author, Budgets budgets) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.role = role;
        this.directory = this.workspace.resolve(role.directoryName());
        this.url = remote.url();
        this.branch = remote.branch();
        this.credentials = remote.credentials();
        this.author = author;
        this.localBudget = budgets.local();
        this.networkBudget = budgets.network();
    }

    @Override
    public String sync() {
        guard();
        try {
            Files.createDirectories(workspace);
        } catch (IOException failure) {
            throw new GitFailure("Cannot create " + workspace + ": " + failure.getMessage(), failure);
        }
        if (!Files.isRegularFile(directory.resolve(".git/HEAD")) || !originIsConfigured()) {
            replaceClone();
        } else {
            removeStaleLocks();
            try {
                update();
            } catch (GitTimeoutFailure timeout) {
                throw timeout;
            } catch (GitFailure failure) {
                replaceClone();
            }
        }
        return head();
    }

    @Override
    public boolean hasChanges() {
        guard();
        return !require(inside(localBudget, "status", "--porcelain", "-z", "-uall")).out().isEmpty();
    }

    @Override
    public String commitAll(String subject, String body) {
        guard();
        require(inside(localBudget, "add", "-A"));
        require(inside(localBudget, "-c", "user.name=" + author.name(), "-c", "user.email=" + author.email(),
                "-c", "commit.gpgsign=false", "commit", "-q", "-m", subject, "-m", body));
        return head();
    }

    @Override
    public PushOutcome push() {
        guard();
        GitRunner.Result result = inside(networkBudget, "push", "--porcelain", "origin",
                "HEAD:refs/heads/" + branch);
        if (result.exit() == 0) {
            return PushOutcome.PUSHED;
        }
        boolean rejected = result.out().lines().anyMatch(line -> line.startsWith("!") && line.contains("[rejected]"));
        if (rejected) {
            return PushOutcome.REJECTED;
        }
        throw result.failure("push", directory);
    }

    /** Refuses the file-system root as the workspace, before anything is deleted. */
    private void guard() {
        if (workspace.getParent() == null) {
            throw new GitFailure("Refused: the workspace must not be the file-system root: " + workspace, null);
        }
    }

    private boolean originIsConfigured() {
        GitRunner.Result origin = inside(localBudget, "remote", "get-url", "origin");
        return origin.exit() == 0 && origin.out().trim().equals(url);
    }

    private void removeStaleLocks() {
        try {
            Files.deleteIfExists(directory.resolve(".git/index.lock"));
            Files.deleteIfExists(directory.resolve(".git/shallow.lock"));
        } catch (IOException failure) {
            throw new GitFailure("Cannot remove a stale lock in " + directory + ": " + failure.getMessage(), failure);
        }
    }

    private void update() {
        String remote = "refs/remotes/origin/" + branch;
        require(inside(networkBudget, "fetch", "--depth", "1", "--no-tags", "--prune", "origin",
                "+refs/heads/" + branch + ":" + remote));
        require(inside(localBudget, "checkout", "-f", "-B", branch, remote));
        require(inside(localBudget, "reset", "--hard", remote));
        require(inside(localBudget, "clean", "-ffdx"));
    }

    private void replaceClone() {
        deleteDirectory();
        GitRunner.Result remote = outside(networkBudget, "ls-remote", "--heads", url, branch);
        require(remote);
        if (remote.out().isBlank()) {
            if (role == Role.SOURCE) {
                throw new GitFailure("The source repository has no branch '" + branch + "'.", null);
            }
            require(outside(localBudget, "init", "-q", directory.toString()));
            require(inside(localBudget, "remote", "add", "origin", url));
            require(inside(localBudget, "checkout", "-q", "-B", branch));
            return;
        }
        require(outside(networkBudget, "clone", "-q", "--depth", "1", "--single-branch", "--no-tags", "--branch",
                branch, url, directory.toString()));
    }

    private String head() {
        GitRunner.Result head = inside(localBudget, "rev-parse", "HEAD");
        return head.exit() == 0 ? head.out().trim() : "";
    }

    private void deleteDirectory() {
        if (!Files.exists(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
            });
        } catch (IOException | UncheckedIOException failure) {
            throw new GitFailure("Cannot delete " + directory + ": " + failure.getMessage(), failure);
        }
    }

    /** Runs git inside the clone; the clone's parent is a ceiling, so git never finds another repository above. */
    private GitRunner.Result inside(Duration budget, String... arguments) {
        return runner.run(directory, budget, environment(), credentials.secrets(), arguments);
    }

    /** Runs git in the workspace directory, for the commands that create the clone. */
    private GitRunner.Result outside(Duration budget, String... arguments) {
        return runner.run(workspace, budget, environment(), credentials.secrets(), arguments);
    }

    private Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>(credentials.environment());
        environment.put("GIT_CEILING_DIRECTORIES", workspace.toString());
        return environment;
    }

    private GitRunner.Result require(GitRunner.Result result) {
        return result.orFail("command", directory);
    }
}
