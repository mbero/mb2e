package io.github.mbero.mb2e.adapter.in.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.CommandParser.CommandParserResults;
import org.springframework.shell.command.CommandRegistration;

/** Covers the three working commands and the report JSON with mocked use cases. */
class ReportCommandsTest {

    private final StepLog log = new StepLog("");

    private static CommandContext context(String command, String... words) {
        CommandParserResults results = mock(CommandParserResults.class);
        when(results.positional()).thenReturn(List.of(words));
        CommandRegistration registration = mock(CommandRegistration.class);
        when(registration.getCommand()).thenReturn(command);
        CommandContext context = mock(CommandContext.class);
        when(context.getParserResults()).thenReturn(results);
        when(context.getCommandRegistration()).thenReturn(registration);
        return context;
    }

    private static BuildReport built(boolean dryRun) {
        return new BuildReport("t-b", "ok", null, null, dryRun, "/v", "/tpl", 5, 4, List.of("a.png"), 1,
                List.of("[[ghost]]"), List.of("missing.png"), 100, "about", List.of("careful"));
    }

    private static PublishReport published(boolean dryRun, String message) {
        return new PublishReport("t-p", "ok", null, null, dryRun, "/t", "main", message, List.of("warn"),
                List.of("index.html"), List.of("git add -A"), List.of("old.html"), List.of("n/a.png"),
                List.of("gone.png"), List.of("lost.png"));
    }

    private static BuildReport refusedBuild() {
        return BuildReport.refused("t-r", "No vault", "Set mb2e.vault.path", false,
                new ConfiguredPaths(null, "/tpl", null));
    }

    /** Runs the call with standard output captured and returns what it printed. */
    private static String printed(Runnable call) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            call.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString();
    }

    @Test
    void buildPrintsTheHumanReportWithItsDryRunMarkAndTraceId() {
        BuildUseCase use = mock(BuildUseCase.class);
        when(use.build(true)).thenReturn(built(true));
        String text = new BuildCommand(use, log).build(context("build"), false, true);
        assertThat(text).contains("[dry run] nothing is written anywhere", "Notes:               4",
                "unresolved link: [[ghost]]", "missing file: missing.png", "Warning: careful",
                "Start note:          about").endsWith("Trace id: t-b");
    }

    @Test
    void buildPrintsOneJsonObjectWithJson() {
        BuildUseCase use = mock(BuildUseCase.class);
        when(use.build(false)).thenReturn(built(false));
        String json = new BuildCommand(use, log).build(context("build"), true, false);
        assertThat(json).startsWith("{\"outcome\":\"ok\",\"traceId\":\"t-b\"").contains("\"noteCount\":4",
                "\"missingLinks\":[\"[[ghost]]\"]", "\"dryRun\":false").endsWith("}");
    }

    @Test
    void aRefusedBuildPrintsItsReportAndThrowsARefusal() {
        BuildUseCase use = mock(BuildUseCase.class);
        when(use.build(anyBoolean())).thenReturn(refusedBuild());
        BuildCommand command = new BuildCommand(use, log);
        String out = printed(() -> assertThatThrownBy(() -> command.build(context("build"), false, false))
                .isInstanceOf(ShellRefusal.class).hasMessage("No vault: Set mb2e.vault.path"));
        assertThat(out).contains("Refused: No vault", "Set mb2e.vault.path", "Trace id: t-r");
        String json = printed(() -> assertThatThrownBy(() -> command.build(context("build"), true, false))
                .isInstanceOf(ShellRefusal.class));
        assertThat(json).startsWith("{\"outcome\":\"refused\"");
    }

    @Test
    void publishPrintsTheChangesTheCountsAndTheCommands() {
        PublishUseCase use = mock(PublishUseCase.class);
        when(use.publish(true)).thenReturn(published(true, "Note"));
        String text = new PublishCommand(use, log).publish(context("publish"), false, true);
        assertThat(text).contains("Warning: warn", "[dry run] ~ index.html", "[dry run] - old.html",
                "[dry run] + assets/img/notes/n/a.png", "[dry run] - assets/img/notes/gone.png",
                "image will not load: lost.png", "Branch:              main", "Note",
                "To publish, run these commands yourself", "  git add -A").endsWith("Trace id: t-p");
    }

    @Test
    void publishWithoutDryRunOrMessageOrCommandsOmitsThem() {
        PublishUseCase use = mock(PublishUseCase.class);
        PublishReport quiet = new PublishReport("t-q", "ok", null, null, false, "/t", "main", null, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        when(use.publish(false)).thenReturn(quiet);
        String text = new PublishCommand(use, log).publish(context("publish"), false, false);
        assertThat(text).doesNotContain("[dry run]", "To publish").contains("Changed files:       0");
    }

    @Test
    void publishPrintsJsonAndRefusesLikeBuild() {
        PublishUseCase use = mock(PublishUseCase.class);
        when(use.publish(false)).thenReturn(published(false, null));
        assertThat(new PublishCommand(use, log).publish(context("publish"), true, false))
                .contains("\"targetPath\":\"/t\"", "\"message\":null", "\"missingMediaCount\":1");
        when(use.publish(true)).thenReturn(PublishReport.refused("t-x", "Not usable", "Bad target", true, "/t"));
        printed(() -> assertThatThrownBy(() -> new PublishCommand(use, log).publish(context("publish"), false, true))
                .isInstanceOf(ShellRefusal.class).hasMessage("Not usable: Bad target"));
    }

    @Test
    void runPrintsASectionPerStepAndASkippedStepHasNoBody() {
        PipelineUseCase use = mock(PipelineUseCase.class);
        when(use.run(false, false)).thenReturn(new RunReport("t-run", "ok", null, null, false,
                StepResult.of("ok", built(false)), StepResult.of("ok", published(false, null))));
        when(use.run(false, true)).thenReturn(new RunReport("t-run2", "ok", null, null, false,
                StepResult.of("ok", built(false)), StepResult.skipped()));
        RunCommand command = new RunCommand(use, log);
        assertThat(command.run(context("run"), false, false, false)).contains("== build: ok ==",
                "== publish: ok ==", "Branch:              main").endsWith("Trace id: t-run");
        assertThat(command.run(context("run"), false, false, true)).contains("== publish: skipped ==")
                .doesNotContain("Branch:");
    }

    @Test
    void runPrintsJsonWithASectionPerStepAndRefusesWhenAStepIsRefused() {
        PipelineUseCase use = mock(PipelineUseCase.class);
        when(use.run(false, true)).thenReturn(new RunReport("t-j", "ok", null, null, false,
                StepResult.of("ok", built(false)), StepResult.skipped()));
        RunCommand command = new RunCommand(use, log);
        assertThat(command.run(context("run"), true, false, true)).contains("\"build\":{\"outcome\":\"ok\"",
                "\"publish\":{\"outcome\":\"skipped\"}").doesNotContain("\"traceId\":\"t-b\"");
        when(use.run(true, false)).thenReturn(new RunReport("t-f", "refused", "No vault", "Set it", true,
                StepResult.of("refused", refusedBuild()), StepResult.notRun()));
        String out = printed(() -> assertThatThrownBy(() -> command.run(context("run"), false, true, false))
                .isInstanceOf(ShellRefusal.class).hasMessage("No vault: Set it"));
        assertThat(out).contains("Refused: No vault", "Trace id: t-f");
    }

    @Test
    void aStrayWordIsRefusedBeforeAnyUseCaseRuns() {
        BuildUseCase use = mock(BuildUseCase.class);
        assertThatThrownBy(() -> new BuildCommand(use, log).build(context("build", "extra"), false, false))
                .isInstanceOf(ShellRefusal.class).hasMessageContaining("extra");
    }
}
