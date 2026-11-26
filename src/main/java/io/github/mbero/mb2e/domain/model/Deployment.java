package io.github.mbero.mb2e.domain.model;

import java.time.Instant;

/**
 * One finished deployment as the history keeps it: the same facts as the {@code step=deploy} log line.
 * <p>{@code cause} is set only for a refused or failed run; commit ids are null when the run had none.
 */
public record Deployment(Instant finishedAt, String outcome, String trigger, int attempts, String sourceCommit,
        String targetCommit, String cause) {

    /** Turns a deploy report into a history entry; the trigger is the first word of the report reason. */
    public static Deployment of(DeployReport report, Instant finishedAt) {
        String reason = report.reason() == null ? "" : report.reason().strip();
        String trigger = reason.isEmpty() ? "unknown" : reason.split("\\s+", 2)[0];
        boolean explained = DeployReport.FAILED.equals(report.outcome())
                || DeployReport.REFUSED.equals(report.outcome());
        return new Deployment(finishedAt, report.outcome(), trigger, report.attempts(), report.sourceCommit(),
                report.targetCommit(), explained ? cause(report.title(), report.detail()) : null);
    }

    private static String cause(String title, String detail) {
        if (title == null || title.isBlank()) {
            return detail;
        }
        return detail == null || detail.isBlank() ? title : title + "\n" + detail;
    }
}
