package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.model.Bundle;
import io.github.mbero.mb2e.domain.port.out.BundleStore;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes {@code assets/js/content.js} inside the template directory and nothing else.
 * <p>The content goes to a sibling temporary file first and replaces the old file in one
 * move, so a failed write leaves the old bundle as it was.
 */
public class FileBundleStore implements BundleStore {

    private static final String RELATIVE_PATH = "assets/js/content.js";

    private final Path templateRoot;

    public FileBundleStore(Path templateRoot) {
        this.templateRoot = templateRoot;
    }

    @Override
    public String serialize(Bundle bundle) {
        return BundleJson.file(bundle);
    }

    @Override
    public long write(Bundle bundle, boolean dryRun) {
        byte[] content = BundleJson.file(bundle).getBytes(StandardCharsets.UTF_8);
        if (dryRun) {
            return content.length;
        }
        Path target = templateRoot.resolve(RELATIVE_PATH);
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".content-", ".tmp");
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return content.length;
        } catch (IOException failure) {
            throw new StoreFailure("Cannot write " + target + ": " + failure.getMessage(), failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // the temporary file is a leftover, not a result
                }
            }
        }
    }
}
