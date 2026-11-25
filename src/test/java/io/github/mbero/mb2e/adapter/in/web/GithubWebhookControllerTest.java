package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
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

    private final DeployQueue queue = mock(DeployQueue.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new GithubWebhookController(queue, new WebhookSettings(SECRET, 1024, "me/notes", "main"))).build();

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
        verifyNoInteractions(queue);
    }

    @Test
    void aMissingOrWrongSignatureIsRefusedWith401AndNothingIsQueued() throws Exception {
        send("push", PUSH, null).andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"signature\"}"));
        send("push", PUSH, sign("another secret of enough length 123456", PUSH)).andExpect(status().isUnauthorized());
        send("push", PUSH, "sha256=zz").andExpect(status().isUnauthorized());
        verifyNoInteractions(queue);
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
        verifyNoInteractions(queue);
    }

    @Test
    void anotherRepositoryAnotherRefOrADeletedBranchIsIgnored() throws Exception {
        signed("push", PUSH.replace("Me/Notes", "me/other")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"repository\"}"));
        signed("push", PUSH.replace("refs/heads/main", "refs/heads/dev")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"ref\"}"));
        signed("push", PUSH.replace("\"deleted\":false", "\"deleted\":true")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ignored\",\"reason\":\"deleted\"}"));
        verifyNoInteractions(queue);
    }

    @Test
    void aValidPushIsAcceptedAndQueuedWithAHarmlessReason() throws Exception {
        when(queue.submit("push abc123 delivery d-1")).thenReturn(DeployQueue.Result.QUEUED);
        signed("push", PUSH).andExpect(status().isAccepted()).andExpect(content().json("{\"status\":\"queued\"}"));
        verify(queue).submit("push abc123 delivery d-1");
        when(queue.submit("push ??x? delivery d-1")).thenReturn(DeployQueue.Result.COALESCED);
        signed("push", PUSH.replace("abc123", "\\n/x\\n")).andExpect(status().isAccepted())
                .andExpect(content().json("{\"status\":\"coalesced\"}"));
    }

    @Test
    void aPushWhileTheServiceStopsGets503() throws Exception {
        when(queue.submit("push abc123 delivery d-1")).thenReturn(DeployQueue.Result.STOPPED);
        signed("push", PUSH).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"refused\",\"reason\":\"stopping\"}"));
    }

    @Test
    void theLargestAcceptedBodyLimitStillReadsTheBody() throws Exception {
        MockMvc largest = MockMvcBuilders.standaloneSetup(new GithubWebhookController(queue,
                new WebhookSettings(SECRET, Long.MAX_VALUE, "me/notes", "main"))).build();
        when(queue.submit("push abc123 delivery d-1")).thenReturn(DeployQueue.Result.QUEUED);
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
        }, new io.github.mbero.mb2e.adapter.in.log.StepLog(""), java.time.Duration.ofSeconds(5));
        MockMvc blocked = MockMvcBuilders.standaloneSetup(
                new GithubWebhookController(real, new WebhookSettings(SECRET, 1024, "me/notes", "main"))).build();
        long begin = System.nanoTime();
        blocked.perform(post("/webhook/github").content(PUSH).header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", sign(SECRET, PUSH))).andExpect(status().isAccepted());
        assertThat(java.time.Duration.ofNanos(System.nanoTime() - begin)).isLessThan(java.time.Duration.ofMillis(500));
        release.countDown();
        real.destroy();
    }

    @Test
    void otherMethodsAndPathsGetPlainErrorsWithoutAStackTrace() throws Exception {
        mvc.perform(get("/webhook/github")).andExpect(status().isMethodNotAllowed());
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
