package io.github.mbero.mb2e.adapter.in.web;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Answers the read-only JSON API of the dashboard: the state of the service and the newest deployments.
 * <p>No answer holds a token or a local path; a failure cause shows {@code <local path>} in place of a path.
 */
public class DashboardApiController {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    /** The state of the service, its start time and uptime, the repositories and the attempt limit. */
    record Status(String state, String startedAt, long uptimeSeconds, String sourceRepo, String targetRepo,
            String siteUrl, int maxAttempts) {
    }

    /** One deployment; times are ISO-8601 instants in UTC. */
    record Item(String finishedAt, String outcome, String trigger, int attempts, String sourceCommit,
            String targetCommit, String cause) {
    }

    /** A short error answer with no stack trace. */
    record Problem(String error) {
    }

    private final Supplier<DeployQueue.State> state;
    private final DeploymentHistory history;
    private final DashboardSettings settings;
    private final Instant startedAt;
    private final Clock clock;
    private final StepLog log;

    public DashboardApiController(Supplier<DeployQueue.State> state, DeploymentHistory history,
            DashboardSettings settings, Clock clock, StepLog log) {
        this.state = state;
        this.history = history;
        this.settings = settings;
        this.clock = clock;
        this.startedAt = clock.instant();
        this.log = log;
    }

    public ServerResponse status() {
        Instant now = clock.instant();
        return json(HttpStatus.OK, new Status(state.get().name().toLowerCase(Locale.ROOT), startedAt.toString(),
                Duration.between(startedAt, now).toSeconds(), settings.sourceRepo(), settings.targetRepo(),
                settings.siteUrl(), settings.maxAttempts()));
    }

    public ServerResponse deployments(ServerRequest request) {
        Optional<Integer> limit = limit(request.param("limit"));
        if (limit.isEmpty()) {
            return json(HttpStatus.BAD_REQUEST,
                    new Problem("limit must be a whole number from 1 to " + MAX_LIMIT + "."));
        }
        List<Deployment> newest;
        try {
            newest = history.newest(limit.get());
        } catch (RuntimeException failure) {
            log.failure("history", failure);
            return json(HttpStatus.SERVICE_UNAVAILABLE, new Problem("The deployment history is not available."));
        }
        return json(HttpStatus.OK, newest.stream().map(this::item).toList());
    }

    private static Optional<Integer> limit(Optional<String> raw) {
        if (raw.isEmpty()) {
            return Optional.of(DEFAULT_LIMIT);
        }
        try {
            int value = Integer.parseInt(raw.get().trim());
            return value >= 1 && value <= MAX_LIMIT ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException failure) {
            return Optional.empty();
        }
    }

    private Item item(Deployment deployment) {
        return new Item(deployment.finishedAt().toString(), deployment.outcome(), deployment.trigger(),
                deployment.attempts(), deployment.sourceCommit(), deployment.targetCommit(),
                hidePaths(deployment.cause()));
    }

    private String hidePaths(String cause) {
        if (cause == null) {
            return null;
        }
        String out = cause;
        for (String path : settings.hiddenPaths()) {
            out = out.replace(path, "<local path>");
        }
        return out;
    }

    private static ServerResponse json(HttpStatus status, Object body) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
