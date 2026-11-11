package io.github.mbero.mb2e.adapter.in.shell;

import org.springframework.shell.ExitRequest;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.command.annotation.Command;

/**
 * Ends an interactive session with exit status 0.
 * <p>Replaces Spring Shell's quit command, whose {@code quit} alias mb2e does not offer.
 */
@Command
public class ExitCommand {

    @Command(command = "exit", description = "End the interactive session")
    public void exit(CommandContext context) {
        ShellRefusal.refuseWords(context);
        // Outside an interactive session (command line, script line) exit is an unknown command.
        if (context.getShellContext().getInteractionMode() != InteractionMode.INTERACTIVE) {
            throw new ShellRefusal("The command 'exit' ends an interactive session and is not a command here: exit");
        }
        throw new ExitRequest();
    }
}
