package io.github.mbero.mb2e.adapter.in.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserException;
import org.springframework.shell.command.CommandParser.CommandParserResults;
import org.springframework.shell.command.CommandExecution.CommandParserExceptionsException;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.context.ShellContext;

class ShellRefusalTest {

    private static CommandContext contextWith(String command, String... words) {
        return contextWith(InteractionMode.INTERACTIVE, command, words);
    }

    private static CommandContext contextWith(InteractionMode mode, String command, String... words) {
        ShellContext shell = mock(ShellContext.class);
        when(shell.getInteractionMode()).thenReturn(mode);
        CommandParserResults results = mock(CommandParserResults.class);
        when(results.positional()).thenReturn(List.of(words));
        CommandRegistration registration = mock(CommandRegistration.class);
        when(registration.getCommand()).thenReturn(command);
        CommandContext context = mock(CommandContext.class);
        when(context.getParserResults()).thenReturn(results);
        when(context.getCommandRegistration()).thenReturn(registration);
        when(context.getShellContext()).thenReturn(shell);
        return context;
    }

    @Test
    void aCommandWithoutWordsIsNotRefused() {
        assertThatCode(() -> ShellRefusal.refuseWords(contextWith("usage"))).doesNotThrowAnyException();
    }

    @Test
    void aStrayWordIsRefusedByName() {
        assertThatThrownBy(() -> ShellRefusal.refuseWords(contextWith("usage", "extra")))
                .isInstanceOf(ShellRefusal.class)
                .hasMessageContaining("usage")
                .hasMessageContaining("extra");
    }

    @Test
    void exitEndsTheSessionWithZero() {
        assertThatThrownBy(() -> new ExitCommand().exit(contextWith("exit")))
                .isInstanceOfSatisfying(ExitRequest.class, request -> assertThat(request.status()).isZero());
    }

    /** Outside an interactive session exit is refused by name, not a stack trace. */
    @Test
    void exitOutsideASessionIsRefusedByName() {
        for (InteractionMode mode : new InteractionMode[] {InteractionMode.ALL, InteractionMode.NONINTERACTIVE}) {
            assertThatThrownBy(() -> new ExitCommand().exit(contextWith(mode, "exit")))
                    .isInstanceOf(ShellRefusal.class).hasMessageContaining("exit");
        }
    }

    @Test
    void exitRefusesAWord() {
        assertThatThrownBy(() -> new ExitCommand().exit(contextWith("exit", "now")))
                .isInstanceOf(ShellRefusal.class);
    }

    @Test
    void theDescriptionIsPlainTextWithoutColourOrClassName() {
        String message = RefusalReporter.describe(CommandParserExceptionsException.of("Command parser resulted errors",
                List.of(CommandParserException.of("Unrecognised option '--json'"))));
        assertThat(message).isEqualTo("Unrecognised option '--json'").doesNotContain("\u001b");
        assertThat(RefusalReporter.describe(new IllegalStateException())).isEqualTo("IllegalStateException");
    }
}
