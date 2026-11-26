package io.github.mbero.mb2e.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Checks the {@code X-Hub-Signature-256} header of a GitHub webhook delivery. */
public final class WebhookSignature {

    private static final Pattern FORMAT = Pattern.compile("^sha256=[0-9a-f]{64}$");

    private WebhookSignature() {
    }

    /**
     * Returns whether the header holds the HMAC-SHA256 of the raw body under the secret.
     * <p>Any malformed header is simply invalid. The digests are compared in constant time.
     */
    public static boolean isValid(String secret, byte[] body, String header) {
        if (header == null || !FORMAT.matcher(header).matches()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(body), decode(header.substring("sha256=".length())));
        } catch (GeneralSecurityException failure) {
            return false;
        }
    }

    private static byte[] decode(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
