package io.github.mbero.mb2e.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    private static final String SECRET = "It's a Secret to Everybody";
    private static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String VECTOR = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    @Test
    void theKnownVectorIsValid() {
        assertThat(WebhookSignature.isValid(SECRET, BODY, VECTOR)).isTrue();
    }

    @Test
    void aChangedBodyByteOrAWrongSecretIsInvalid() {
        assertThat(WebhookSignature.isValid(SECRET, "Hello, World?".getBytes(StandardCharsets.UTF_8), VECTOR)).isFalse();
        assertThat(WebhookSignature.isValid("another secret", BODY, VECTOR)).isFalse();
    }

    @Test
    void aMalformedHeaderIsInvalidAndNeverThrows() {
        String digest = VECTOR.substring("sha256=".length());
        String[] headers = {null, "", "sha256=", "sha1=" + digest, "sha256=" + digest.substring(1),
                "sha256=" + digest + "0", "sha256=" + "g" + digest.substring(1), "sha256=" + digest.toUpperCase(),
                " " + VECTOR, VECTOR + " ", "SHA256=" + digest};
        for (String header : headers) {
            assertThat(WebhookSignature.isValid(SECRET, BODY, header)).as("header %s", header).isFalse();
        }
    }

    @Test
    void theDigestIsComparedInConstantTimeAndNeverWithEquals() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/io/github/mbero/mb2e/adapter/in/web/WebhookSignature.java"));
        assertThat(source).contains("MessageDigest.isEqual").doesNotContain("equals(");
    }
}
