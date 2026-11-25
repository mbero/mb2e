package io.github.mbero.mb2e.adapter.in.web;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;

/**
 * Runs deployments on one worker thread with one waiting slot.
 * <p>Each run starts with a fresh sync, so one waiting run covers every later trigger: a burst of
 * pushes gives at most two runs, and two runs never overlap. {@link #submit} never blocks.
 */
public class DeployQueue implements DeployTrigger, DeployHistory, DisposableBean {

    /** What happened to a trigger. */
    public enum Result {
        QUEUED,
        COALESCED,
        /** The service is stopping; the trigger is dropped and the next start-up run catches it up. */
        STOPPED
    }

    /** The newest finished deployment and its finish time. */
    public record Last(DeployReport report, Instant finishedAt) {
    }

    private final DeployUseCase deploy;
    private final StepLog log;
    private final Duration shutdownWait;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1), runnable -> {
                Thread thread = new Thread(runnable, "mb2e-deploy");
                thread.setDaemon(false);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private volatile Last last;

    public DeployQueue(DeployUseCase deploy, StepLog log, Duration shutdownWait) {
        this.deploy = deploy;
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

    private void run(String reason) {
        try {
            DeployReport report = deploy.deploy(reason);
            log.deploy(report);
            last = new Last(report, Instant.now());
        } catch (RuntimeException failure) {
            log.failure("deploy", failure);
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
