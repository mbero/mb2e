package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.port.out.DatasourceFiles;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Reads single files of the datasource. A path that leaves the datasource root reads as no file. */
public class FileDatasourceFiles implements DatasourceFiles {

    private final Path root;

    public FileDatasourceFiles(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public Optional<byte[]> read(String path) {
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root) || file.equals(root) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read " + file + ": " + failure.getMessage(), failure);
        }
    }
}
