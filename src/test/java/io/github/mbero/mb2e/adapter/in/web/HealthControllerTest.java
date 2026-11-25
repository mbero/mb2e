package io.github.mbero.mb2e.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mbero.mb2e.domain.model.DeployReport;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    private Optional<DeployQueue.Last> last = Optional.empty();
    private final MockMvc mvc = MockMvcBuilders.routerFunctions(WebRoutes.of(
            new GithubWebhookController(reason -> DeployQueue.Result.QUEUED,
                    new WebhookSettings("0123456789abcdef0123456789abcdef", 1024, "me/notes", "main")),
            new HealthController(() -> last))).build();

    @Test
    void beforeTheFirstRunTheServiceIsUpWithNoLastDeploy() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"up\",\"lastDeploy\":null}"));
    }

    @Test
    void aFailedRunKeepsTheServiceUpAndShowsNoPathOrUrl() throws Exception {
        DeployReport failed = new DeployReport("11111111-2222", DeployReport.FAILED, "Git failed",
                "git push failed in /var/lib/mb2e/target: https://github.com/me/x", "push", "s", null, 3, null);
        last = Optional.of(new DeployQueue.Last(failed, Instant.parse("2026-11-25T10:00:00Z")));
        mvc.perform(get("/healthz")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("up"))
                .andExpect(jsonPath("$.lastDeploy.outcome").value("failed"))
                .andExpect(jsonPath("$.lastDeploy.finishedAt").value("2026-11-25T10:00:00Z"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/"))));
    }
}
