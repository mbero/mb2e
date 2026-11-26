package io.github.mbero.mb2e.adapter.in.web;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.DisposableBean;

/** Submits a {@code poll} trigger at a fixed interval, so a missed webhook is caught up. */
public class PollScheduler implements DisposableBean {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mb2e-poll");
        thread.setDaemon(true);
        return thread;
    });

    public PollScheduler(DeployQueue queue, Duration interval) {
        scheduler.scheduleWithFixedDelay(() -> queue.submit("poll"), interval.toMillis(), interval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
    }
}
