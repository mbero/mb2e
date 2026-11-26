package io.github.mbero.mb2e.infrastructure.config;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import io.github.mbero.mb2e.adapter.in.web.CsrfCookieFilter;
import io.github.mbero.mb2e.adapter.in.web.DashboardApiController;
import io.github.mbero.mb2e.adapter.in.web.DashboardSettings;
import io.github.mbero.mb2e.adapter.in.web.DeployQueue;
import io.github.mbero.mb2e.adapter.in.web.DiscardedClientRepository;
import io.github.mbero.mb2e.adapter.in.web.LoginAnswers;
import io.github.mbero.mb2e.adapter.in.web.OwnerOnlyUserService;
import io.github.mbero.mb2e.adapter.in.web.SessionLifetimeFilter;
import io.github.mbero.mb2e.adapter.in.web.WebRoutes;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * The dashboard of the server mode: its JSON API and the GitHub login that guards it. It exists only when
 * the three {@code MB2E_DASHBOARD_*} variables are set; without them the server starts no web security.
 * <p>The webhook and the health probe stay open: the webhook has its signature, the probe shows no detail.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${mb2e.mode:cli}' == 'server' and ${mb2e.server.dashboard.enabled:false}")
public class DashboardSecurityConfig {

    private static final String REGISTRATION = "github";
    private static final Duration SESSION_MAX_AGE = Duration.ofHours(12);

    @Bean
    DashboardApiController dashboardApiController(DeployQueue queue, DeploymentHistory history,
            ServerProperties properties, Mb2eProperties mb2e, StepLog log) {
        DashboardSettings settings = new DashboardSettings(properties.source().repository(),
                properties.target().repository(), ServerConfig.ATTEMPTS,
                Stream.of(properties.workspace(), mb2e.template().path()).filter(Objects::nonNull).toList());
        return new DashboardApiController(queue::state, history, settings, Clock.systemUTC(), log);
    }

    @Bean
    RouterFunction<ServerResponse> dashboardRoutes(DashboardApiController api) {
        return WebRoutes.dashboard(api);
    }

    /** The GitHub OAuth App with no scope: the public profile is enough to read the login. */
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(ServerProperties properties) {
        ServerProperties.Dashboard dashboard = properties.dashboard();
        // Built by hand: the Spring GitHub preset asks for read:user, and an empty scope list keeps that preset.
        ClientRegistration github = ClientRegistration.withRegistrationId(REGISTRATION)
                .clientId(dashboard.clientId())
                .clientSecret(dashboard.clientSecret().value())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .clientName("GitHub")
                .authorizationUri(dashboard.githubUrl() + "/login/oauth/authorize")
                .tokenUri(dashboard.githubUrl() + "/login/oauth/access_token")
                .userInfoUri(dashboard.githubApiUrl() + "/user")
                .userNameAttributeName("login")
                .build();
        return new InMemoryClientRegistrationRepository(github);
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new DiscardedClientRepository();
    }

    @Bean
    SecurityFilterChain dashboardSecurity(HttpSecurity http, ServerProperties properties, StepLog log)
            throws Exception {
        LoginAnswers answers = new LoginAnswers(log);
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/webhook/github").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        // The single-page app sends the plain cookie value back in the X-XSRF-TOKEN header.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/webhook/github"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(new SessionLifetimeFilter(SESSION_MAX_AGE, Clock.systemUTC()),
                        SecurityContextHolderFilter.class)
                .oauth2Login(login -> login
                        .loginPage("/dashboard/sign-in")
                        .userInfoEndpoint(user -> user.userService(new OwnerOnlyUserService(
                                new DefaultOAuth2UserService(), properties.dashboard().allowedLogin())))
                        .successHandler(answers)
                        .failureHandler(answers))
                .logout(logout -> logout.logoutUrl("/api/logout").logoutSuccessHandler(answers))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(answers).accessDeniedHandler(answers))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }
}
