package io.github.mbero.mb2e.domain.model;

/** One section of the pipeline report: the step's own report, or just skipped or not_run. */
public record StepResult<T>(String outcome, T report) {

    public static final String SKIPPED = "skipped";
    public static final String NOT_RUN = "not_run";

    public static <T> StepResult<T> skipped() {
        return new StepResult<>(SKIPPED, null);
    }

    public static <T> StepResult<T> notRun() {
        return new StepResult<>(NOT_RUN, null);
    }

    public static <T> StepResult<T> of(String outcome, T report) {
        return new StepResult<>(outcome, report);
    }

    public boolean isRefused() {
        return "refused".equals(outcome);
    }
}
