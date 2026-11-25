package io.github.mbero.mb2e.adapter.in.shell;

import java.util.List;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserResults;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.context.DefaultShellContext;
import org.springframework.shell.context.InteractionMode;

/** Builds real Spring Shell command contexts for one command and its positional words. */
final class ShellContexts {

    private ShellContexts() {
    }

    static CommandContext of(String command, String... words) {
        return of(InteractionMode.INTERACTIVE, command, words);
    }

    static CommandContext of(InteractionMode mode, String command, String... words) {
        CommandRegistration registration = CommandRegistration.builder().command(command)
                .withTarget().function(context -> null).and().build();
        CommandParserResults results = CommandParserResults.of(registration, List.of(), List.of(words), List.of());
        DefaultShellContext shell = new DefaultShellContext();
        shell.setInteractionMode(mode);
        return CommandContext.of(words, results, null, registration, shell);
    }
}
