package io.github.mbero.mb2e.domain.model;

/**
 * Run report: a build section and a publish section, both always present, and an outcome that is ok
 * only when no step was refused. Title and detail are the refused step's own.
 */
public record RunReport(String traceId, String outcome, String title, String detail, boolean dryRun,
        StepResult<BuildReport> build, StepResult<PublishReport> publish) {

    public boolean isRefused() {
        return "refused".equals(outcome);
    }
}
