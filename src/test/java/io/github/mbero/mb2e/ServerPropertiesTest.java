package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.infrastructure.config.ServerProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;

class ServerPropertiesTest {

    private static Map<String, String> environment() {
        Map<String, String> env = new HashMap<>();
        env.put("MB2E_SOURCE_REPO_URL", "https://github.com/me/notes");
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/me/me.github.io");
        env.put("MB2E_WEBHOOK_SECRET", "0123456789abcdef0123456789abcdef");
        env.put("MB2E_SOURCE_TOKEN", "source-token-value");
        env.put("MB2E_TARGET_TOKEN", "target-token-value");
        env.put("MB2E_GIT_USER_NAME", "Bot");
        env.put("MB2E_GIT_USER_EMAIL", "bot@example.com");
        return env;
    }

    private static ServerProperties bound() {
        Map<String, Object> map = ServerEnvironment.properties(environment(), path -> null);
        Mb2eEnvironment server = Mb2eEnvironment.forServer(null, map);
        return Binder.get(server).bind("mb2e.server", ServerProperties.class).get();
    }

    @Test
    void theValidatedMapBindsToTheRecord() {
        ServerProperties p = bound();
        assertThat(p.source().url()).isEqualTo("https://github.com/me/notes");
        assertThat(p.target().branch()).isEqualTo("main");
        assertThat(p.source().token().value()).isEqualTo("source-token-value");
        assertThat(p.webhook().secret().value()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(p.webhook().maxBodyBytes()).isEqualTo(26214400L);
        assertThat(p.git().networkTimeoutSeconds()).isEqualTo(120);
        assertThat(p.syncOnStart()).isTrue();
        assertThat(p.pollIntervalSeconds()).isZero();
    }

    @Test
    void noTextFormShowsASecret() {
        String text = bound().toString();
        assertThat(text).doesNotContain("source-token-value", "target-token-value",
                "0123456789abcdef0123456789abcdef").contains("***");
    }
}
