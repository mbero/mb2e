package io.github.mbero.mb2e.adapter.in.web;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Accepts only the configured GitHub login, compared without case. The signed-in user keeps the login and
 * nothing else from the GitHub profile, so the session holds no token and no personal data.
 */
public class OwnerOnlyUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    static final String LOGIN = "login";

    /** A GitHub account that is not the allowed one; it carries the login for the access-denied page. */
    public static final class NotAllowed extends OAuth2AuthenticationException {
        private final String login;

        NotAllowed(String login) {
            super(new OAuth2Error("access_denied", "This GitHub account has no access.", null));
            this.login = login;
        }

        public String login() {
            return login;
        }
    }

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> github;
    private final String allowedLogin;

    public OwnerOnlyUserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> github, String allowedLogin) {
        this.github = github;
        this.allowedLogin = allowedLogin;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        Object value = github.loadUser(request).getAttributes().get(LOGIN);
        String login = value == null ? "" : value.toString();
        if (!login.equalsIgnoreCase(allowedLogin)) {
            throw new NotAllowed(login);
        }
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_OWNER")), Map.of(LOGIN, login), LOGIN);
    }
}
