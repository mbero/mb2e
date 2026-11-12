package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Build report in which every field is present.
 * <p>A refusal is the same record with outcome {@code refused}, a title and a detail.
 */
public record BuildReport(String traceId, String outcome, String title, String detail, boolean dryRun,
        String vaultPath, String templatePath, int scannedCount, int noteCount, List<String> attachments,
        int emptyNoteCount, List<String> missingLinks, List<String> missingAssets, long outputBytes,
        String defaultNote, List<String> warnings) {

    public static final String OK = "ok";
    public static final String REFUSED = "refused";

    public BuildReport {
        attachments = List.copyOf(attachments);
        missingLinks = List.copyOf(missingLinks);
        missingAssets = List.copyOf(missingAssets);
        warnings = List.copyOf(warnings);
    }

    public static BuildReport refused(String traceId, String title, String detail, boolean dryRun,
            ConfiguredPaths paths) {
        return new BuildReport(traceId, REFUSED, title, detail, dryRun, paths.vault(), paths.template(), 0, 0,
                List.of(), 0, List.of(), List.of(), 0, null, List.of());
    }

    public BuildReport withTraceId(String id) {
        return new BuildReport(id, outcome, title, detail, dryRun, vaultPath, templatePath, scannedCount,
                noteCount, attachments, emptyNoteCount, missingLinks, missingAssets, outputBytes, defaultNote,
                warnings);
    }

    public boolean isRefused() {
        return REFUSED.equals(outcome);
    }

    public int attachmentCount() {
        return attachments.size();
    }

    public int missingLinkCount() {
        return missingLinks.size();
    }

    public int missingAssetCount() {
        return missingAssets.size();
    }
}
