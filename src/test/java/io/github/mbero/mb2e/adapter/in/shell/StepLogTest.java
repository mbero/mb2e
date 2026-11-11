package io.github.mbero.mb2e.adapter.in.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.PublishReport;
import io.github.mbero.mb2e.domain.model.RunReport;
import io.github.mbero.mb2e.domain.model.StepResult;
import io.github.mbero.mb2e.domain.model.SyncReport;
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
    void eachStepWritesOneInfoLineWithStepOutcomeCountsAndTraceId() {
        StepLog log = new StepLog("");
        log.publish(published());
        log.build(new BuildReport("t-2", "ok", null, null, 3, 1, 0, 0, 0, 100, "about", List.of(), List.of(),
                List.of()));
        log.sync(SyncReport.refused("t-3", "Vault not found", "No vault", false, "/v", 0));

        assertThat(events.list).hasSize(3).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
        assertThat(events.list.get(0).getFormattedMessage()).contains("step=publish", "outcome=ok",
                "traceId=t-1", "changed=2", "deleted=1", "media=1");
        assertThat(events.list.get(1).getFormattedMessage()).contains("step=build", "traceId=t-2", "notes=3");
        assertThat(events.list.get(2).getFormattedMessage()).contains("step=sync", "outcome=refused",
                "traceId=t-3", "Vault not found");
    }

    @Test
    void aRunLogsAllThreeStepsEvenWhenOneIsSkipped() {
        new StepLog("").run(new RunReport("t-4", "ok", null, null, false, null, StepResult.skipped(),
                StepResult.skipped(), StepResult.of("ok", published())));
        assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage).satisfiesExactly(
                a -> assertThat(a).contains("step=sync", "outcome=skipped", "traceId=t-4"),
                b -> assertThat(b).contains("step=build", "outcome=skipped"),
                c -> assertThat(c).contains("step=publish", "outcome=ok"));
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
                "java.lang.IllegalStateException: boom", "at io.github.mbero.mb2e.adapter.in.shell.StepLogTest");
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
}
