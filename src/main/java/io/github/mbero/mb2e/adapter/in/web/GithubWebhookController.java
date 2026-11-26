package io.github.mbero.mb2e.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Receives GitHub push webhooks. The payload is only a trigger: the repository and the branch come from the
 * configuration, and the payload is read to filter, never to decide where to deploy.
 * <p>The signature is checked before the body is parsed, so an unauthenticated caller learns nothing.
 */
public class GithubWebhookController {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The JSON answer of the endpoint; a reason appears only for a refused or ignored delivery. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Answer(String status, String reason) {
    }

    private final DeployTrigger trigger;
    private final String secret;
    private final long maxBodyBytes;
    private final String repository;
    private final String ref;

    public GithubWebhookController(DeployTrigger trigger, WebhookSettings settings) {
        this.trigger = trigger;
        this.secret = settings.secret();
        this.maxBodyBytes = settings.maxBodyBytes();
        this.repository = settings.repository().toLowerCase(Locale.ROOT);
        this.ref = "refs/heads/" + settings.branch();
    }

    public ServerResponse receive(HttpServletRequest request) throws IOException {
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
        JsonNode payload;
        try {
            payload = JSON.readTree(body);
        } catch (IOException invalid) {
            return answer(HttpStatus.BAD_REQUEST, "refused", "payload");
        }
        if (payload == null || !payload.isObject()) {
            return answer(HttpStatus.BAD_REQUEST, "refused", "payload");
        }
        if (!repository.equals(payload.path("repository").path("full_name").asText("").toLowerCase(Locale.ROOT))) {
            return answer(HttpStatus.OK, "ignored", "repository");
        }
        if (!ref.equals(payload.path("ref").asText(""))) {
            return answer(HttpStatus.OK, "ignored", "ref");
        }
        if (payload.path("deleted").asBoolean(false)) {
            return answer(HttpStatus.OK, "ignored", "deleted");
        }
        String reason = "push " + safe(payload.path("after").asText("")) + " delivery "
                + safe(request.getHeader("X-GitHub-Delivery"));
        DeployQueue.Result result = trigger.submit(reason);
        if (result == DeployQueue.Result.STOPPED) {
            return answer(HttpStatus.SERVICE_UNAVAILABLE, "refused", "stopping");
        }
        return answer(HttpStatus.ACCEPTED, result.name().toLowerCase(Locale.ROOT), null);
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

    private static ServerResponse answer(HttpStatus status, String state, String reason) {
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body(new Answer(state, reason));
    }
}
