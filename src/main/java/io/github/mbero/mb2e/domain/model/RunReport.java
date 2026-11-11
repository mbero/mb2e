package io.github.mbero.mb2e.domain.model;

/**
 * Pipeline report: one section per step, all three always present, and an outcome that is ok
 * only when no step was refused. Title and detail are the refused step's own. {@code openPath}
 * names the local page to open when publication is switched off, else it is null.
 */
public record RunReport(String traceId, String outcome, String title, String detail, boolean dryRun,
        String openPath, StepResult<SyncReport> sync, StepResult<BuildReport> build,
        StepResult<PublishReport> publish) {

    public boolean isRefused() {
        return "refused".equals(outcome);
    }
}
