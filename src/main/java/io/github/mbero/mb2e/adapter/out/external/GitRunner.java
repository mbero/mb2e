package io.github.mbero.mb2e.adapter.out.external;

import io.github.mbero.mb2e.domain.port.out.GitFailure;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs one git program invocation with a time budget and returns its exit code and text.
 * <p>A command that exceeds the budget is killed together with its child processes. Secret
 * values are replaced by {@code ***} in every text that leaves this class, including the
 * message of a {@link GitFailure}. Git never prompts and never takes an optional lock, and it never
 * sees a variable of this application, so a process holds no secret but its own credentials.
 */
final class GitRunner {

    static final String MASK = "***";

    /** How long the readers may take to finish once the process is gone. */
    private static final Duration DRAIN = Duration.ofSeconds(10);

    /** The prefix of the variables that hold this application's settings and secrets. */
    private static final String OWN_PREFIX = "MB2E_";

    /** The outcome of a finished invocation; both texts are already masked. */
    record Result(int exit, String out, String err) {

        /** Returns this result when git succeeded; otherwise the failure of {@link #failure}. */
        Result orFail(String what, Object where) {
            if (exit != 0) {
                throw failure(what, where);
            }
            return this;
        }

        /** Returns the failure that names the command, the place and git's error. */
        GitFailure failure(String what, Object where) {
            return new GitFailure("git " + what + " failed in " + where + ": " + err.trim(), null);
        }
    }

    /**
     * Runs {@code git} with the given arguments and masks every non-blank secret in the texts it returns.
     *
     * @throws GitFailure when git cannot start or is interrupted; a {@code GitTimeoutFailure} when it exceeds the budget
     */
    Result run(Path directory, Duration budget, Map<String, String> extraEnvironment, List<String> secrets,
            String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        Map<String, String> environment = builder.environment();
        Map<String, String> child = childEnvironment(environment, extraEnvironment);
        environment.clear();
        environment.putAll(child);
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
                throw new GitTimeoutFailure("git " + what + " did not answer within " + budget.toMillis() + " ms in "
                        + directory);
            }
            return new Result(process.exitValue(), mask(drain(out, what), secrets), mask(drain(err, what), secrets));
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

    /**
     * Returns the environment of a git process: the inherited one without this application's variables and
     * {@code SPRING_APPLICATION_JSON}, plus the credentials of this call and the two fixed git switches.
     */
    static Map<String, String> childEnvironment(Map<String, String> inherited, Map<String, String> extra) {
        Map<String, String> child = new HashMap<>();
        inherited.forEach((name, value) -> {
            if (!name.toUpperCase(Locale.ROOT).startsWith(OWN_PREFIX)
                    && !name.equals("SPRING_APPLICATION_JSON")) {
                child.put(name, value);
            }
        });
        child.putAll(extra);
        child.put("GIT_OPTIONAL_LOCKS", "0");
        child.put("GIT_TERMINAL_PROMPT", "0");
        return child;
    }

    /** Returns the whole text of a stream; a text that cannot be read in full is a failure, never empty. */
    private static String drain(CompletableFuture<String> text, String what) {
        try {
            return text.get(DRAIN.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new GitFailure("git " + what + " was interrupted", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new GitFailure("The output of git " + what + " could not be read in full", failure);
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
        // One thread for each stream, so that a busy shared pool cannot delay the reader.
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }, runnable -> Thread.ofVirtual().name("git-output").start(runnable));
    }
}
