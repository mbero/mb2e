package io.github.mbero.mb2e.adapter.in.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class StepLogTest {

    @TempDir
    Path dir;

    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger("mb2e.step");

    @BeforeEach
    void attach() {
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(events);
    }

    private static PublishReport published() {
        return new PublishReport("t-1", "ok", null, null, true, "/t", "main", null, List.of(),
                List.of("a", "b"), List.of(), List.of("x"), List.of("m"), List.of(), List.of());
    }

    @Test
    void eachStepWritesOneLineWithStepOutcomeCountsAndTraceIdAtTheLevelOfItsOutcome() {
        StepLog log = new StepLog("");
        log.publish(published());
        log.build(new BuildReport("t-2", "ok", null, null, false, "/v", "/tpl", 4, 3, List.of("a.png"), 0,
                List.of(), List.of(), 100, "about", List.of()));
        log.build(BuildReport.refused("t-3", "Vault not found", "No vault", false,
                new ConfiguredPaths("/v", "/tpl", null)));

        assertThat(events.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.INFO, Level.INFO, Level.WARN);
        assertThat(events.list.get(0).getFormattedMessage()).contains("step=publish", "outcome=ok",
                "traceId=t-1", "changed=2", "deleted=1", "media=1");
        assertThat(events.list.get(1).getFormattedMessage()).contains("step=build", "traceId=t-2", "notes=3",
                "attachments=1");
        assertThat(events.list.get(2).getFormattedMessage()).contains("step=build", "outcome=refused",
                "traceId=t-3", "Vault not found");
    }

    @Test
    void aRunLogsBothStepsEvenWhenOneIsSkipped() {
        new StepLog("").run(new RunReport("t-4", "ok", null, null, false, StepResult.skipped(),
                StepResult.of("ok", published())));
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).satisfiesExactly(
                a -> assertThat(a).contains("step=build", "outcome=skipped", "traceId=t-4"),
                b -> assertThat(b).contains("step=publish", "outcome=ok"));
    }

    @Test
    void withoutAFileNothingIsWrittenToDisk() throws IOException {
        new StepLog(" ").publish(published());
        try (var listing = Files.list(dir)) {
            assertThat(listing).isEmpty();
        }
    }

    @Test
    void aConfiguredFileReceivesEveryLineAndTheStackTraceOfAFailure() throws IOException {
        Path file = dir.resolve("logs/mb2e.log");
        StepLog log = new StepLog(file.toString());
        log.publish(published());
        assertThatThrownBy(() -> log.guarded("build", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        String text = Files.readString(file);
        assertThat(text).contains("step=publish", "traceId=t-1", "step=build outcome=failed", "traceId=",
                "java.lang.IllegalStateException: boom", "at io.github.mbero.mb2e.adapter.in.log.StepLogTest");
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("step=build outcome=failed") && !message.contains("\tat "));
    }

    @Test
    void anUnwritableFileDoesNotFailTheRun() throws IOException {
        Path blocker = Files.writeString(dir.resolve("file"), "x");
        new StepLog(blocker.resolve("sub/log").toString()).publish(published());
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("cannot be written"));
    }

    @Test
    void aDeploymentWritesOneLineWithAttemptsAndCommits() {
        new StepLog("").deploy(new DeployReport("t-9", "deployed", null, null, "push", "abc123", "def456", 2, null));
        new StepLog("").deploy(new DeployReport("t-10", "failed", "Git failed", "boom", "poll", null, null, 1, null));
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).satisfiesExactly(
                a -> assertThat(a).contains("step=deploy outcome=deployed traceId=t-9 reason=\"push\" attempts=2",
                        "sourceCommit=abc123", "targetCommit=def456").doesNotContain("detail="),
                b -> assertThat(b).contains("step=deploy outcome=failed", "sourceCommit=-", "targetCommit=-",
                        "title=\"Git failed\"", "detail=\"boom\""));
    }

    @Test
    void aMultiLineDetailStaysOnOneConsoleLine() {
        new StepLog("").deploy(new DeployReport("t-11", "failed", "Git failed",
                "remote: \"Invalid\" password\nfatal: denied", "push abc", null, null, 1, null));
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).singleElement()
                .satisfies(line -> assertThat(line).doesNotContain("\n")
                        .contains("detail=\"remote: 'Invalid' password fatal: denied\""));
    }

    @Test
    void aFailedDeploymentIsAnErrorAndAFailureKeepsItsMessageOnOneQuotedLine() {
        StepLog log = new StepLog("");
        log.deploy(new DeployReport("t-12", "failed", "Git failed", "boom", "poll", null, null, 1, null));
        log.failure("history", new IllegalStateException("cannot open\n\"history.db\""));
        assertThat(events.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.ERROR, Level.ERROR);
        assertThat(events.list.get(1).getFormattedMessage()).endsWith("error=\"cannot open 'history.db'\"");
    }

    @Test
    void aTitleWithALineBreakCannotForgeASecondLine() {
        new StepLog("").build(BuildReport.refused("t-13", "Bad\nstep=publish outcome=ok", "x", false,
                new ConfiguredPaths("/v", "/tpl", null)));
        assertThat(events.list).singleElement().extracting(ILoggingEvent::getFormattedMessage)
                .satisfies(line -> assertThat(line).doesNotContain("\n").contains("title=\"Bad step=publish outcome=ok\""));
    }

    @Test
    void theStartLineAndALoginRefusalCarryOnlySafeWords() {
        StepLog log = new StepLog("");
        log.start("port=8080 dashboard=on");
        log.login("denied", "some one\nelse");
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
                "step=start outcome=started port=8080 dashboard=on", "step=login outcome=denied login=some_one_else");
        assertThat(events.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.INFO, Level.WARN);
    }
}
