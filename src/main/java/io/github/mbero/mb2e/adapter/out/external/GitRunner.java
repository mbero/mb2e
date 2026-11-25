package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.port.out.GitFailure;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs one git program invocation with a time budget and returns its exit code and text.
 * <p>A command that exceeds the budget is killed together with its child processes. Secret
 * values are replaced by {@code ***} in every text that leaves this class, including the
 * message of a {@link GitFailure}. Git never prompts and never takes an optional lock.
 */
final class GitRunner {

    static final String MASK = "***";

    /** How long the readers may take to finish once the process is gone. */
    private static final Duration DRAIN = Duration.ofSeconds(1);

    /** The outcome of a finished invocation; both texts are already masked. */
    record Result(int exit, String out, String err) {
    }

    /**
     * Runs {@code git} with the given arguments and masks every non-blank secret in the texts it returns.
     *
     * @throws GitFailure when git cannot start, exceeds the budget, or is interrupted
     */
    Result run(Path directory, Duration budget, Map<String, String> extraEnvironment, List<String> secrets,
            String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().putAll(extraEnvironment);
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        String what = mask(String.join(" ", arguments), secrets);
        Process process;
        try {
            process = builder.start();
        } catch (IOException failure) {
            throw new GitFailure("git could not be run (" + what + "): " + mask(failure.getMessage(), secrets),
                    failure);
        }
        try {
            process.getOutputStream().close();
            CompletableFuture<String> out = read(process.getInputStream());
            CompletableFuture<String> err = read(process.getErrorStream());
            if (!process.waitFor(budget.toMillis(), TimeUnit.MILLISECONDS)) {
                kill(process);
                throw new GitFailure("git " + what + " did not answer within " + budget.toMillis() + " ms in "
                        + directory, null);
            }
            return new Result(process.exitValue(), mask(drain(out), secrets), mask(drain(err), secrets));
        } catch (IOException failure) {
            kill(process);
            throw new GitFailure("git " + what + " failed: " + mask(failure.getMessage(), secrets), failure);
        } catch (InterruptedException interrupted) {
            kill(process);
            Thread.currentThread().interrupt();
            throw new GitFailure("git " + what + " was interrupted", interrupted);
        }
    }

    /** Kills the children first, so that none is left holding the output pipes. */
    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        try {
            process.waitFor(DRAIN.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static String drain(CompletableFuture<String> text) {
        try {
            return text.get(DRAIN.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return "";
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
            return "";
        }
    }

    static String mask(String text, List<String> secrets) {
        if (text == null) {
            return null;
        }
        String masked = text;
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                masked = masked.replace(secret, MASK);
            }
        }
        return masked;
    }

    private static CompletableFuture<String> read(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                return "";
            }
        });
    }
}
