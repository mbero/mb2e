package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ServerEnvironmentTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static Map<String, String> complete() {
        Map<String, String> env = new HashMap<>();
        env.put("MB2E_SOURCE_REPO_URL", "https://github.com/me/notes");
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/me/me.github.io.git");
        env.put("MB2E_WEBHOOK_SECRET", SECRET);
        env.put("MB2E_GITHUB_TOKEN", "ghp_token");
        env.put("MB2E_GIT_USER_NAME", "Mb2e Bot");
        env.put("MB2E_GIT_USER_EMAIL", "bot@example.com");
        return env;
    }

    private static Map<String, Object> properties(Map<String, String> env) {
        return ServerEnvironment.properties(env, path -> {
            throw new IllegalStateException("no file expected");
        });
    }

    @Test
    void aCompleteEnvironmentGivesTheDerivedPropertiesAndTheDefaults() {
        Map<String, Object> p = properties(complete());
        assertThat(p).containsEntry("mb2e.vault.path", "/var/lib/mb2e/source")
                .containsEntry("mb2e.target.path", "/var/lib/mb2e/target")
                .containsEntry("mb2e.template.path", "/opt/mb2e/template/mbs-blog")
                .containsEntry("server.port", 8080L)
                .containsEntry("mb2e.server.source.branch", "main")
                .containsEntry("mb2e.server.git.network-timeout-seconds", 120L)
                .containsEntry("mb2e.server.sync-on-start", true)
                .containsEntry("mb2e.server.poll-interval-seconds", 0L)
                .containsEntry("mb2e.tag", "blog-ready")
                .containsEntry("mb2e.commit-message", "chore(content): refresh Knowledge Hub");
    }

    @Test
    void eachRequiredVariableIsNamedWhenItIsMissing() {
        for (String name : new String[] {"MB2E_SOURCE_REPO_URL", "MB2E_TARGET_REPO_URL", "MB2E_WEBHOOK_SECRET",
                "MB2E_GIT_USER_NAME", "MB2E_GIT_USER_EMAIL"}) {
            Map<String, String> env = complete();
            env.remove(name);
            assertThatThrownBy(() -> properties(env)).isInstanceOf(Mb2eEnvironment.Refused.class)
                    .hasMessageContaining(name);
        }
        Map<String, String> noToken = complete();
        noToken.remove("MB2E_GITHUB_TOKEN");
        assertThatThrownBy(() -> properties(noToken)).hasMessageContaining("MB2E_SOURCE_TOKEN");
        noToken.put("MB2E_SOURCE_TOKEN", "s");
        assertThatThrownBy(() -> properties(noToken)).hasMessageContaining("MB2E_TARGET_TOKEN");
    }

    @Test
    void aVariableAndItsFileTwinTogetherAreRefusedByName() {
        Map<String, String> env = complete();
        env.put("MB2E_WEBHOOK_SECRET_FILE", "/run/secrets/hook");
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_WEBHOOK_SECRET")
                .hasMessageContaining("MB2E_WEBHOOK_SECRET_FILE");
    }

    @Test
    void aSecretFileIsReadAndStripped() {
        Map<String, String> env = complete();
        env.remove("MB2E_WEBHOOK_SECRET");
        env.put("MB2E_WEBHOOK_SECRET_FILE", "/run/secrets/hook");
        Function<Path, String> reader = path -> path.toString().equals("/run/secrets/hook") ? SECRET + "\n" : null;
        assertThat(ServerEnvironment.properties(env, reader)).containsEntry("mb2e.server.webhook.secret", SECRET);
        assertThatThrownBy(() -> ServerEnvironment.properties(env, path -> {
            throw new IllegalStateException(SECRET);
        })).hasMessageContaining("MB2E_WEBHOOK_SECRET_FILE").hasMessageNotContaining(SECRET);
        assertThatThrownBy(() -> ServerEnvironment.properties(env, path -> "  \n"))
                .hasMessageContaining("MB2E_WEBHOOK_SECRET_FILE is empty");
    }

    @Test
    void theGithubTokenFillsBothAndTheSpecificTokenWins() {
        Map<String, Object> both = properties(complete());
        assertThat(both).containsEntry("mb2e.server.source.token", "ghp_token")
                .containsEntry("mb2e.server.target.token", "ghp_token");
        Map<String, String> env = complete();
        env.put("MB2E_SOURCE_TOKEN", "source-only");
        Map<String, Object> mixed = properties(env);
        assertThat(mixed).containsEntry("mb2e.server.source.token", "source-only")
                .containsEntry("mb2e.server.target.token", "ghp_token");
    }

    @Test
    void equalRepositoriesAreRefusedWhateverTheCaseOrSuffix() {
        Map<String, String> env = complete();
        env.put("MB2E_TARGET_REPO_URL", "https://github.com/ME/Notes.git");
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_SOURCE_REPO_URL")
                .hasMessageContaining("MB2E_TARGET_REPO_URL");
    }

    @Test
    void aUrlWithUserInformationOrAnotherHostIsRefusedWithoutPrintingIt() {
        for (String url : new String[] {"https://user:pw@github.com/o/r", "http://github.com/o/r",
                "git@github.com:o/r.git", "https://example.com/o/r", "https://github.com/o"}) {
            Map<String, String> env = complete();
            env.put("MB2E_SOURCE_REPO_URL", url);
            assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_SOURCE_REPO_URL")
                    .hasMessageNotContaining("user").hasMessageNotContaining("pw")
                    .hasMessageNotContaining("example.com");
        }
    }

    @Test
    void aSubdirectoryMustBeRelativeAndStayInsideTheRepository() {
        for (String bad : new String[] {"../x", "/x", "a/../b", "a\\b"}) {
            Map<String, String> env = complete();
            env.put("MB2E_SOURCE_SUBDIR", bad);
            assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_SOURCE_SUBDIR");
        }
        Map<String, String> env = complete();
        env.put("MB2E_SOURCE_SUBDIR", "vault/notes/");
        assertThat(properties(env)).containsEntry("mb2e.vault.path", "/var/lib/mb2e/source/vault/notes")
                .containsEntry("mb2e.server.source.subdir", "vault/notes");
    }

    @Test
    void aBranchMustBeANameAndNeverAnOptionOrAFullReference() {
        for (String bad : new String[] {"-x", "--upload-pack=x", "refs/heads/main", "a..b", "main.lock", "a b", "x/"}) {
            Map<String, String> env = complete();
            env.put("MB2E_TARGET_BRANCH", bad);
            assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_TARGET_BRANCH");
        }
        Map<String, String> env = complete();
        env.put("MB2E_TARGET_BRANCH", "gh-pages");
        assertThat(properties(env)).containsEntry("mb2e.server.target.branch", "gh-pages");
    }

    @Test
    void aShortWebhookSecretIsRefusedWithoutPrintingIt() {
        Map<String, String> env = complete();
        env.put("MB2E_WEBHOOK_SECRET", SECRET.substring(1));
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_WEBHOOK_SECRET")
                .hasMessageNotContaining(SECRET.substring(1));
    }

    @Test
    void numbersAndFlagsAreRangeChecked() {
        Map<String, String> bad = new HashMap<>();
        bad.put("MB2E_HTTP_PORT", "0");
        bad.put("MB2E_GIT_NETWORK_TIMEOUT_SECONDS", "9");
        bad.put("MB2E_POLL_INTERVAL_SECONDS", "59");
        bad.put("MB2E_SYNC_ON_START", "yes");
        bad.put("MB2E_FORCE", "1");
        bad.put("MB2E_WEBHOOK_MAX_BODY_BYTES", "-1");
        bad.put("MB2E_COMMIT_MESSAGE", "two\nlines");
        for (Map.Entry<String, String> entry : bad.entrySet()) {
            Map<String, String> env = complete();
            env.put(entry.getKey(), entry.getValue());
            assertThatThrownBy(() -> properties(env)).hasMessageContaining(entry.getKey());
        }
        Map<String, String> good = complete();
        good.put("MB2E_HTTP_PORT", "9999");
        good.put("MB2E_POLL_INTERVAL_SECONDS", "60");
        good.put("MB2E_SYNC_ON_START", "false");
        assertThat(properties(good)).containsEntry("server.port", 9999L)
                .containsEntry("mb2e.server.poll-interval-seconds", 60L)
                .containsEntry("mb2e.server.sync-on-start", false);
    }

    @Test
    void thePathsMustBeAbsoluteAndTheTemplateMustStayOutsideTheWorkspace() {
        Map<String, String> env = complete();
        env.put("MB2E_WORKSPACE", "relative/dir");
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_WORKSPACE");
        env.put("MB2E_WORKSPACE", "/");
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_WORKSPACE");
        env.put("MB2E_WORKSPACE", "/data");
        env.put("MB2E_TEMPLATE_PATH", "/data/template");
        assertThatThrownBy(() -> properties(env)).hasMessageContaining("MB2E_TEMPLATE_PATH");
    }

    @Test
    void onlyTheListedVariablesAreReadSoServerPortHasNoEffect() {
        Map<String, String> env = complete();
        env.put("SERVER_PORT", "9999");
        env.put("MB2E_UNKNOWN", "x");
        env.put("SPRING_PROFILES_ACTIVE", "evil");
        assertThat(properties(env)).containsEntry("server.port", 8080L).doesNotContainKey("spring.profiles.active");
    }
}
