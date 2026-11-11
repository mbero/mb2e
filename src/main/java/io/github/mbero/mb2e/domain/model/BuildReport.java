package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Build report in which every field is present.
 * <p>A refusal is the same record with outcome {@code refused} and a title and a detail.
 */
public record BuildReport(String traceId, String outcome, String title, String detail, int noteCount,
        int attachmentCount, int emptyNoteCount, int missingLinkCount, int missingAssetCount,
        long outputBytes, String defaultNote, List<String> warnings, List<String> missingLinks,
        List<String> missingAssets) {

    public static final String OK = "ok";
    public static final String REFUSED = "refused";

    public BuildReport {
        warnings = List.copyOf(warnings);
        missingLinks = List.copyOf(missingLinks);
        missingAssets = List.copyOf(missingAssets);
    }

    public static BuildReport refused(String traceId, String title, String detail) {
        return new BuildReport(traceId, REFUSED, title, detail, 0, 0, 0, 0, 0, 0, null, List.of(),
                List.of(), List.of());
    }

    public BuildReport withTraceId(String id) {
        return new BuildReport(id, outcome, title, detail, noteCount, attachmentCount, emptyNoteCount,
                missingLinkCount, missingAssetCount, outputBytes, defaultNote, warnings, missingLinks,
                missingAssets);
    }

    public boolean isRefused() {
        return REFUSED.equals(outcome);
    }
}
