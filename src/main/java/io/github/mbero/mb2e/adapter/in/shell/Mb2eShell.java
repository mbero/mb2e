package io.github.mbero.mb2e.adapter.in.shell;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Input;
import org.springframework.shell.ResultHandlerService;
import org.springframework.shell.Shell;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.shell.config.ShellConversionServiceSupplier;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.exit.ExitCodeMappings;
import org.jline.terminal.Terminal;

/**
 * Shell that validates every word of every line before Spring Shell parses it.
 * <p>Applies to the command line, script lines and typed lines alike, because Spring Shell
 * drops an empty word and reads a bare {@code --} as the end of options before a command sees them.
 */
public class Mb2eShell extends Shell {

    public Mb2eShell(ResultHandlerService resultHandlerService, CommandCatalog commandRegistry, Terminal terminal,
            ShellContext shellContext, ExitCodeMappings exitCodeMappings) {
        super(resultHandlerService, commandRegistry, terminal, shellContext, exitCodeMappings);
    }

    @Autowired
    void useConversionService(ShellConversionServiceSupplier supplier) {
        setConversionService(supplier.get());
    }

    @Override
    protected Object evaluate(Input input) {
        // A blank line is no input at all; only a blank word inside a command is refused.
        if (input.rawText() != null && !input.rawText().isBlank()) {
            for (String word : input.words()) {
                Optional<String> refused = ArgumentGuard.wordRefusal(word);
                if (refused.isPresent()) {
                    return new ShellRefusal(refused.get());
                }
            }
        }
        return super.evaluate(input);
    }
}
