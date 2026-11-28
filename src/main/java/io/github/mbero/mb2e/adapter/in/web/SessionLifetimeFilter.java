package io.github.mbero.mb2e.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends a session older than the total limit, whatever its activity; the container ends an idle one.
 * It runs before the security context is read, so the request then counts as signed out.
 */
public class SessionLifetimeFilter extends OncePerRequestFilter {

    private final Duration maxAge;
    private final Clock clock;

    public SessionLifetimeFilter(Duration maxAge, Clock clock) {
        this.maxAge = maxAge;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && clock.millis() - session.getCreationTime() > maxAge.toMillis()) {
            session.invalidate();
        }
        chain.doFilter(request, response);
    }
}
