package io.github.mbero.mb2e.adapter.in.shell;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.shell.CommandNotFound;
import org.springframework.shell.ResultHandler;
import org.springframework.core.Ordered;
import org.springframework.shell.command.CommandParserExceptionResolver;
import org.springframework.shell.command.CommandExecution.CommandParserExceptionsException;
import org.springframework.shell.command.CommandHandlingResult;

/**
 * Reports every refusal on standard error as plain text, so standard output carries only the report.
 * <p>Spring Shell's own handlers print a refusal to standard output in ANSI colour and an
 * unexpected failure with its stack trace.
 */
public final class RefusalReporter {

    private RefusalReporter() {
    }

    /** Returns the text a person reads: no class name, no stack trace, no colour. */
    public static String describe(Throwable failure) {
        if (failure instanceof CommandParserExceptionsException parser && !parser.getParserExceptions().isEmpty()) {
            List<String> messages = parser.getParserExceptions().stream()
                    .map(Throwable::getMessage).collect(Collectors.toList());
            return String.join(System.lineSeparator(), messages);
        }
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static volatile boolean reported;

    /** Returns whether a refusal was written to standard error since the last {@link #forget()}. */
    public static boolean reported() {
        return reported;
    }

    public static void forget() {
        reported = false;
    }

    public static void report(Throwable failure) {
        reported = true;
        System.err.println(describe(failure));
        System.err.flush();
    }

    /** Handles any other failure of a command. */
    public static class Failures implements ResultHandler<Throwable> {
        @Override
        public void handleResult(Throwable failure) {
            report(failure);
        }
    }

    /** Handles a word that is not a command, in place of Spring Shell's own handler for this type. */
    public static class UnknownCommands implements ResultHandler<CommandNotFound> {
        @Override
        public void handleResult(CommandNotFound failure) {
            report(failure);
        }
    }

    /**
     * Handles a rejected parameter before Spring Shell's resolver does.
     * <p>That resolver writes the message in colour to standard output; this one writes
     * nothing to standard output and reports the message on standard error.
     */
    public static class ParameterRefusals extends CommandParserExceptionResolver implements Ordered {
        @Override
        public CommandHandlingResult resolve(Exception failure) {
            if (failure instanceof CommandParserExceptionsException) {
                report(failure);
                return CommandHandlingResult.of("", 1);
            }
            return super.resolve(failure);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
