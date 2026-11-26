package io.github.mbero.mb2e.domain.model;

/**
 * The result of one deployment: what happened, which commits it used and how many attempts it took.
 * <p>{@code run} is null when the failure came before the pipeline ran.
 */
public record DeployReport(String traceId, String outcome, String title, String detail, String reason,
        String sourceCommit, String targetCommit, int attempts, RunReport run) {

    public static final String DEPLOYED = "deployed";
    public static final String UP_TO_DATE = "up_to_date";
    public static final String REFUSED = "refused";
    public static final String FAILED = "failed";

    public boolean isFailed() {
        return FAILED.equals(outcome);
    }
}
