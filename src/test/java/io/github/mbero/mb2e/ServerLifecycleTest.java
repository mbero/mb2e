package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.model.DeployReport;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Starts the real server context with a fake deploy use case and checks start-up, poll and shutdown. */
class ServerLifecycleTest {

    static final List<String> REASONS = new CopyOnWriteArrayList<>();
    static volatile CountDownLatch block = new CountDownLatch(0);
    static volatile CountDownLatch entered = new CountDownLatch(1);

    @Configuration(proxyBeanMethods = false)
    static class Fake {
        @Bean
        DeployUseCase deployUseCase() {
            return reason -> {
                REASONS.add(reason);
                entered.countDown();
                try {
                    block.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return new DeployReport("t", DeployReport.UP_TO_DATE, null, null, reason, "s", null, 1, null);
            };
        }
    }

    @TempDir
    Path workspace;

    private ConfigurableApplicationContext context;

    @AfterEach
    void close() {
        if (context != null) {
            context.close();
        }
        REASONS.clear();
        block = new CountDownLatch(0);
        entered = new CountDownLatch(1);
    }

    private Map<String, Object> properties(String syncOnStart, String poll) throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("MB2E_SOURCE_REPO_URL", "https://github.com/me/notes");
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/me/me.github.io");
        env.put("MB2E_WEBHOOK_SECRET", "0123456789abcdef0123456789abcdef");
        env.put("MB2E_GITHUB_TOKEN", "token");
        env.put("MB2E_GIT_USER_NAME", "Bot");
        env.put("MB2E_GIT_USER_EMAIL", "bot@example.com");
        env.put("MB2E_WORKSPACE", workspace.toString());
        env.put("MB2E_TEMPLATE_PATH", "/opt/template-not-used");
        env.put("MB2E_SYNC_ON_START", syncOnStart);
        env.put("MB2E_POLL_INTERVAL_SECONDS", poll);
        Map<String, Object> properties = new HashMap<>(ServerEnvironment.properties(env, path -> null));
        try (ServerSocket free = new ServerSocket(0)) {
            properties.put("server.port", free.getLocalPort());
        }
        properties.put("spring.main.allow-bean-definition-overriding", true);
        return properties;
    }

    private ConfigurableApplicationContext start(Map<String, Object> properties) {
        return new SpringApplicationBuilder(Mb2eApplication.class, Fake.class)
                .environment(Mb2eEnvironment.forServer(null, properties)).bannerMode(Banner.Mode.OFF).run();
    }

    @Test
    void withSyncOnStartTheUseCaseReceivesOneStartupCall() throws Exception {
        context = start(properties("true", "0"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(REASONS).containsExactly("startup");
    }

    @Test
    void withoutSyncOnStartTheUseCaseIsNotCalledAndNoPollTaskExists() throws Exception {
        context = start(properties("false", "0"));
        Thread.sleep(500);
        assertThat(REASONS).isEmpty();
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.adapter.in.web.PollScheduler.class)).isEmpty();
    }

    @Test
    void closingTheContextWaitsForARunThatIsInProgress() throws Exception {
        block = new CountDownLatch(1);
        context = start(properties("true", "0"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        new Thread(() -> {
            try {
                Thread.sleep(700);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            block.countDown();
        }).start();
        long begin = System.nanoTime();
        context.close();
        assertThat(Duration.ofNanos(System.nanoTime() - begin)).isGreaterThan(Duration.ofMillis(400));
        context = null;
    }

    @Test
    void aSecondServerOnTheSameWorkspaceIsRefusedAndNamesTheLockFile() throws Exception {
        context = start(properties("false", "0"));
        Map<String, Object> second = properties("false", "0");
        assertThatThrownBy(() -> start(second))
                .satisfies(failure -> assertThat(rootOf(failure).getMessage()).contains(".mb2e.lock"));
    }

    private static Throwable rootOf(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root;
    }
}
