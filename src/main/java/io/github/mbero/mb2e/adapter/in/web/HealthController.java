package io.github.mbero.mb2e.adapter.in.web;

import io.github.mbero.mb2e.domain.model.DeployReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Answers the health probe. A refused or failed deployment never makes the state {@code down}:
 * a restart does not repair a wrong note or a wrong token. The answer shows no path, URL or detail.
 */
@Controller
@ConditionalOnProperty(name = "mb2e.mode", havingValue = "server")
public class HealthController {

    private final DeployQueue queue;

    public HealthController(DeployQueue queue) {
        this.queue = queue;
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        String last = queue.last().map(item -> {
            DeployReport report = item.report();
            return "{\"outcome\":\"" + report.outcome() + "\",\"finishedAt\":\"" + item.finishedAt()
                    + "\",\"traceId\":\"" + report.traceId() + "\"}";
        }).orElse("null");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"up\",\"lastDeploy\":" + last + "}");
    }
}
