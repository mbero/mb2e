package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.JsonText;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import java.util.List;

/** Serializes reports as single JSON objects in which every field is always present. */
final class ReportJson {

    private ReportJson() {
    }

    static String of(BuildReport report) {
        return build(report, true);
    }

    /** The build report; inside a run report it leaves out the trace id and dry-run flag the run states once. */
    private static String build(BuildReport report, boolean standalone) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + (standalone ? ",\"traceId\":" + JsonText.quote(report.traceId()) : "")
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + (standalone ? ",\"dryRun\":" + report.dryRun() : "")
                + ",\"vaultPath\":" + JsonText.quote(report.vaultPath())
                + ",\"templatePath\":" + JsonText.quote(report.templatePath())
                + ",\"scannedCount\":" + report.scannedCount()
                + ",\"noteCount\":" + report.noteCount()
                + ",\"attachmentCount\":" + report.attachmentCount()
                + ",\"attachments\":" + array(report.attachments())
                + ",\"emptyNoteCount\":" + report.emptyNoteCount()
                + ",\"missingLinkCount\":" + report.missingLinkCount()
                + ",\"missingLinks\":" + array(report.missingLinks())
                + ",\"missingAssetCount\":" + report.missingAssetCount()
                + ",\"missingAssets\":" + array(report.missingAssets())
                + ",\"outputBytes\":" + report.outputBytes()
                + ",\"defaultNote\":" + JsonText.quote(report.defaultNote())
                + ",\"warnings\":" + array(report.warnings()) + "}";
    }

    /** Serializes the publication report with every field present. */
    static String of(PublishReport report) {
        return publish(report, true);
    }

    /** The publication report; inside a run report it leaves out the trace id and dry-run flag the run states once. */
    private static String publish(PublishReport report, boolean standalone) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + (standalone ? ",\"traceId\":" + JsonText.quote(report.traceId()) : "")
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + (standalone ? ",\"dryRun\":" + report.dryRun() : "")
                + ",\"targetPath\":" + JsonText.quote(report.targetPath())
                + ",\"branch\":" + JsonText.quote(report.branch())
                + ",\"message\":" + JsonText.quote(report.message())
                + ",\"warnings\":" + array(report.warnings())
                + ",\"changedFileCount\":" + report.changedFileCount()
                + ",\"changedFiles\":" + array(report.changedFiles())
                + ",\"commands\":" + array(report.commands())
                + ",\"deletedCount\":" + report.deletedCount()
                + ",\"deleted\":" + array(report.deleted())
                + ",\"mediaCount\":" + report.mediaCount()
                + ",\"media\":" + array(report.media())
                + ",\"mediaDeletedCount\":" + report.mediaDeletedCount()
                + ",\"mediaDeleted\":" + array(report.mediaDeleted())
                + ",\"missingMediaCount\":" + report.missingMediaCount()
                + ",\"missingMedia\":" + array(report.missingMedia()) + "}";
    }

    /** Serializes the run report: a section per step, holding that step's report or just its outcome. */
    static String of(RunReport report) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + ",\"traceId\":" + JsonText.quote(report.traceId())
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + ",\"dryRun\":" + report.dryRun()
                + ",\"build\":" + section(report.build(), r -> build(r, false))
                + ",\"publish\":" + section(report.publish(), r -> publish(r, false)) + "}";
    }

    private static <T> String section(StepResult<T> step, java.util.function.Function<T, String> document) {
        return step.report() == null ? "{\"outcome\":" + JsonText.quote(step.outcome()) + "}"
                : document.apply(step.report());
    }

    private static String array(List<String> values) {
        return values.stream().map(JsonText::quote).collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
