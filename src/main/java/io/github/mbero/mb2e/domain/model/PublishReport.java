package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Publication report in which every field is present, a refusal included.
 * The counts follow the lists.
 */
public record PublishReport(String traceId, String outcome, String title, String detail, boolean dryRun,
        String targetPath, String branch, String message, List<String> warnings, List<String> changedFiles,
        List<String> commands, List<String> deleted, List<String> media, List<String> mediaDeleted,
        List<String> missingMedia) {

    public static final String OK = "ok";
    public static final String REFUSED = "refused";

    public PublishReport {
        warnings = List.copyOf(warnings);
        changedFiles = List.copyOf(changedFiles);
        commands = List.copyOf(commands);
        deleted = List.copyOf(deleted);
        media = List.copyOf(media);
        mediaDeleted = List.copyOf(mediaDeleted);
        missingMedia = List.copyOf(missingMedia);
    }

    public static PublishReport refused(String traceId, String title, String detail, boolean dryRun,
            String targetPath) {
        return new PublishReport(traceId, REFUSED, title, detail, dryRun, targetPath, null, null, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public boolean isRefused() {
        return REFUSED.equals(outcome);
    }

    public PublishReport withTraceId(String id) {
        return new PublishReport(id, outcome, title, detail, dryRun, targetPath, branch, message, warnings,
                changedFiles, commands, deleted, media, mediaDeleted, missingMedia);
    }

    public int changedFileCount() {
        return changedFiles.size();
    }

    public int deletedCount() {
        return deleted.size();
    }

    public int mediaCount() {
        return media.size();
    }

    public int mediaDeletedCount() {
        return mediaDeleted.size();
    }

    public int missingMediaCount() {
        return missingMedia.size();
    }
}
