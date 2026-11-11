package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Sync report in which all fifteen fields are present, a refusal included.
 * <p>A sync never deletes, so {@code deletedCount} is always 0 and {@code deleted} always
 * empty; neither is a constructor parameter.
 */
public record SyncReport(String traceId, String outcome, String title, String detail, boolean dryRun,
        String vaultPath, int scannedCount, int selectedCount, List<String> added, List<String> updated,
        int unchangedCount, List<String> attachments, List<String> missingAssets, List<String> brokenLinks) {

    public static final String OK = "ok";
    public static final String REFUSED = "refused";

    public SyncReport {
        added = List.copyOf(added);
        updated = List.copyOf(updated);
        attachments = List.copyOf(attachments);
        missingAssets = List.copyOf(missingAssets);
        brokenLinks = List.copyOf(brokenLinks);
    }

    public static SyncReport refused(String traceId, String title, String detail, boolean dryRun,
            String vaultPath, int scannedCount) {
        return new SyncReport(traceId, REFUSED, title, detail, dryRun, vaultPath, scannedCount, 0, List.of(),
                List.of(), 0, List.of(), List.of(), List.of());
    }

    public SyncReport withTraceId(String id) {
        return new SyncReport(id, outcome, title, detail, dryRun, vaultPath, scannedCount, selectedCount, added,
                updated, unchangedCount, attachments, missingAssets, brokenLinks);
    }

    public boolean isRefused() {
        return REFUSED.equals(outcome);
    }

    public int addedCount() {
        return added.size();
    }

    public int updatedCount() {
        return updated.size();
    }

    /** Always 0: a sync never deletes. */
    public int deletedCount() {
        return 0;
    }

    public List<String> deleted() {
        return List.of();
    }

    public int attachmentCount() {
        return attachments.size();
    }

    public int missingAssetCount() {
        return missingAssets.size();
    }

    public int brokenLinkCount() {
        return brokenLinks.size();
    }
}
