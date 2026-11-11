package io.github.mbero.mb2e.adapter.in.shell;

import java.util.Arrays;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.ShellRunner;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.jline.NonInteractiveShellRunner;

/**
 * Runs the process arguments as one command line, passing each argument as exactly one word.
 * <p>Spring Shell's {@code NonInteractiveShellRunner} joins the arguments and tokenizes them
 * again, so a quote or backslash inside an argument would be read twice and a word could be lost.
 */
@Order(NonInteractiveShellRunner.PRECEDENCE)
public class OneShotRunner implements ShellRunner {

    private final Shell shell;
    private final ShellContext shellContext;

    public OneShotRunner(Shell shell, ShellContext shellContext) {
        this.shell = shell;
        this.shellContext = shellContext;
    }

    @Override
    public boolean run(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            return false;
        }
        List<String> words = List.copyOf(Arrays.asList(args));
        Input input = new Input() {
            @Override
            public String rawText() {
                return String.join(" ", words);
            }

            @Override
            public List<String> words() {
                return words;
            }
        };
        shellContext.setInteractionMode(InteractionMode.NONINTERACTIVE);
        shell.run(new InputProvider() {
            private boolean given;

            @Override
            public Input readInput() {
                if (given) {
                    return null;
                }
                given = true;
                return input;
            }
        });
        return true;
    }
}
