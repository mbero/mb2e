package io.github.mbero.mb2e.adapter.in.shell;

import java.util.List;
import org.springframework.shell.command.CommandContext;

/**
 * Refusal raised by an mb2e command; the message names what was refused.
 * <p>It is reported on standard error and ends a one-shot run with exit status 1.
 */
public class ShellRefusal extends RuntimeException {

    public ShellRefusal(String message) {
        super(message);
    }

    /**
     * Prints a refused report: the JSON document with {@code --json}, otherwise the human
     * form, which ends with the trace id like every human report.
     * <p>The sentence on standard error is the exception message; this is the report.
     */
    static void printRefusal(boolean json, String document, String title, String detail, String traceId) {
        System.out.println(json ? document
                : "Refused: " + title + System.lineSeparator() + detail + System.lineSeparator()
                        + "Trace id: " + traceId);
        System.out.flush();
    }

    /**
     * Refuses every positional word; every command calls this first.
     * <p>Spring Shell already refuses an unknown option but silently ignores a stray word,
     * and no mb2e command takes a positional parameter.
     */
    public static void refuseWords(CommandContext context) {
        List<String> words = context.getParserResults().positional();
        if (!words.isEmpty()) {
            throw new ShellRefusal("The command '" + context.getCommandRegistration().getCommand()
                    + "' does not take " + (words.size() == 1 ? "this word" : "these words") + ": "
                    + String.join(" ", words));
        }
    }
}
