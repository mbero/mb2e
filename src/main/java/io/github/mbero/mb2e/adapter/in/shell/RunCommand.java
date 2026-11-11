package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Shell command {@code run [--json] [--dry-run] [--skip-sync] [--skip-build] [--no-publish]}.
 * <p>Runs sync, build and publish once, synchronously, and prints one report with a section per step.
 */
@Command
public class RunCommand {

    private final PipelineUseCase pipeline;
    private final StepLog log;

    public RunCommand(PipelineUseCase pipeline, StepLog log) {
        this.pipeline = pipeline;
        this.log = log;
    }

    @Command(command = "run", description = "Sync, build and publish, in that order")
    public String run(CommandContext context,
            @Option(longNames = "json", defaultValue = "false") boolean json,
            @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun,
            @Option(longNames = "skip-sync", defaultValue = "false") boolean skipSync,
            @Option(longNames = "skip-build", defaultValue = "false") boolean skipBuild,
            @Option(longNames = "no-publish", defaultValue = "false") boolean noPublish) {
        ShellRefusal.refuseWords(context);
        RunReport report = log.guarded("run", () -> pipeline.run(dryRun, skipSync, skipBuild, noPublish));
        log.run(report);
        if (report.isRefused()) {
            ShellRefusal.printRefusal(json, ReportJson.of(report), report.title(), report.detail(),
                    report.traceId());
            throw new ShellRefusal(report.title() + ": " + report.detail());
        }
        return json ? ReportJson.of(report) : human(report);
    }

    static String human(RunReport report) {
        String nl = System.lineSeparator();
        StringBuilder out = new StringBuilder();
        section(out, "sync", report.sync(), report.sync().report() == null ? null
                : SyncCommand.body(report.sync().report()));
        section(out, "build", report.build(), report.build().report() == null ? null
                : BuildCommand.body(report.build().report()));
        section(out, "publish", report.publish(), report.publish().report() == null ? null
                : PublishCommand.body(report.publish().report()));
        if (report.openPath() != null) {
            out.append("Open the page: ").append(report.openPath()).append(nl);
        }
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
