package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OwnerOnlyUserServiceTest {

    private static OAuth2User fromGithub(String login) {
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of("login", login, "id", 7, "email", "me@example.com"), "login");
    }

    @Test
    void theAllowedLoginInAnyCaseSignsInAndKeepsOnlyItsLogin() {
        OAuth2User user = new OwnerOnlyUserService(request -> fromGithub("Me"), "me").loadUser(null);
        assertThat(user.getName()).isEqualTo("Me");
        assertThat(user.getAttributes()).containsOnlyKeys("login");
    }

    @Test
    void anotherLoginIsRefusedAndTheRefusalNamesIt() {
        assertThatThrownBy(() -> new OwnerOnlyUserService(request -> fromGithub("intruder"), "me").loadUser(null))
                .isInstanceOfSatisfying(OwnerOnlyUserService.NotAllowed.class,
                        refused -> assertThat(refused.login()).isEqualTo("intruder"));
    }

    @Test
    void aProfileWithoutALoginIsRefused() {
        assertThatThrownBy(() -> new OwnerOnlyUserService(request -> new DefaultOAuth2User(List.of(),
                Map.of("id", 7), "id"), "me").loadUser(null)).isInstanceOf(OwnerOnlyUserService.NotAllowed.class);
    }
}
