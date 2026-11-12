package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.port.in.BuildUseCase;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Shell command {@code build [--json] [--dry-run]} that selects the tagged notes and assembles the site in memory.
 * <p>A success is printed on standard output. A refusal is one sentence on standard error
 * with exit status 1 and, with {@code --json}, also the refused report on standard output.
 */
@Command
public class BuildCommand {

    private final BuildUseCase build;
    private final StepLog log;

    public BuildCommand(BuildUseCase build, StepLog log) {
        this.build = build;
        this.log = log;
    }

    @Command(command = "build", description = "Select the tagged notes and assemble the site in memory; writes nothing")
    public String build(CommandContext context,
            @Option(longNames = "json", defaultValue = "false") boolean json,
            @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun) {
        ShellRefusal.refuseWords(context);
        BuildReport report = log.guarded("build", () -> build.build(dryRun));
        log.build(report);
        if (report.isRefused()) {
            ShellRefusal.printRefusal(json, ReportJson.of(report), report.title(), report.detail(),
                    report.traceId());
            throw new ShellRefusal(report.title() + ": " + report.detail());
        }
        return json ? ReportJson.of(report) : human(report);
    }

    static String human(BuildReport report) {
        return body(report) + System.lineSeparator() + "Trace id: " + report.traceId();
    }

    /** The report without its trace id, for a run report that shows the id once. */
    static String body(BuildReport report) {
        String nl = System.lineSeparator();
        StringBuilder out = new StringBuilder();
        if (report.dryRun()) {
            out.append("[dry run] nothing is written anywhere").append(nl);
        }
        out.append("Vault:               ").append(report.vaultPath()).append(nl)
                .append("Template:            ").append(report.templatePath()).append(nl)
                .append("Notes scanned:       ").append(report.scannedCount()).append(nl)
                .append("Notes:               ").append(report.noteCount()).append(nl)
                .append("Attachments:         ").append(report.attachmentCount()).append(nl)
                .append("Empty notes:         ").append(report.emptyNoteCount()).append(nl)
                .append("Unresolved [[ ]]:    ").append(report.missingLinkCount()).append(nl)
                .append("Missing files:       ").append(report.missingAssetCount()).append(nl)
                .append("assets/js/data.js:   ").append(report.outputBytes()).append(" bytes").append(nl)
                .append("Start note:          ").append(report.defaultNote()).append(nl);
        report.missingLinks().forEach(entry -> out.append("  unresolved link: ").append(entry).append(nl));
        report.missingAssets().forEach(entry -> out.append("  missing file: ").append(entry).append(nl));
        report.warnings().forEach(warning -> out.append("Warning: ").append(warning).append(nl));
        return out.toString().stripTrailing();
    }
}
