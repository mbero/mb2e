package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.port.out.DatasourceMirror;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Datasource access for a sync: compares and writes files under the configured root.
 * <p>It deliberately has no operation that removes a file or a directory.
 */
public class FileDatasourceMirror implements DatasourceMirror {

    private final Path root;

    public FileDatasourceMirror(Path root) {
        this.root = root;
    }

    @Override
    public Presence compare(String path, byte[] content) {
        Path target = root.resolve(path);
        try {
            if (!Files.isRegularFile(target)) {
                return Presence.ABSENT;
            }
            if (Files.size(target) != content.length) {
                return Presence.DIFFERENT;
            }
            return Arrays.equals(Files.readAllBytes(target), content) ? Presence.SAME : Presence.DIFFERENT;
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read " + target + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public void write(String path, byte[] content) {
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root.normalize())) {
            throw new StoreFailure("Refusing to write outside the datasource: " + target, null);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException failure) {
            throw new StoreFailure("Cannot write " + target + ": " + failure.getMessage(), failure);
        }
    }
}
