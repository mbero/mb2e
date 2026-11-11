package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** A recursive listing of the files below a directory, as sorted relative paths with "/" separators. */
final class FileTree {

    private FileTree() {
    }

    static List<String> files(Path root, String directory) {
        Path start = root.resolve(directory);
        if (!Files.isDirectory(start, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(start)) {
            return walk.filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> root.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException failure) {
            throw new StoreFailure("Cannot read " + start + ": " + failure.getMessage(), failure);
        }
    }
}
