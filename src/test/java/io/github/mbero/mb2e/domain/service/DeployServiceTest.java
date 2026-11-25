package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.out.GitFailure;
import io.github.mbero.mb2e.domain.port.out.GitWorkspace;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeployServiceTest {

    private final List<String> calls = new ArrayList<>();

    /** A workspace that records its calls; the scripted answers are consumed in order. */
    private final class Fake implements GitWorkspace {
        private final String name;
        String commitId = "target-1";
        boolean changed = true;
        final Deque<PushOutcome> pushes = new ArrayDeque<>();
        String commitSubject;
        String commitBody;
        RuntimeException syncFailure;
        int syncs;

        Fake(String name) {
            this.name = name;
        }

        @Override
        public String sync() {
            calls.add(name + ".sync");
            syncs++;
            if (syncFailure != null) {
                throw syncFailure;
            }
            return name + "-sync-" + syncs;
        }

        @Override
        public boolean hasChanges() {
            calls.add(name + ".hasChanges");
            return changed;
        }

        @Override
        public String commitAll(String subject, String body) {
            calls.add(name + ".commitAll");
            commitSubject = subject;
            commitBody = body;
            return commitId;
        }

        @Override
        public PushOutcome push() {
            calls.add(name + ".push");
            return pushes.isEmpty() ? PushOutcome.PUSHED : pushes.poll();
        }
    }

    private final Fake source = new Fake("source");
    private final Fake target = new Fake("target");
    private RunReport result = ok();
    private final PipelineUseCase pipeline = (dryRun, noPublish) -> {
        calls.add("pipeline.run(" + dryRun + ", " + noPublish + ")");
        return result;
    };

    private static RunReport ok() {
        return new RunReport("t-run", "ok", null, null, false, StepResult.skipped(), StepResult.skipped());
    }

    private DeployReport deploy() {
        return new DeployService(source, target, pipeline, "chore: refresh", 3).deploy("push abc");
    }

    @Test
    void aChangedSiteIsSyncedBuiltCommittedAndPushedInThatOrder() {
        DeployReport report = deploy();
        assertThat(calls).containsExactly("source.sync", "target.sync", "pipeline.run(false, false)",
                "target.hasChanges", "target.commitAll", "target.push");
        assertThat(report.outcome()).isEqualTo(DeployReport.DEPLOYED);
        assertThat(report.attempts()).isEqualTo(1);
        assertThat(report.reason()).isEqualTo("push abc");
        assertThat(report.sourceCommit()).isEqualTo("source-sync-1");
        assertThat(report.targetCommit()).isEqualTo("target-1");
        assertThat(report.traceId()).isEqualTo("t-run");
    }

    @Test
    void theCommitCarriesTheSubjectAndTheSourceCommitInItsBody() {
        deploy();
        assertThat(target.commitSubject).isEqualTo("chore: refresh");
        assertThat(target.commitBody).isEqualTo("Source commit: source-sync-1");
    }

    @Test
    void anUnchangedSiteIsUpToDateWithNoCommitAndNoPush() {
        target.changed = false;
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.UP_TO_DATE);
        assertThat(calls).doesNotContain("target.commitAll", "target.push");
    }

    @Test
    void aRefusedRunIsRefusedWithItsOwnTitleAndNothingIsCommitted() {
        result = new RunReport("t-ref", "refused", "No tagged note", "Tag one", false,
                StepResult.of("refused", BuildReport.refused("t-ref", "No tagged note", "Tag one", false,
                        new ConfiguredPaths("/v", "/tpl", "/t"))), StepResult.notRun());
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.REFUSED);
        assertThat(report.title()).isEqualTo("No tagged note");
        assertThat(report.detail()).isEqualTo("Tag one");
        assertThat(calls).doesNotContain("target.hasChanges", "target.commitAll", "target.push");
    }

    @Test
    void aRejectedPushStartsAgainFromAFreshSync() {
        target.pushes.add(GitWorkspace.PushOutcome.REJECTED);
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.DEPLOYED);
        assertThat(report.attempts()).isEqualTo(2);
        assertThat(source.syncs).isEqualTo(2);
        assertThat(target.syncs).isEqualTo(2);
    }

    @Test
    void threeRejectedPushesFailTheDeployment() {
        for (int i = 0; i < 3; i++) {
            target.pushes.add(GitWorkspace.PushOutcome.REJECTED);
        }
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.FAILED);
        assertThat(report.isFailed()).isTrue();
        assertThat(report.attempts()).isEqualTo(3);
        assertThat(report.detail()).contains("3 times");
    }

    @Test
    void aGitFailureInTheSourceSyncFailsWithoutRunningThePipeline() {
        source.syncFailure = new GitFailure("token expired", null);
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.FAILED);
        assertThat(report.detail()).isEqualTo("token expired");
        assertThat(report.traceId()).isNotBlank();
        assertThat(calls).containsExactly("source.sync");
    }

    @Test
    void anyOtherRuntimeFailureIsAlsoAReportAndNeverThrown() {
        source.syncFailure = new IllegalStateException();
        DeployReport report = deploy();
        assertThat(report.outcome()).isEqualTo(DeployReport.FAILED);
        assertThat(report.detail()).isEqualTo("IllegalStateException");
    }

    @Test
    void theDomainClassesImportNothingFromSpring() throws Exception {
        for (String name : new String[] {"service/DeployService", "model/DeployReport", "port/in/DeployUseCase",
                "port/out/GitWorkspace"}) {
            java.nio.file.Path file = java.nio.file.Path.of("src/main/java/io/github/mbero/mb2e/domain", name + ".java");
            assertThat(java.nio.file.Files.readString(file)).doesNotContain("org.springframework");
        }
    }
}
