package io.github.mbero.mb2e.adapter.in.web;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;

/**
 * Runs deployments on one worker thread with one waiting slot and records each finished run in the history.
 * <p>Each run starts with a fresh sync, so one waiting run covers every later trigger: a burst of pushes gives
 * at most two runs, and two never overlap. {@link #submit} never blocks; a history failure never fails a run.
 */
public class DeployQueue implements DeployTrigger, LatestDeploy, DisposableBean {

    /** What happened to a trigger. */
    public enum Result {
        QUEUED,
        COALESCED,
        /** The service is stopping; the trigger is dropped and the next start-up run catches it up. */
        STOPPED
    }

    /** What the worker does now. */
    public enum State {
        IDLE,
        RUNNING,
        /** A run is in progress and one more waits behind it. */
        WAITING
    }

    /** The newest finished deployment and its finish time. */
    public record Last(DeployReport report, Instant finishedAt) {
    }

    private final DeployUseCase deploy;
    private final DeploymentHistory history;
    private final StepLog log;
    private final Duration shutdownWait;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1), runnable -> {
                Thread thread = new Thread(runnable, "mb2e-deploy");
                thread.setDaemon(false);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private volatile Last last;
    private volatile boolean running;

    public DeployQueue(DeployUseCase deploy, DeploymentHistory history, StepLog log, Duration shutdownWait) {
        this.deploy = deploy;
        this.history = history;
        this.log = log;
        this.shutdownWait = shutdownWait;
    }

    @Override
    public Result submit(String reason) {
        if (executor.isShutdown()) {
            return Result.STOPPED;
        }
        try {
            executor.execute(() -> run(reason));
            return Result.QUEUED;
        } catch (RejectedExecutionException rejected) {
            return executor.isShutdown() ? Result.STOPPED : Result.COALESCED;
        }
    }

    @Override
    public Optional<Last> last() {
        return Optional.ofNullable(last);
    }

    /** Tells whether a run is in progress and whether another one waits. */
    public State state() {
        if (!executor.getQueue().isEmpty()) {
            return State.WAITING;
        }
        return running ? State.RUNNING : State.IDLE;
    }

    private void run(String reason) {
        running = true;
        try {
            DeployReport report = deploy.deploy(reason);
            log.deploy(report);
            Instant finishedAt = Instant.now();
            last = new Last(report, finishedAt);
            remember(Deployment.of(report, finishedAt));
        } catch (RuntimeException failure) {
            log.failure("deploy", failure);
        } finally {
            running = false;
        }
    }

    private void remember(Deployment deployment) {
        try {
            history.record(deployment);
        } catch (RuntimeException failure) {
            log.failure("history", failure);
        }
    }

    /** Drops the waiting run, waits for the current run, then stops the worker. */
    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        executor.getQueue().clear();
        if (!executor.awaitTermination(shutdownWait.toMillis(), TimeUnit.MILLISECONDS)) {
            executor.shutdownNow();
        }
    }
}
