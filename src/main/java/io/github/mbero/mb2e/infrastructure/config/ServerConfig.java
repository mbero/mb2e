package io.github.mbero.mb2e.infrastructure.config;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.adapter.in.web.DashboardApiController;
import io.github.mbero.mb2e.adapter.in.web.DeployQueue;
import io.github.mbero.mb2e.adapter.in.web.GithubWebhookController;
import io.github.mbero.mb2e.adapter.in.web.HealthController;
import io.github.mbero.mb2e.adapter.in.web.PollScheduler;
import io.github.mbero.mb2e.adapter.in.web.WebRoutes;
import io.github.mbero.mb2e.adapter.in.web.WebhookSettings;
import io.github.mbero.mb2e.adapter.out.external.GitCredentials;
import io.github.mbero.mb2e.adapter.out.external.GitWorkspaceProgram;
import io.github.mbero.mb2e.adapter.out.persistence.SqliteDeploymentHistory;
import io.github.mbero.mb2e.domain.port.in.DeployUseCase;
import io.github.mbero.mb2e.domain.port.in.PipelineUseCase;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import io.github.mbero.mb2e.domain.port.out.GitWorkspace;
import io.github.mbero.mb2e.domain.service.DeployService;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Beans of the server mode: the workspace lock, the two git clones, the deploy use case, the queue, the
 * triggers and the deployment history. Only this configuration creates a {@link GitWorkspace}, so the CLI cannot push.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "mb2e.mode", havingValue = "server")
@EnableConfigurationProperties(ServerProperties.class)
public class ServerConfig {

    private static final Duration LOCAL_BUDGET = Duration.ofSeconds(30);
    /** The attempt limit of one deployment; the dashboard shows it as "n of 3". */
    static final int ATTEMPTS = 3;
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(60);
    /** The history file lives on the workspace volume, next to the two clones, so it survives a restart. */
    private static final String HISTORY_FILE = "history.db";

    @Bean(destroyMethod = "close")
    WorkspaceLock workspaceLock(ServerProperties properties) {
        return WorkspaceLock.acquire(Path.of(properties.workspace()));
    }

    @Bean
    GitWorkspace sourceWorkspace(ServerProperties properties, WorkspaceLock lock) {
        ServerProperties.Source source = properties.source();
        return workspace(GitWorkspaceProgram.Role.SOURCE, properties, source.url(), source.branch(), source.token());
    }

    @Bean
    GitWorkspace targetWorkspace(ServerProperties properties, WorkspaceLock lock) {
        ServerProperties.Target target = properties.target();
        return workspace(GitWorkspaceProgram.Role.TARGET, properties, target.url(), target.branch(), target.token());
    }

    private static GitWorkspace workspace(GitWorkspaceProgram.Role role, ServerProperties properties, String url,
            String branch, Secret token) {
        GitCredentials credentials = token == null ? GitCredentials.none() : GitCredentials.httpsToken(token.value());
        return new GitWorkspaceProgram(Path.of(properties.workspace()), role,
                new GitWorkspaceProgram.Remote(url, branch, credentials),
                new GitWorkspaceProgram.Author(properties.git().userName(), properties.git().userEmail()),
                new GitWorkspaceProgram.Budgets(LOCAL_BUDGET,
                        Duration.ofSeconds(properties.git().networkTimeoutSeconds())));
    }

    @Bean
    DeployUseCase deployUseCase(@Qualifier("sourceWorkspace") GitWorkspace source,
            @Qualifier("targetWorkspace") GitWorkspace target, PipelineUseCase pipeline, Mb2eProperties properties) {
        return new DeployService(source, target, pipeline, properties.commitMessage(), ATTEMPTS);
    }

    @Bean
    DeploymentHistory deploymentHistory(ServerProperties properties, WorkspaceLock lock) {
        return new SqliteDeploymentHistory(Path.of(properties.workspace(), HISTORY_FILE),
                DashboardApiController.MAX_LIMIT);
    }

    @Bean
    DeployQueue deployQueue(DeployUseCase deploy, DeploymentHistory history, StepLog log) {
        return new DeployQueue(deploy, history, log, SHUTDOWN_WAIT);
    }

    @Bean
    WebhookSettings webhookSettings(ServerProperties properties) {
        return new WebhookSettings(properties.webhook().secret().value(), properties.webhook().maxBodyBytes(),
                properties.source().repository(), properties.source().branch());
    }

    @Bean
    GithubWebhookController githubWebhookController(DeployQueue queue, WebhookSettings settings, StepLog log) {
        return new GithubWebhookController(queue, settings, log);
    }

    @Bean
    HealthController healthController(DeployQueue queue) {
        return new HealthController(queue);
    }

    @Bean
    RouterFunction<ServerResponse> webRoutes(GithubWebhookController webhook, HealthController health) {
        return WebRoutes.of(webhook, health);
    }

    /**
     * Logs the start with the settings that explain the later lines, then runs one deployment, so a push that
     * arrived while the service was down is caught up.
     */
    @Bean
    ApplicationRunner startupDeployment(DeployQueue queue, ServerProperties properties, StepLog log,
            Environment environment) {
        return arguments -> {
            log.start("port=" + environment.getProperty("local.server.port", "-")
                    + " source=" + properties.source().repository() + "@" + properties.source().branch()
                    + " target=" + properties.target().repository() + "@" + properties.target().branch()
                    + " syncOnStart=" + properties.syncOnStart() + " pollSeconds=" + properties.pollIntervalSeconds()
                    + " dashboard=" + (properties.dashboard().enabled() ? "on" : "off"));
            if (properties.syncOnStart()) {
                queue.submit("startup");
            }
        };
    }

    /** Exists only when a poll interval is set; with 0 no scheduled task exists. */
    @Bean
    @ConditionalOnExpression("${mb2e.server.poll-interval-seconds:0} > 0")
    PollScheduler pollScheduler(DeployQueue queue, ServerProperties properties) {
        return new PollScheduler(queue, Duration.ofSeconds(properties.pollIntervalSeconds()));
    }
}
