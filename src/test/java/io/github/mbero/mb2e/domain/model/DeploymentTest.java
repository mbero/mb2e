package io.github.mbero.mb2e.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeploymentTest {

    private static final Instant AT = Instant.parse("2026-11-26T10:00:00Z");

    private static DeployReport report(String outcome, String reason, String title, String detail) {
        return new DeployReport("t", outcome, title, detail, reason, "src1", "tgt1", 2, null);
    }

    @Test
    void theTriggerIsTheFirstWordOfTheReason() {
        assertThat(Deployment.of(report(DeployReport.DEPLOYED, "push abc delivery d-1", null, null), AT).trigger())
                .isEqualTo("push");
        assertThat(Deployment.of(report(DeployReport.UP_TO_DATE, "startup", null, null), AT).trigger())
                .isEqualTo("startup");
        assertThat(Deployment.of(report(DeployReport.UP_TO_DATE, null, null, null), AT).trigger())
                .isEqualTo("unknown");
    }

    @Test
    void aFailedRunJoinsTitleAndDetailIntoTheCause() {
        Deployment failed = Deployment.of(report(DeployReport.FAILED, "poll", "Git failed", "token expired"), AT);
        assertThat(failed.cause()).isEqualTo("Git failed\ntoken expired");
        assertThat(failed).extracting(Deployment::finishedAt, Deployment::attempts, Deployment::sourceCommit,
                Deployment::targetCommit).containsExactly(AT, 2, "src1", "tgt1");
    }

    @Test
    void aRefusedRunWithOnlyATitleKeepsTheTitle() {
        assertThat(Deployment.of(report(DeployReport.REFUSED, "push", "No note carries the tag", null), AT).cause())
                .isEqualTo("No note carries the tag");
    }

    @Test
    void aSuccessfulRunHasNoCause() {
        assertThat(Deployment.of(report(DeployReport.DEPLOYED, "push", "x", "y"), AT).cause()).isNull();
        assertThat(Deployment.of(report(DeployReport.UP_TO_DATE, "push", "x", "y"), AT).cause()).isNull();
    }
}
