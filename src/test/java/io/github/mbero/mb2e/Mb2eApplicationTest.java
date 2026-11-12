package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.command.CommandCatalog;

@SpringBootTest(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.shell.noninteractive.enabled=false",
        "spring.shell.script.enabled=false"})
class Mb2eApplicationTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    CommandCatalog catalog;

    /** Help, usage and exit, and the three working commands. */
    @Test
    void theShellOffersOnlyTheCommandsThatExist() {
        assertThat(catalog.getRegistrations().keySet()).containsExactlyInAnyOrder("help", "usage", "exit", "run", "build", "publish");
    }

    @Test
    void noFrameworkBuiltInIsRegistered() {
        assertThat(catalog.getRegistrations()).doesNotContainKeys(
                "history", "version", "script", "clear", "stacktrace", "completion", "quit");
    }

    /** --help and -h are the parameters of no command. */
    @Test
    void noCommandHasAHelpOption() {
        catalog.getRegistrations().forEach((name, registration) ->
                registration.getOptions().forEach(option -> {
                    assertThat(option.getLongNames()).as("%s long names", name).doesNotContain("help");
                    assertThat(option.getShortNames()).as("%s short names", name).doesNotContain('h');
                }));
    }

    @Test
    void onlyTheWorkingCommandsTakeParameters() {
        catalog.getRegistrations().forEach((name, registration) -> {
            if (name.equals("run")) {
                assertThat(registration.getOptions().stream().flatMap(o -> java.util.Arrays.stream(o.getLongNames())))
                        .containsExactlyInAnyOrder("json", "dry-run", "no-publish");
            } else if (name.equals("build") || name.equals("publish")) {
                assertThat(registration.getOptions().stream().flatMap(o -> java.util.Arrays.stream(o.getLongNames())))
                        .containsExactlyInAnyOrder("json", "dry-run");
            } else {
                assertThat(registration.getOptions()).as("%s options", name).isEmpty();
            }
        });
    }

    @Test
    void noEmbeddedServerStarts() {
        assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
    }
}
