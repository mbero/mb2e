package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/** The CLI environment starts no web security and no OAuth client, although both are on the class path. */
class CliSecurityIsolationTest {

    @Test
    void theCliContextHasNoSecurityFilterChainAndNoOauthClient() {
        Mb2eEnvironment environment = Mb2eEnvironment.create(null, "target/no-such-local-paths.yaml");
        // Only the shell runners are switched off, so the context starts without reading a terminal.
        environment.getPropertySources().addFirst(new MapPropertySource("test-no-shell", Map.of(
                "spring.shell.interactive.enabled", false, "spring.shell.noninteractive.enabled", false,
                "spring.shell.script.enabled", false)));
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Mb2eApplication.class)
                .environment(environment).bannerMode(Banner.Mode.OFF).run()) {
            assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
            assertThat(context.getBeanNamesForType(SecurityFilterChain.class)).isEmpty();
            assertThat(context.getBeanNamesForType(ClientRegistrationRepository.class)).isEmpty();
            assertThat(context.getBeanNamesForType(
                    org.springframework.security.core.userdetails.UserDetailsService.class)).isEmpty();
        }
    }

    @Test
    void theServerExcludesAllWebSecurityWithoutTheDashboardAndOnlyBootLoginBeansWithIt() {
        assertThat(Mb2eEnvironment.forServer(null, Map.of("mb2e.server.dashboard.enabled", false))
                .getProperty(Mb2eEnvironment.EXCLUDE)).isEqualTo(Mb2eEnvironment.SECURITY_AUTO_CONFIGURATION);
        assertThat(Mb2eEnvironment.forServer(null, Map.of("mb2e.server.dashboard.enabled", true))
                .getProperty(Mb2eEnvironment.EXCLUDE)).isEqualTo(Mb2eEnvironment.LOGIN_AUTO_CONFIGURATION)
                .doesNotContain("servlet.SecurityAutoConfiguration");
        assertThat(Mb2eEnvironment.create(null).getProperty(Mb2eEnvironment.EXCLUDE))
                .isEqualTo(Mb2eEnvironment.SECURITY_AUTO_CONFIGURATION);
    }
}
