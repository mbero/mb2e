package io.github.mbero.mb2e.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitCredentialsTest {

    @Test
    void aTokenBecomesABasicAuthorizationHeaderForGithubOnly() {
        GitCredentials credentials = GitCredentials.httpsToken("abc");
        assertThat(credentials.environment()).containsEntry("GIT_CONFIG_COUNT", "1")
                .containsEntry("GIT_CONFIG_KEY_0", "http.https://github.com/.extraheader")
                .containsEntry("GIT_CONFIG_VALUE_0", "AUTHORIZATION: basic eC1hY2Nlc3MtdG9rZW46YWJj")
                .containsEntry("GIT_CONFIG_NOSYSTEM", "1")
                .containsEntry("GIT_CONFIG_GLOBAL", "/dev/null");
        assertThat(credentials.secrets()).containsExactly("abc", "eC1hY2Nlc3MtdG9rZW46YWJj");
    }

    @Test
    void toStringShowsNeitherTheTokenNorItsEncoding() {
        assertThat(GitCredentials.httpsToken("abc").toString()).doesNotContain("abc", "eC1hY2Nlc3MtdG9rZW46YWJj");
    }

    @Test
    void noneHasNoEnvironmentAndNoSecret() {
        assertThat(GitCredentials.none().environment()).isEmpty();
        assertThat(GitCredentials.none().secrets()).isEmpty();
        assertThat(GitCredentials.none().toString()).contains("none");
    }

    @Test
    void theTokenMarkerAppearsOnlyInTheCredentialsSource() throws Exception {
        try (var files = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java"))) {
            assertThat(files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> read(p).contains("x-access-token"))
                    .map(p -> p.getFileName().toString())).containsExactly("GitCredentials.java");
        }
    }

    private static String read(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.readString(path);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }
}
