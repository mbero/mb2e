package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.adapter.in.shell.BuildCommand;
import io.github.mbero.mb2e.adapter.in.shell.PublishCommand;
import io.github.mbero.mb2e.adapter.in.shell.RunCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** In the server mode the shell commands do not exist; the shell runners stay off, as in the CLI tests. */
@SpringBootTest(properties = {
        "mb2e.mode=server",
        "spring.shell.interactive.enabled=false",
        "spring.shell.noninteractive.enabled=false",
        "spring.shell.script.enabled=false"})
class Mb2eServerModeTest {

    @Autowired
    ApplicationContext context;

    @Test
    void noShellCommandBeanExists() {
        assertThat(context.getBeansOfType(RunCommand.class)).isEmpty();
        assertThat(context.getBeansOfType(BuildCommand.class)).isEmpty();
        assertThat(context.getBeansOfType(PublishCommand.class)).isEmpty();
    }
}
