package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.model.SyncReport;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes one structured line per step: the step, its outcome, its counts and the trace id.
 * <p>The line goes to the {@code mb2e.step} logger, which logback sends to standard error. When
 * a log file is configured, the line and the full stack trace of any failure are appended to it
 * as well. A file that cannot be written is reported once per line as a warning and never fails a run.
 */
public class StepLog {

    private static final Logger LOG = LoggerFactory.getLogger("mb2e.step");

    private final Path file;

    /** Creates the log; a null or blank file name switches the file off. */
    public StepLog(String fileName) {
        this.file = fileName == null || fileName.isBlank() ? null : Path.of(fileName);
    }

    /** Runs a step and logs the failure, with its stack trace, when it throws. */
    public <T> T guarded(String step, Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException failure) {
            failure(step, failure);
            throw failure;
        }
    }

    public void sync(SyncReport report) {
        line("sync", report.outcome(), report.traceId(), report.title(), report.detail(),
                "dryRun=" + report.dryRun() + " scanned=" + report.scannedCount() + " selected="
                        + report.selectedCount() + " added=" + report.addedCount() + " updated="
                        + report.updatedCount() + " unchanged=" + report.unchangedCount() + " attachments="
                        + report.attachmentCount() + " missingFiles=" + report.missingAssetCount()
                        + " linksOutside=" + report.brokenLinkCount());
    }

    public void build(BuildReport report) {
        line("build", report.outcome(), report.traceId(), report.title(), report.detail(),
                "notes=" + report.noteCount() + " attachments=" + report.attachmentCount() + " emptyNotes="
                        + report.emptyNoteCount() + " unresolvedLinks=" + report.missingLinkCount()
                        + " missingFiles=" + report.missingAssetCount() + " bytes=" + report.outputBytes());
    }

    public void publish(PublishReport report) {
        line("publish", report.outcome(), report.traceId(), report.title(), report.detail(),
                "dryRun=" + report.dryRun() + " changed=" + report.changedFileCount() + " deleted="
                        + report.deletedCount() + " media=" + report.mediaCount() + " mediaDeleted="
                        + report.mediaDeletedCount() + " mediaMissing=" + report.missingMediaCount());
    }

    /** Logs the three steps of a run; a step that did not happen shows as skipped or not_run. */
    public void run(RunReport report) {
        step("sync", report.sync(), report.traceId());
        step("build", report.build(), report.traceId());
        step("publish", report.publish(), report.traceId());
    }

    private <T> void step(String name, StepResult<T> result, String traceId) {
        if (result.report() instanceof SyncReport report) {
            sync(report);
        } else if (result.report() instanceof BuildReport report) {
            build(report);
        } else if (result.report() instanceof PublishReport report) {
            publish(report);
        } else {
            line(name, result.outcome(), traceId, null, null, "");
        }
    }

    /** Logs a step that ended with an exception, under a fresh trace id, and appends its stack trace to the file. */
    public void failure(String step, Throwable failure) {
        String traceId = UUID.randomUUID().toString();
        String line = "step=" + step + " outcome=failed traceId=" + traceId + " error="
                + RefusalReporter.describe(failure);
        LOG.info(line);
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        append(line + System.lineSeparator() + trace);
    }

    private void line(String step, String outcome, String traceId, String title, String detail, String counts) {
        String line = "step=" + step + " outcome=" + outcome + " traceId=" + traceId
                + (counts.isEmpty() ? "" : " " + counts) + (title == null ? "" : " title=\"" + title + "\"");
        LOG.info(line);
        append(detail == null ? line : line + " detail=\"" + detail + "\"");
    }

    private void append(String text) {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, Instant.now() + " " + text + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            LOG.warn("The log file {} cannot be written: {}", file, failure.getMessage());
        }
    }
}
