package io.github.mbero.mb2e.adapter.in.web;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Answers the health probe. A refused or failed deployment never makes the state {@code down}:
 * a restart does not repair a wrong note or a wrong token. The answer shows no path, URL or detail.
 */
public class HealthController {

    /** The JSON answer: the state of the service and the newest finished deployment, or null. */
    record Health(String status, LastDeploy lastDeploy) {
    }

    /** The newest deployment: its outcome, its finish time and its trace id. */
    record LastDeploy(String outcome, String finishedAt, String traceId) {
    }

    private final LatestDeploy history;

    public HealthController(LatestDeploy history) {
        this.history = history;
    }

    public ServerResponse health() {
        LastDeploy last = history.last().map(item -> new LastDeploy(item.report().outcome(),
                item.finishedAt().toString(), item.report().traceId())).orElse(null);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(new Health("up", last));
    }
}
