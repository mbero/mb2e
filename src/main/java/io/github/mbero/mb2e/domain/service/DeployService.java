package io.github.mbero.mb2e.domain.service;

import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitWorkspace;
import java.util.UUID;

/**
 * Brings the target repository up to date: sync both clones, run the pipeline, commit and push when
 * the site changed. A rejected push starts again from a fresh sync, up to the attempt limit.
 * <p>It never throws and never forces a push.
 */
public class DeployService implements DeployUseCase {

    private final GitWorkspace source;
    private final GitWorkspace target;
    private final PipelineUseCase pipeline;
    private final String commitSubject;
    private final int maxAttempts;

    public DeployService(GitWorkspace source, GitWorkspace target, PipelineUseCase pipeline, String commitSubject,
            int maxAttempts) {
        this.source = source;
        this.target = target;
        this.pipeline = pipeline;
        this.commitSubject = commitSubject;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public DeployReport deploy(String reason) {
        Progress progress = new Progress(reason);
        try {
            while (progress.attempts < maxAttempts) {
                progress.attempts++;
                progress.sourceCommit = source.sync();
                target.sync();
                progress.run = pipeline.run(false, false);
                if (progress.run.isRefused()) {
                    return progress.report(DeployReport.REFUSED, progress.run.title(), progress.run.detail());
                }
                if (!target.hasChanges()) {
                    return progress.report(DeployReport.UP_TO_DATE, null, null);
                }
                progress.targetCommit = target.commitAll(commitSubject, "Source commit: " + progress.sourceCommit);
                if (target.push() == GitWorkspace.PushOutcome.PUSHED) {
                    return progress.report(DeployReport.DEPLOYED, null, null);
                }
            }
            return progress.report(DeployReport.FAILED, "The target branch moved",
                    "The push was rejected " + progress.attempts + " times; the next trigger tries again.");
        } catch (GitFailure failure) {
            return progress.report(DeployReport.FAILED, "Git failed", failure.getMessage());
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return progress.report(DeployReport.FAILED, "Deployment failed",
                    message == null || message.isBlank() ? failure.getClass().getSimpleName() : message);
        }
    }

    /** What one deployment has reached so far; any exit turns it into a report. */
    private static final class Progress {

        private final String reason;
        private int attempts;
        private String sourceCommit;
        private String targetCommit;
        private RunReport run;

        private Progress(String reason) {
            this.reason = reason;
        }

        private DeployReport report(String outcome, String title, String detail) {
            String traceId = run != null ? run.traceId() : UUID.randomUUID().toString();
            String commit = DeployReport.DEPLOYED.equals(outcome) || DeployReport.FAILED.equals(outcome)
                    ? targetCommit : null;
            return new DeployReport(traceId, outcome, title, detail, reason, sourceCommit, commit, attempts, run);
        }
    }
}
