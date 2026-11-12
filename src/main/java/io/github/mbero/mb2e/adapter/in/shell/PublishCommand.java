package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.port.in.PublishUseCase;
import java.util.List;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Shell command {@code publish [--json] [--dry-run]} that builds the site and mirrors it into the publication target.
 * <p>Prints the git commands the operator runs next and runs none of them. Refusals are
 * reported as in {@link BuildCommand}.
 */
@Command
public class PublishCommand {

    private final PublishUseCase publish;
    private final StepLog log;

    public PublishCommand(PublishUseCase publish, StepLog log) {
        this.publish = publish;
        this.log = log;
    }

    @Command(command = "publish", description = "Build the site and mirror it into the publication target")
    public String publish(CommandContext context,
            @Option(longNames = "json", defaultValue = "false") boolean json,
            @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun) {
        ShellRefusal.refuseWords(context);
        PublishReport report = log.guarded("publish", () -> publish.publish(dryRun));
        log.publish(report);
        if (report.isRefused()) {
            ShellRefusal.printRefusal(json, ReportJson.of(report), report.title(), report.detail(),
                    report.traceId());
            throw new ShellRefusal(report.title() + ": " + report.detail());
        }
        return json ? ReportJson.of(report) : human(report);
    }

    static String human(PublishReport report) {
        return body(report) + System.lineSeparator() + "Trace id: " + report.traceId();
    }

    /** The report without its trace id, for a run report that shows the id once. */
    static String body(PublishReport report) {
        String nl = System.lineSeparator();
        String mark = report.dryRun() ? "  [dry run] " : "  ";
        StringBuilder out = new StringBuilder();
        report.warnings().forEach(warning -> out.append("Warning: ").append(warning).append(nl));
        report.changedFiles().forEach(path -> out.append(mark).append("~ ").append(path).append(nl));
        report.deleted().forEach(path -> out.append(mark).append("- ").append(path).append(nl));
        report.media().forEach(path -> out.append(mark).append("+ assets/img/notes/").append(path).append(nl));
        report.mediaDeleted().forEach(path -> out.append(mark).append("- assets/img/notes/").append(path).append(nl));
        lines(out, "image will not load: ", report.missingMedia());
        out.append("Target:              ").append(report.targetPath()).append(nl)
                .append("Branch:              ").append(report.branch()).append(nl)
                .append("Changed files:       ").append(report.changedFileCount()).append(nl)
                .append("Deleted:             ").append(report.deletedCount()).append(nl)
                .append("Images copied:       ").append(report.mediaCount()).append(nl)
                .append("Images removed:      ").append(report.mediaDeletedCount()).append(nl)
                .append("Images missing:      ").append(report.missingMediaCount()).append(nl);
        if (report.message() != null) {
            out.append(report.message()).append(nl);
        }
        if (!report.commands().isEmpty()) {
            out.append(nl).append("To publish, run these commands yourself (mb2e runs none of them):").append(nl);
            report.commands().forEach(command -> out.append("  ").append(command).append(nl));
        }
        return out.toString().stripTrailing();
    }

    private static void lines(StringBuilder out, String label, List<String> entries) {
        entries.forEach(entry -> out.append("  ").append(label).append(entry).append(System.lineSeparator()));
    }
}
