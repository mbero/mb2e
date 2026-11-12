package io.github.mbero.mb2e.adapter.in.shell;

import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;

/**
 * Prints the fixed usage listing; the command takes no parameter.
 * <p>The listing is deliberately static rather than derived from the registered commands:
 * it states the public command surface.
 */
@Command
public class UsageCommand {

    static final String USAGE = String.join(System.lineSeparator(),
            "mb2e - publish a vault of tagged notes as a static site",
            "",
            "Commands:",
            "  build    select the tagged notes and assemble the site in memory; writes nothing",
            "  publish  build the site and mirror it into the target repository",
            "  run      build and publish, in that order",
            "",
            "Parameters:",
            "  --json         print one JSON report document on standard output",
            "  --dry-run      plan the operation and write nothing",
            "  --no-publish   run only: build and leave out the publish step",
            "",
            "Other listings:",
            "  help     the shell's own listing",
            "  usage    this text",
            "",
            "Exit status: 0 when the operation succeeded, 1 when it was refused.",
            "Paths, tag and the other values are configuration, not parameters.");

    @Command(command = "usage", description = "Print the usage text")
    public String usage(CommandContext context) {
        ShellRefusal.refuseWords(context);
        return text();
    }

    String text() {
        return USAGE;
    }
}
