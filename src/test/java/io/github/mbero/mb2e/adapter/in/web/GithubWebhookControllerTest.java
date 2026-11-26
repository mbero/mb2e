package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Calls the controller with a real HMAC; the signature check is never switched off. */
class GithubWebhookControllerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String PUSH = "{\"ref\":\"refs/heads/main\",\"after\":\"abc123\","
            + "\"deleted\":false,\"repository\":{\"full_name\":\"Me/Notes\"}}";

    /** Records each trigger and answers with the result that the test sets. */
    private static final class RecordingTrigger implements DeployTrigger {
        private final List<String> reasons = new ArrayList<>();
        private DeployQueue.Result next = DeployQueue.Result.QUEUED;

        @Override
        public DeployQueue.Result submit(String reason) {
            reasons.add(reason);
            return next;
        }
    }

    private final RecordingTrigger queue = new RecordingTrigger();
    private final Path logFile = tempLog();
    private final MockMvc mvc = server(queue, 1024, logFile);

    private static Path tempLog() {
        try {
            Path file = Files.createTempFile("mb2e-webhook", ".log");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static MockMvc server(DeployTrigger trigger, long maxBodyBytes) {
        return server(trigger, maxBodyBytes, null);
    }

    private static MockMvc server(DeployTrigger trigger, long maxBodyBytes, Path log) {
        GithubWebhookController webhook = new GithubWebhookController(trigger,
                new WebhookSettings(SECRET, maxBodyBytes, "me/notes", "main"),
                new StepLog(log == null ? "" : log.toString()));
        HealthController health = new HealthController(java.util.Optional::empty);
        return MockMvcBuilders.routerFunctions(WebRoutes.of(webhook, health)).build();
    }

    private static String sign(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private ResultActions send(String event, String body, String signature) throws Exception {
        MockHttpServletRequestBuilder request = post("/webhook/github").contentType(MediaType.APPLICATION_JSON)
                .content(body).header("X-GitHub-Event", event).header("X-GitHub-Delivery", "d-1");
        if (signature != null) {
            request = request.header("X-Hub-Signature-256", signature);
        }
        return mvc.perform(request);
    }

    private ResultActions signed(String event, String body) throws Exception {
        return send(event, body, sign(SECRET, body));
    }

    @Test
    void aBodyLargerThanTheLimitIsRefusedWith413() throws Exception {
        String big = "x".repeat(2000);
        send("push", big, sign(SECRET, big)).andExpect(status().isPayloadTooLarge())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"body too large\"}"));
        assertThat(queue.reasons).isEmpty();
    }

    @Test
    void aMissingOrWrongSignatureIsRefusedWith401AndNothingIsQueued() throws Exception {
        send("push", PUSH, null).andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"signature\"}"));
        send("push", PUSH, sign("another secret of enough length 123456", PUSH)).andExpect(status().isUnauthorized());
        send("push", PUSH, "sha256=zz").andExpect(status().isUnauthorized());
        assertThat(queue.reasons).isEmpty();
    }

    @Test
    void everyDeliveryWritesOneLogLineAndAForgedDeliveryIdStaysOneWord() throws Exception {
        send("push", PUSH, null);
        signed("push", PUSH);
        mvc.perform(post("/webhook/github").contentType(MediaType.APPLICATION_JSON).content("{}")
                .header("X-GitHub-Event", "ping").header("X-GitHub-Delivery", "d-2\nstep=deploy outcome=deployed"));
        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).endsWith("step=webhook outcome=refused reason=\"signature\" delivery=d-1");
        assertThat(lines.get(1)).endsWith("step=webhook outcome=queued delivery=d-1");
        assertThat(lines.get(2)).endsWith("step=webhook outcome=refused reason=\"signature\""
                + " delivery=d-2_step_deploy_outcome_deployed");
    }

    @Test
    void aPingGetsPong() throws Exception {
        signed("ping", "{\"zen\":\"x\"}").andExpect(status().isOk()).andExpect(content().json("{\"status\":\"pong\"}"));
    }

    @Test
    void anotherEventIsIgnored() throws Exception {
        signed("issues", "{}").andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"event\"}"));
    }

    @Test
    void aBodyThatIsNotAJsonObjectIsRefusedWith400() throws Exception {
        signed("push", "[1,2]").andExpect(status().isBadRequest())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"payload\"}"));
        signed("push", "not json").andExpect(status().isBadRequest());
        assertThat(queue.reasons).isEmpty();
    }

    @Test
    void anotherRepositoryAnotherRefOrADeletedBranchIsIgnored() throws Exception {
        signed("push", PUSH.replace("Me/Notes", "me/other")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"repository\"}"));
        signed("push", PUSH.replace("refs/heads/main", "refs/heads/dev")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"ref\"}"));
        signed("push", PUSH.replace("\"deleted\":false", "\"deleted\":true")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"deleted\"}"));
        assertThat(queue.reasons).isEmpty();
    }

    @Test
    void aValidPushIsAcceptedAndQueuedWithAHarmlessReason() throws Exception {
        signed("push", PUSH).andExpect(status().isAccepted()).andExpect(content().json("{\"status\":\"queued\"}"));
        queue.next = DeployQueue.Result.COALESCED;
        signed("push", PUSH.replace("abc123", "\\n/x\\n")).andExpect(status().isAccepted())
                .andExpect(content().json("{\"status\":\"coalesced\"}"));
        assertThat(queue.reasons).containsExactly("push abc123 delivery d-1", "push ??x? delivery d-1");
    }

    @Test
    void aPushWhileTheServiceStopsGets503() throws Exception {
        queue.next = DeployQueue.Result.STOPPED;
        signed("push", PUSH).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"stopping\"}"));
    }

    @Test
    void theLargestAcceptedBodyLimitStillReadsTheBody() throws Exception {
        MockMvc largest = server(queue, Long.MAX_VALUE);
        largest.perform(post("/webhook/github").contentType(MediaType.APPLICATION_JSON).content(PUSH)
                .header("X-GitHub-Event", "push").header("X-GitHub-Delivery", "d-1")
                .header("X-Hub-Signature-256", sign(SECRET, PUSH))).andExpect(status().isAccepted());
    }

    @Test
    void aValidPushIsAnsweredFastWhileTheUseCaseBlocks() throws Exception {
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        DeployQueue real = new DeployQueue(reason -> {
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return new io.github.mbero.mb2e.domain.model.DeployReport("t", "up_to_date", null, null, reason, null,
                    null, 1, null);
        }, new io.github.mbero.mb2e.domain.port.out.DeploymentHistory() {
            @Override
            public void record(io.github.mbero.mb2e.domain.model.Deployment deployment) {
            }

            @Override
            public java.util.List<io.github.mbero.mb2e.domain.model.Deployment> newest(int limit) {
                return java.util.List.of();
            }
        }, new io.github.mbero.mb2e.adapter.in.log.StepLog(""), java.time.Duration.ofSeconds(5));
        MockMvc blocked = server(real, 1024);
        long begin = System.nanoTime();
        blocked.perform(post("/webhook/github").content(PUSH).header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", sign(SECRET, PUSH))).andExpect(status().isAccepted());
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - begin)).isLessThan(java.time.Duration.ofMillis(500));
        release.countDown();
        real.destroy();
    }

    @Test
    void otherMethodsAndPathsGetPlainErrorsWithoutAStackTrace() throws Exception {
        mvc.perform(get("/webhook/github")).andExpect(status().isNotFound());
        mvc.perform(post("/other")).andExpect(status().isNotFound());
        assertThat(mvc.perform(get("/webhook/github")).andReturn().getResponse().getContentAsString())
                .doesNotContain("Exception", "at io.github");
    }

    @Test
    void theControllerImportsNothingFromTheOuterLayers() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/io/github/mbero/mb2e/adapter/in/web/GithubWebhookController.java"));
        assertThat(source).doesNotContain("adapter.out", "adapter.in.shell", "domain.service");
    }
}
