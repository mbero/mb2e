package io.github.mbero.mb2e.adapter.in.web;

import io.github.mbero.mb2e.adapter.in.log.StepLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Every answer of the login flow: redirects back into the dashboard after GitHub, short JSON errors for the
 * API, and one {@code step=login} log line for each event. No answer holds a stack trace.
 */
public class LoginAnswers implements AuthenticationSuccessHandler, AuthenticationFailureHandler,
        LogoutSuccessHandler, AuthenticationEntryPoint, AccessDeniedHandler {

    static final String HOME = "/dashboard/";
    static final String SIGN_IN = "/dashboard/sign-in";
    static final String DENIED = "/dashboard/denied";

    private final StepLog log;

    public LoginAnswers(StepLog log) {
        this.log = log;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        log.login("success", authentication.getName());
        response.sendRedirect(HOME);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failure) throws IOException {
        if (failure instanceof OwnerOnlyUserService.NotAllowed notAllowed) {
            log.login("denied", notAllowed.login());
            response.sendRedirect(DENIED + "?login=" + URLEncoder.encode(notAllowed.login(), StandardCharsets.UTF_8));
            return;
        }
        log.login("failed", null);
        response.sendRedirect(SIGN_IN + "?error");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        log.login("logout", authentication == null ? null : authentication.getName());
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    /** An API call without a valid session. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        json(response, HttpStatus.UNAUTHORIZED, "Sign in first.");
    }

    /** A request that changes state without a valid CSRF token. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException failure)
            throws IOException {
        json(response, HttpStatus.FORBIDDEN, "The request has no valid CSRF token.");
    }

    private static void json(HttpServletResponse response, HttpStatus status, String error) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
