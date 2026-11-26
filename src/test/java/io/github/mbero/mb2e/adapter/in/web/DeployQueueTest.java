package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeployQueueTest {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger overlaps = new AtomicInteger();
    private final List<String> reasons = Collections.synchronizedList(new ArrayList<>());
    private boolean blockFirst = true;
    private volatile RuntimeException failure;
    private final List<Deployment> recorded = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean historyBroken;

    private final DeploymentHistory history = new DeploymentHistory() {
        @Override
        public void record(Deployment deployment) {
            if (historyBroken) {
                throw new StoreFailure("disk full", null);
            }
            recorded.add(deployment);
        }

        @Override
        public List<Deployment> newest(int limit) {
            return List.copyOf(recorded);
        }
    };

    private final DeployUseCase fake = reason -> {
        if (running.incrementAndGet() > 1) {
            overlaps.incrementAndGet();
        }
        try {
            reasons.add(reason);
            started.countDown();
            if (blockFirst && reasons.size() == 1) {
                release.await();
            }
            if (failure != null) {
                RuntimeException thrown = failure;
                failure = null;
                throw thrown;
            }
            return new DeployReport("t", DeployReport.UP_TO_DATE, null, null, reason, "s", null, 1, null);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        } finally {
            running.decrementAndGet();
        }
    };

    private DeployQueue queue() {
        return new DeployQueue(fake, history, new StepLog(""), Duration.ofSeconds(10));
    }

    @Test
    void tenTriggersDuringARunGiveTwoRunsAndEightCoalesced() throws Exception {
        DeployQueue queue = queue();
        List<DeployQueue.Result> results = new ArrayList<>();
        results.add(queue.submit("first"));
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        long begin = System.nanoTime();
        for (int i = 0; i < 9; i++) {
            results.add(queue.submit("later " + i));
        }
        assertThat(Duration.ofNanos(System.nanoTime() - begin)).isLessThan(Duration.ofMillis(50));
        assertThat(results.stream().filter(r -> r == DeployQueue.Result.QUEUED).count()).isEqualTo(2);
        assertThat(results.stream().filter(r -> r == DeployQueue.Result.COALESCED).count()).isEqualTo(8);
        release.countDown();
        awaitRuns(2);
        queue.destroy();
        assertThat(reasons).hasSize(2);
        assertThat(overlaps.get()).isZero();
        assertThat(queue.last()).isPresent();
    }

    @Test
    void aFailureInTheUseCaseDoesNotStopTheWorker() throws Exception {
        blockFirst = false;
        failure = new IllegalStateException("boom");
        DeployQueue queue = queue();
        queue.submit("one");
        awaitRuns(1);
        awaitIdle(queue);
        assertThat(queue.last()).isEmpty();
        assertThat(queue.submit("two")).isEqualTo(DeployQueue.Result.QUEUED);
        awaitRuns(2);
        queue.destroy();
        assertThat(queue.last()).isPresent();
    }

    @Test
    void destroyWaitsForTheCurrentRun() throws Exception {
        DeployQueue queue = queue();
        queue.submit("first");
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            release.countDown();
        });
        releaser.start();
        long begin = System.nanoTime();
        queue.destroy();
        assertThat(Duration.ofNanos(System.nanoTime() - begin)).isGreaterThan(Duration.ofMillis(200));
        assertThat(queue.last()).isPresent();
    }

    @Test
    void afterDestroyATriggerIsStoppedNotRun() throws Exception {
        blockFirst = false;
        DeployQueue queue = queue();
        queue.destroy();
        assertThat(queue.submit("late")).isEqualTo(DeployQueue.Result.STOPPED);
        Thread.sleep(200);
        assertThat(reasons).isEmpty();
    }

    @Test
    void destroyDropsTheWaitingRunAndWaitsForTheCurrentOne() throws Exception {
        DeployQueue queue = queue();
        queue.submit("current");
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(queue.submit("waiting")).isEqualTo(DeployQueue.Result.QUEUED);
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            release.countDown();
        }).start();
        queue.destroy();
        assertThat(reasons).containsExactly("current");
    }

    @Test
    void eachFinishedRunGoesToTheHistoryWithItsTrigger() throws Exception {
        blockFirst = false;
        DeployQueue queue = queue();
        queue.submit("push abc delivery d-1");
        awaitRecorded(1);
        queue.submit("poll");
        awaitRecorded(2);
        queue.destroy();
        assertThat(recorded).extracting(Deployment::trigger).containsExactly("push", "poll");
        assertThat(recorded.get(1).finishedAt()).isEqualTo(queue.last().orElseThrow().finishedAt());
    }

    @Test
    void aBrokenHistoryNeverFailsARunNorStopsTheWorker() throws Exception {
        blockFirst = false;
        historyBroken = true;
        DeployQueue queue = queue();
        queue.submit("one");
        awaitRuns(1);
        awaitIdle(queue);
        assertThat(queue.last()).isPresent();
        historyBroken = false;
        assertThat(queue.submit("two")).isEqualTo(DeployQueue.Result.QUEUED);
        awaitRecorded(1);
        queue.destroy();
        assertThat(recorded).extracting(Deployment::trigger).containsExactly("two");
    }

    @Test
    void theStateIsRunningDuringARunWaitingWithOneBehindAndIdleAfter() throws Exception {
        DeployQueue queue = queue();
        assertThat(queue.state()).isEqualTo(DeployQueue.State.IDLE);
        queue.submit("first");
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(queue.state()).isEqualTo(DeployQueue.State.RUNNING);
        queue.submit("second");
        assertThat(queue.state()).isEqualTo(DeployQueue.State.WAITING);
        release.countDown();
        awaitRecorded(2);
        for (int i = 0; i < 100 && queue.state() != DeployQueue.State.IDLE; i++) {
            Thread.sleep(20);
        }
        assertThat(queue.state()).isEqualTo(DeployQueue.State.IDLE);
        queue.destroy();
    }

    private void awaitRecorded(int count) throws InterruptedException {
        for (int i = 0; i < 100 && recorded.size() < count; i++) {
            Thread.sleep(50);
        }
        assertThat(recorded).hasSize(count);
    }

    private void awaitRuns(int count) throws InterruptedException {
        for (int i = 0; i < 100 && reasons.size() < count; i++) {
            Thread.sleep(50);
        }
        assertThat(reasons).hasSize(count);
    }

    private static void awaitIdle(DeployQueue queue) throws InterruptedException {
        Thread.sleep(200);
    }
}
