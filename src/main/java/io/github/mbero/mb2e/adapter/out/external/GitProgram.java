package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.model.GitChange;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs the git program for three reads only: the origin URL, the branch and the porcelain status.
 * <p>A branch with no commit yet is read from its symbolic reference.
 * <p>Each invocation has its own time budget and is never retried. The status read takes
 * no optional lock, so it cannot rewrite the index.
 */
public class GitProgram implements GitRepository {

    static final long BUDGET_MS = 5000;

    private final Path directory;
    private final long budgetMs;

    public GitProgram(Path directory) {
        this(directory, BUDGET_MS);
    }

    GitProgram(Path directory, long budgetMs) {
        this.directory = directory;
        this.budgetMs = budgetMs;
    }

    private record Result(int exit, String out, String err) {
    }

    @Override
    public Optional<String> originUrl() {
        Result result = git("remote", "get-url", "origin");
        String url = result.out().trim();
        return result.exit() == 0 && !url.isEmpty() ? Optional.of(url) : Optional.empty();
    }

    @Override
    public String branch() {
        Result head = git("rev-parse", "--abbrev-ref", "HEAD");
        if (head.exit() != 0) {
            // A repository with no commit yet has a branch name but no HEAD commit.
            Result unborn = git("symbolic-ref", "--short", "-q", "HEAD");
            if (unborn.exit() == 0 && !unborn.out().isBlank()) {
                return unborn.out().trim();
            }
        }
        return require(head, "rev-parse --abbrev-ref HEAD").out().trim();
    }

    @Override
    public List<GitChange> status() {
        Result result = require(git("status", "--porcelain", "-z", "-uall"), "status --porcelain");
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

    private Result require(Result result, String what) {
        if (result.exit() != 0) {
            throw new GitFailure("git " + what + " failed in " + directory + ": " + result.err().trim(), null);
        }
        return result;
    }

    private Result git(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        String what = String.join(" ", arguments);
        Process process;
        try {
            process = builder.start();
        } catch (IOException failure) {
            throw new GitFailure("git could not be run (" + what + "): " + failure.getMessage(), failure);
        }
        try {
            process.getOutputStream().close();
            CompletableFuture<String> out = read(process.getInputStream());
            CompletableFuture<String> err = read(process.getErrorStream());
            if (!process.waitFor(budgetMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new GitFailure("git " + what + " did not answer within " + budgetMs + " ms in "
                        + directory, null);
            }
            return new Result(process.exitValue(), out.join(), err.join());
        } catch (IOException failure) {
            process.destroyForcibly();
            throw new GitFailure("git " + what + " failed: " + failure.getMessage(), failure);
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new GitFailure("git " + what + " was interrupted", interrupted);
        }
    }

    private static CompletableFuture<String> read(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                return "";
            }
        });
    }
}
