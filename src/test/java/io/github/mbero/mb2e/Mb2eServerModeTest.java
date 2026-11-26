package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.adapter.in.shell.BuildCommand;
import io.github.mbero.mb2e.adapter.in.shell.PublishCommand;
import io.github.mbero.mb2e.adapter.in.shell.RunCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** In the server mode the shell commands do not exist and the server beans do; the shell runners stay off. */
@SpringBootTest(properties = {
        "mb2e.mode=server",
        "mb2e.server.workspace=target/test-workspaces/server-mode-test",
        "mb2e.server.sync-on-start=false",
        "mb2e.server.source.url=https://github.com/me/notes",
        "mb2e.server.source.repository=me/notes",
        "mb2e.server.source.token=source-token",
        "mb2e.server.target.url=https://github.com/me/me.github.io",
        "mb2e.server.target.token=target-token",
        "mb2e.server.webhook.secret=0123456789abcdef0123456789abcdef",
        "mb2e.server.git.user-name=Bot",
        "mb2e.server.git.user-email=bot@example.com",
        "spring.shell.interactive.enabled=false",
        "spring.shell.noninteractive.enabled=false",
        "spring.shell.script.enabled=false"})
class Mb2eServerModeTest {

    @Autowired
    ApplicationContext context;

    @Test
    void theServerBeansExistAndTheyHideTheirSecrets() {
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.adapter.in.web.DeployQueue.class)).hasSize(1);
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.adapter.in.web.GithubWebhookController.class)).hasSize(1);
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.adapter.in.web.HealthController.class)).hasSize(1);
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.domain.port.out.GitWorkspace.class)).hasSize(2);
        assertThat(context.getBeansOfType(io.github.mbero.mb2e.adapter.in.web.PollScheduler.class)).isEmpty();
        assertThat(context.getBean(io.github.mbero.mb2e.infrastructure.config.ServerProperties.class).toString())
                .doesNotContain("source-token", "target-token", "0123456789abcdef");
    }

    @Test
    void buildConfigNeverReferencesAGitWorkspace() throws Exception {
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/mbero/mb2e/infrastructure/config/BuildConfig.java")))
                .doesNotContain("GitWorkspace");
    }

    @Test
    void noShellCommandBeanExists() {
        assertThat(context.getBeansOfType(RunCommand.class)).isEmpty();
        assertThat(context.getBeansOfType(BuildCommand.class)).isEmpty();
        assertThat(context.getBeansOfType(PublishCommand.class)).isEmpty();
    }
}
