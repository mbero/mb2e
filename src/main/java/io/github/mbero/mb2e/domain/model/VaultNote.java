package io.github.mbero.mb2e.domain.model;

/** A Markdown file of the vault: its vault-relative path with "/" separators, and its bytes. */
public record VaultNote(String path, byte[] bytes) {
}
