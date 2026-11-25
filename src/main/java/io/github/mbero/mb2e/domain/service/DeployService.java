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
        int attempts = 0;
        String sourceCommit = null;
        String targetCommit = null;
        RunReport run = null;
        try {
            while (attempts < maxAttempts) {
                attempts++;
                sourceCommit = source.sync();
                target.sync();
                run = pipeline.run(false, false);
                if (run.isRefused()) {
                    return report(DeployReport.REFUSED, run.title(), run.detail(), reason, sourceCommit, null,
                            attempts, run);
                }
                if (!target.hasChanges()) {
                    return report(DeployReport.UP_TO_DATE, null, null, reason, sourceCommit, null, attempts, run);
                }
                targetCommit = target.commitAll(commitSubject, "Source commit: " + sourceCommit);
                if (target.push() == GitWorkspace.PushOutcome.PUSHED) {
                    return report(DeployReport.DEPLOYED, null, null, reason, sourceCommit, targetCommit, attempts,
                            run);
                }
            }
            return report(DeployReport.FAILED, "The target branch moved",
                    "The push was rejected " + attempts + " times; the next trigger tries again.", reason,
                    sourceCommit, targetCommit, attempts, run);
        } catch (GitFailure failure) {
            return report(DeployReport.FAILED, "Git failed", failure.getMessage(), reason, sourceCommit,
                    targetCommit, attempts, run);
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return report(DeployReport.FAILED, "Deployment failed",
                    message == null || message.isBlank() ? failure.getClass().getSimpleName() : message, reason,
                    sourceCommit, targetCommit, attempts, run);
        }
    }

    private static DeployReport report(String outcome, String title, String detail, String reason,
            String sourceCommit, String targetCommit, int attempts, RunReport run) {
        String traceId = run != null ? run.traceId() : UUID.randomUUID().toString();
        return new DeployReport(traceId, outcome, title, detail, reason, sourceCommit, targetCommit, attempts, run);
    }
}
