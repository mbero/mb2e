package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Shell command {@code run [--json] [--dry-run] [--no-publish]}.
 * <p>Runs build and publish once, synchronously, and prints one report with a section per step.
 */
@Command
public class RunCommand {

    private final PipelineUseCase pipeline;
    private final StepLog log;

    public RunCommand(PipelineUseCase pipeline, StepLog log) {
        this.pipeline = pipeline;
        this.log = log;
    }

    @Command(command = "run", description = "Build and publish, in that order")
    public String run(CommandContext context,
            @Option(longNames = "json", defaultValue = "false") boolean json,
            @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun,
            @Option(longNames = "no-publish", defaultValue = "false") boolean noPublish) {
        ShellRefusal.refuseWords(context);
        RunReport report = log.guarded("run", () -> pipeline.run(dryRun, noPublish));
        log.run(report);
        if (report.isRefused()) {
            ShellRefusal.printRefusal(json, ReportJson.of(report), report.title(), report.detail(),
                    report.traceId());
            throw new ShellRefusal(report.title() + ": " + report.detail());
        }
        return json ? ReportJson.of(report) : human(report);
    }

    static String human(RunReport report) {
        StringBuilder out = new StringBuilder();
        section(out, "build", report.build(), report.build().report() == null ? null
                : BuildCommand.body(report.build().report()));
        section(out, "publish", report.publish(), report.publish().report() == null ? null
                : PublishCommand.body(report.publish().report()));
        return out.append("Trace id: ").append(report.traceId()).toString();
    }

    private static void section(StringBuilder out, String name, StepResult<?> step, String body) {
        String nl = System.lineSeparator();
        out.append("== ").append(name).append(": ").append(step.outcome()).append(" ==").append(nl);
        if (body != null) {
            out.append(body).append(nl);
        }
    }
}
