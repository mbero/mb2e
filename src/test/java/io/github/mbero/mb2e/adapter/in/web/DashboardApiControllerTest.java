package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardApiControllerTest {

    private static final Instant START = Instant.parse("2026-11-26T08:00:00Z");

    /** A clock that the test moves forward by hand. */
    private static final class MovableClock extends Clock {
        private Instant now = START;

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MovableClock clock = new MovableClock();
    private final List<Deployment> stored = new ArrayList<>();
    private final List<Integer> askedLimits = new ArrayList<>();
    private boolean historyBroken;
    private DeployQueue.State state = DeployQueue.State.IDLE;

    private final DeploymentHistory history = new DeploymentHistory() {
        @Override
        public void record(Deployment deployment) {
            stored.add(deployment);
        }

        @Override
        public List<Deployment> newest(int limit) {
            askedLimits.add(limit);
            if (historyBroken) {
                throw new StoreFailure("cannot open /var/lib/mb2e/history.db", null);
            }
            return stored.stream().limit(limit).toList();
        }
    };

    private final MockMvc mvc = MockMvcBuilders.routerFunctions(WebRoutes.dashboard(new DashboardApiController(
            () -> state, history, new DashboardSettings("me/notes", "me/me.github.io", 3,
                    List.of("/var/lib/mb2e", "/opt/mb2e/template/mbs-blog")), clock, new StepLog("")))).build();

    @Test
    void theStatusShowsStateUptimeRepositoriesSiteAndAttemptLimit() throws Exception {
        clock.now = START.plus(Duration.ofHours(3).plusMinutes(4));
        state = DeployQueue.State.WAITING;
        mvc.perform(get("/api/status")).andExpect(status().isOk())
                .andExpect(content().json("""
                        {"state":"waiting","startedAt":"2026-11-26T08:00:00Z","uptimeSeconds":11040,
                         "sourceRepo":"me/notes","targetRepo":"me/me.github.io",
                         "siteUrl":"https://me.github.io","maxAttempts":3}""", true));
    }

    @Test
    void deploymentsComeNewestFirstWithUtcTimesAndNullsForMissingCommits() throws Exception {
        stored.add(new Deployment(Instant.parse("2026-11-26T09:14:03Z"), "failed", "push", 3, "a41c9e2", null,
                "Git failed\ngit push failed in /var/lib/mb2e/target"));
        stored.add(new Deployment(Instant.parse("2026-11-26T08:52:40Z"), "deployed", "poll", 1, "7d03b1f",
                "9f8e7d6", null));
        mvc.perform(get("/api/deployments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].finishedAt").value("2026-11-26T09:14:03Z"))
                .andExpect(jsonPath("$[0].outcome").value("failed"))
                .andExpect(jsonPath("$[0].targetCommit").doesNotExist())
                .andExpect(jsonPath("$[0].cause").value("Git failed\ngit push failed in <local path>/target"))
                .andExpect(jsonPath("$[1].trigger").value("poll"))
                .andExpect(jsonPath("$[1].cause").isEmpty())
                .andExpect(content().string(not(containsString("/var/lib"))));
        assertThat(askedLimits).containsExactly(DashboardApiController.DEFAULT_LIMIT);
    }

    @Test
    void aValidLimitReachesTheHistory() throws Exception {
        mvc.perform(get("/api/deployments").param("limit", "500")).andExpect(status().isOk());
        mvc.perform(get("/api/deployments").param("limit", "1")).andExpect(status().isOk());
        assertThat(askedLimits).containsExactly(500, 1);
    }

    @Test
    void aWrongLimitIsABadRequestThatNeverReadsTheHistory() throws Exception {
        for (String limit : List.of("0", "501", "-3", "abc", "")) {
            mvc.perform(get("/api/deployments").param("limit", limit)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("limit must be a whole number from 1 to 500."));
        }
        assertThat(askedLimits).isEmpty();
    }

    @Test
    void aBrokenHistoryGivesAShortServiceUnavailableAnswerWithNoPath() throws Exception {
        historyBroken = true;
        mvc.perform(get("/api/deployments")).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"error\":\"The deployment history is not available.\"}", true))
                .andExpect(content().string(not(containsString("/var/lib"))));
    }

    @Test
    void theSiteOfAProjectRepositoryHasTheRepositoryNameAsPath() {
        assertThat(new DashboardSettings("me/notes", "Me/blog", 3, List.of()).siteUrl())
                .isEqualTo("https://me.github.io/blog");
        assertThat(new DashboardSettings("me/notes", "me/ME.github.io", 3, List.of()).siteUrl())
                .isEqualTo("https://me.github.io");
        assertThat(new DashboardSettings("me/notes", null, 3, List.of()).siteUrl()).isNull();
    }

    @Test
    void meAnswersTheSignedInLogin() throws Exception {
        mvc.perform(get("/api/me").principal(() -> "me")).andExpect(status().isOk())
                .andExpect(content().json("{\"login\":\"me\"}", true));
    }

    @Test
    void meWithoutAPrincipalIsUnauthorized() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Sign in first."));
    }
}
