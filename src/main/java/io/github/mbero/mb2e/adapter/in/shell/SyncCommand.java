package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.SyncReport;
import io.github.mbero.mb2e.domain.port.in.SyncUseCase;
import java.util.List;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Shell command {@code sync [--json] [--dry-run]} that mirrors tagged vault notes into the datasource.
 * <p>The command only formats the report. Refusals are reported as in {@link BuildCommand}.
 */
@Command
public class SyncCommand {

    private final SyncUseCase sync;
    private final StepLog log;

    public SyncCommand(SyncUseCase sync, StepLog log) {
        this.sync = sync;
        this.log = log;
    }

    @Command(command = "sync", description = "Mirror the tagged notes of the vault into the datasource")
    public String sync(CommandContext context,
            @Option(longNames = "json", defaultValue = "false") boolean json,
            @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun) {
        ShellRefusal.refuseWords(context);
        SyncReport report = log.guarded("sync", () -> sync.sync(dryRun));
        log.sync(report);
        if (report.isRefused()) {
            ShellRefusal.printRefusal(json, ReportJson.of(report), report.title(), report.detail(),
                    report.traceId());
            throw new ShellRefusal(report.title() + ": " + report.detail());
        }
        return json ? ReportJson.of(report) : human(report);
    }

    static String human(SyncReport report) {
        return body(report) + System.lineSeparator() + "Trace id: " + report.traceId();
    }

    /** The report without its trace id, for a run report that shows the id once. */
    static String body(SyncReport report) {
        String nl = System.lineSeparator();
        String mark = report.dryRun() ? "  [dry run] " : "  ";
        StringBuilder out = new StringBuilder();
        report.added().forEach(path -> out.append(mark).append("+ ").append(path).append(nl));
        report.updated().forEach(path -> out.append(mark).append("~ ").append(path).append(nl));
        out.append("Vault:               ").append(report.vaultPath()).append(nl)
                .append("Notes scanned:       ").append(report.scannedCount()).append(nl)
                .append("Notes selected:      ").append(report.selectedCount()).append(nl)
                .append("Added:               ").append(report.addedCount()).append(nl)
                .append("Updated:             ").append(report.updatedCount()).append(nl)
                .append("Unchanged:           ").append(report.unchangedCount()).append(nl)
                .append("Deleted:             ").append(report.deletedCount()).append(nl)
                .append("Attachments:         ").append(report.attachmentCount()).append(nl)
                .append("Missing files:       ").append(report.missingAssetCount()).append(nl)
                .append("Links outside:       ").append(report.brokenLinkCount()).append(nl);
        lines(out, "missing file: ", report.missingAssets());
        lines(out, "link outside the selection: ", report.brokenLinks());
        return out.toString().stripTrailing();
    }

    private static void lines(StringBuilder out, String label, List<String> entries) {
        entries.forEach(entry -> out.append("  ").append(label).append(entry).append(System.lineSeparator()));
    }
}
