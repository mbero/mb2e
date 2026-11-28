package io.github.mbero.mb2e.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbero.mb2e.adapter.in.log.StepLog;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Receives GitHub push webhooks. The payload is only a trigger: the repository and the branch come from the
 * configuration, and the payload is read to filter, never to decide where to deploy.
 * <p>The signature is checked before parsing, so an unauthenticated caller learns nothing; each delivery logs one line.
 */
public class GithubWebhookController {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The JSON answer of the endpoint; a reason appears only for a refused or ignored delivery. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Answer(String status, String reason) {
    }

    /** The HTTP status of an answer and its body. */
    private record Decision(HttpStatus http, Answer answer) {
    }

    private final DeployTrigger trigger;
    private final String secret;
    private final long maxBodyBytes;
    private final String repository;
    private final String ref;
    private final StepLog log;

    public GithubWebhookController(DeployTrigger trigger, WebhookSettings settings, StepLog log) {
        this.trigger = trigger;
        this.log = log;
        this.secret = settings.secret();
        this.maxBodyBytes = settings.maxBodyBytes();
        this.repository = settings.repository().toLowerCase(Locale.ROOT);
        this.ref = "refs/heads/" + settings.branch();
    }

    public ServerResponse receive(HttpServletRequest request) throws IOException {
        String delivery = request.getHeader("X-GitHub-Delivery");
        Decision decision = decide(request, delivery);
        log.webhook(decision.answer().status(), decision.answer().reason(), delivery);
        return ServerResponse.status(decision.http()).contentType(MediaType.APPLICATION_JSON).body(decision.answer());
    }

    private Decision decide(HttpServletRequest request, String delivery) throws IOException {
        if (request.getContentLengthLong() > maxBodyBytes) {
            return answer(HttpStatus.PAYLOAD_TOO_LARGE, "refused", "body too large");
        }
        byte[] body = readLimited(request.getInputStream());
        if (body == null) {
            return answer(HttpStatus.PAYLOAD_TOO_LARGE, "refused", "body too large");
        }
        if (!WebhookSignature.isValid(secret, body, request.getHeader("X-Hub-Signature-256"))) {
            return answer(HttpStatus.UNAUTHORIZED, "refused", "signature");
        }
        String event = request.getHeader("X-GitHub-Event");
        if ("ping".equals(event)) {
            return answer(HttpStatus.OK, "pong", null);
        }
        if (!"push".equals(event)) {
            return answer(HttpStatus.OK, "ignored", "event");
        }
        JsonNode payload = parse(body);
        if (payload == null) {
            return answer(HttpStatus.BAD_REQUEST, "refused", "payload");
        }
        Optional<String> ignored = ignoredPush(payload);
        if (ignored.isPresent()) {
            return answer(HttpStatus.OK, "ignored", ignored.get());
        }
        String reason = "push " + safe(payload.path("after").asText("")) + " delivery " + safe(delivery);
        DeployQueue.Result result = trigger.submit(reason);
        if (result == DeployQueue.Result.STOPPED) {
            return answer(HttpStatus.SERVICE_UNAVAILABLE, "refused", "stopping");
        }
        return answer(HttpStatus.ACCEPTED, result.name().toLowerCase(Locale.ROOT), null);
    }

    /** Returns the JSON object of the body, or null when the body is not one. */
    private static JsonNode parse(byte[] body) {
        try {
            JsonNode payload = JSON.readTree(body);
            return payload != null && payload.isObject() ? payload : null;
        } catch (IOException invalid) {
            return null;
        }
    }

    /** Names why a valid push does not concern this service: another repository, another branch, a deletion. */
    private Optional<String> ignoredPush(JsonNode payload) {
        if (!repository.equals(payload.path("repository").path("full_name").asText("").toLowerCase(Locale.ROOT))) {
            return Optional.of("repository");
        }
        if (!ref.equals(payload.path("ref").asText(""))) {
            return Optional.of("ref");
        }
        return payload.path("deleted").asBoolean(false) ? Optional.of("deleted") : Optional.empty();
    }

    /** Reads at most the limit; returns null when the body is larger. */
    private byte[] readLimited(InputStream stream) throws IOException {
        long limit = Math.min(maxBodyBytes, Integer.MAX_VALUE - 9L);
        byte[] body = stream.readNBytes((int) (limit + 1));
        return body.length > limit ? null : body;
    }

    /** Keeps a header or payload value harmless in a log line. */
    private static String safe(String value) {
        if (value == null) {
            return "-";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "?");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    private static Decision answer(HttpStatus status, String state, String reason) {
        return new Decision(status, new Answer(state, reason));
    }
}
