package io.github.mbero.mb2e.domain.model;

import java.util.List;

/**
 * Result of a vault scan: the configured path, whether it can be scanned, every Markdown note
 * with its bytes, and the relative path of every other file (the attachment candidates).
 * Housekeeping directories and symbolic links never appear here.
 */
public record Vault(String path, Status status, List<VaultNote> notes, List<String> files) {

    public enum Status { OK, MISSING, NOT_A_DIRECTORY }

    public Vault {
        notes = List.copyOf(notes);
        files = List.copyOf(files);
    }

    public static Vault unusable(String path, Status status) {
        return new Vault(path, status, List.of(), List.of());
    }
}
