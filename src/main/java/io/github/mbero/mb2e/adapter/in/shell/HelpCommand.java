package io.github.mbero.mb2e.adapter.in.shell;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;

/**
 * Prints the command listing, generated from the command catalogue.
 * <p>Replaces Spring Shell's help command, which accepts a {@code --command} parameter
 * and a command name; this one takes no parameter at all.
 */
@Command
public class HelpCommand {

    /** Resolved at call time because the catalogue is built from this very bean. */
    private final ObjectProvider<CommandCatalog> catalog;

    public HelpCommand(ObjectProvider<CommandCatalog> catalog) {
        this.catalog = catalog;
    }

    @Command(command = "help", description = "List the commands")
    public String help(CommandContext context) {
        ShellRefusal.refuseWords(context);
        Map<String, CommandRegistration> sorted = new TreeMap<>(catalog.getObject().getRegistrations());
        int width = sorted.keySet().stream().mapToInt(String::length).max().orElse(0);
        return "Commands:" + System.lineSeparator() + sorted.entrySet().stream()
                .map(e -> "  " + String.format("%-" + width + "s", e.getKey()) + "  "
                        + e.getValue().getDescription())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
