package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class SessionLifetimeFilterTest {

    private static boolean survives(Duration age) throws Exception {
        MockHttpSession session = new MockHttpSession();
        Clock later = Clock.fixed(Instant.ofEpochMilli(session.getCreationTime()).plus(age), ZoneOffset.UTC);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockFilterChain chain = new MockFilterChain();
        new SessionLifetimeFilter(Duration.ofHours(12), later).doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isSameAs(request);
        return !session.isInvalid();
    }

    @Test
    void aSessionWithinTheTotalLimitStays() throws Exception {
        assertThat(survives(Duration.ofHours(11).plusMinutes(59))).isTrue();
    }

    @Test
    void aSessionOlderThanTheTotalLimitEndsWhateverItsActivity() throws Exception {
        assertThat(survives(Duration.ofHours(12).plusSeconds(1))).isFalse();
    }

    @Test
    void aRequestWithoutASessionPassesUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        new SessionLifetimeFilter(Duration.ofHours(12), Clock.systemUTC())
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(request.getSession(false)).isNull();
    }
}
