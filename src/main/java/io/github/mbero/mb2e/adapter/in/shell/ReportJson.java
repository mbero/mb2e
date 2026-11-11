package io.github.mbero.mb2e.adapter.in.shell;

import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.JsonText;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.model.SyncReport;
import java.util.List;

/** Serializes reports as single JSON objects in which every field is always present. */
final class ReportJson {

    private ReportJson() {
    }

    static String of(BuildReport report) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + ",\"traceId\":" + JsonText.quote(report.traceId())
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + ",\"noteCount\":" + report.noteCount()
                + ",\"attachmentCount\":" + report.attachmentCount()
                + ",\"emptyNoteCount\":" + report.emptyNoteCount()
                + ",\"missingLinkCount\":" + report.missingLinkCount()
                + ",\"missingAssetCount\":" + report.missingAssetCount()
                + ",\"outputBytes\":" + report.outputBytes()
                + ",\"defaultNote\":" + JsonText.quote(report.defaultNote())
                + ",\"warnings\":" + array(report.warnings())
                + ",\"missingLinks\":" + array(report.missingLinks())
                + ",\"missingAssets\":" + array(report.missingAssets()) + "}";
    }

    /** Serializes the sync report: all fifteen fields plus the common ones. */
    static String of(SyncReport report) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + ",\"traceId\":" + JsonText.quote(report.traceId())
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + ",\"dryRun\":" + report.dryRun()
                + ",\"vaultPath\":" + JsonText.quote(report.vaultPath())
                + ",\"scannedCount\":" + report.scannedCount()
                + ",\"selectedCount\":" + report.selectedCount()
                + ",\"addedCount\":" + report.addedCount()
                + ",\"updatedCount\":" + report.updatedCount()
                + ",\"unchangedCount\":" + report.unchangedCount()
                + ",\"deletedCount\":" + report.deletedCount()
                + ",\"attachmentCount\":" + report.attachmentCount()
                + ",\"missingAssetCount\":" + report.missingAssetCount()
                + ",\"brokenLinkCount\":" + report.brokenLinkCount()
                + ",\"added\":" + array(report.added())
                + ",\"updated\":" + array(report.updated())
                + ",\"deleted\":" + array(report.deleted())
                + ",\"attachments\":" + array(report.attachments())
                + ",\"missingAssets\":" + array(report.missingAssets())
                + ",\"brokenLinks\":" + array(report.brokenLinks()) + "}";
    }

    /** Serializes the publication report with all twenty-one fields. */
    static String of(PublishReport report) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + ",\"traceId\":" + JsonText.quote(report.traceId())
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + ",\"dryRun\":" + report.dryRun()
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

    /** Serializes the pipeline report: a section per step, holding that step's report or just its outcome. */
    static String of(RunReport report) {
        return "{\"outcome\":" + JsonText.quote(report.outcome())
                + ",\"traceId\":" + JsonText.quote(report.traceId())
                + ",\"title\":" + JsonText.quote(report.title())
                + ",\"detail\":" + JsonText.quote(report.detail())
                + ",\"dryRun\":" + report.dryRun()
                + ",\"openPath\":" + JsonText.quote(report.openPath())
                + ",\"sync\":" + section(report.sync(), ReportJson::of)
                + ",\"build\":" + section(report.build(), ReportJson::of)
                + ",\"publish\":" + section(report.publish(), ReportJson::of) + "}";
    }

    private static <T> String section(StepResult<T> step, java.util.function.Function<T, String> document) {
        return step.report() == null ? "{\"outcome\":" + JsonText.quote(step.outcome()) + "}"
                : document.apply(step.report());
    }

    private static String array(List<String> values) {
        return values.stream().map(JsonText::quote).collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
