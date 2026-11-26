package io.github.mbero.mb2e.adapter.in.web;

/**
 * What the webhook endpoint needs: the shared secret, the body limit, and the repository and branch whose
 * push counts. {@code repository} is {@code owner/name}. The text form hides the secret.
 */
public record WebhookSettings(String secret, long maxBodyBytes, String repository, String branch) {

    @Override
    public String toString() {
        return "WebhookSettings[repository=" + repository + ", branch=" + branch + ", secret=***]";
    }
}
