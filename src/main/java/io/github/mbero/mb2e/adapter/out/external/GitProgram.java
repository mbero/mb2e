package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs the git program for three reads only: the origin URL, the branch and the porcelain status.
 * <p>A branch with no commit yet is read from its symbolic reference.
 * <p>Each invocation has its own time budget and is never retried. The status read takes
 * no optional lock, so it cannot rewrite the index.
 */
public class GitProgram implements GitRepository {

    static final long BUDGET_MS = 5000;

    private static final GitRunner RUNNER = new GitRunner();

    private final Path directory;
    private final long budgetMs;

    public GitProgram(Path directory) {
        this(directory, BUDGET_MS);
    }

    GitProgram(Path directory, long budgetMs) {
        this.directory = directory;
        this.budgetMs = budgetMs;
    }

    @Override
    public Optional<String> originUrl() {
        GitRunner.Result result = git("remote", "get-url", "origin");
        String url = result.out().trim();
        return result.exit() == 0 && !url.isEmpty() ? Optional.of(url) : Optional.empty();
    }

    @Override
    public String branch() {
        GitRunner.Result head = git("rev-parse", "--abbrev-ref", "HEAD");
        if (head.exit() != 0) {
            // A repository with no commit yet has a branch name but no HEAD commit.
            GitRunner.Result unborn = git("symbolic-ref", "--short", "-q", "HEAD");
            if (unborn.exit() == 0 && !unborn.out().isBlank()) {
                return unborn.out().trim();
            }
        }
        return require(head, "rev-parse --abbrev-ref HEAD").out().trim();
    }

    @Override
    public List<GitChange> status() {
        GitRunner.Result result = require(git("status", "--porcelain", "-z", "-uall"), "status --porcelain");
        List<GitChange> changes = new ArrayList<>();
        String[] fields = result.out().split("\0");
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (field.length() < 4) {
                continue;
            }
            String code = field.substring(0, 2);
            changes.add(new GitChange(code, field.substring(3)));
            if (code.indexOf('R') >= 0 || code.indexOf('C') >= 0) {
                i++;
            }
        }
        return changes;
    }

    private GitRunner.Result require(GitRunner.Result result, String what) {
        return result.orFail(what, directory);
    }

    private GitRunner.Result git(String... arguments) {
        return RUNNER.run(directory, Duration.ofMillis(budgetMs), Map.of(), List.of(), arguments);
    }
}
