package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.port.out.PublishTarget;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Working tree of the publication target. It runs no process and knows nothing of git.
 * <p>The only deletes are single files below {@code assets} and the whole datasource
 * directory. A path that leaves the target is refused, and so is a write or delete
 * that would pass through a symbolic link inside the target.
 */
public class FilePublishTarget implements PublishTarget {

    private final String configured;
    private final Path template;
    private final Path datasource;

    public FilePublishTarget(String configured, Path template, Path datasource) {
        this.configured = configured;
        this.template = template;
        this.datasource = datasource;
    }

    @Override
    public String location() {
        return configured;
    }

    @Override
    public Kind inspect() {
        if (configured == null || configured.isBlank()) {
            return Kind.MISSING;
        }
        Path path = resolved();
        if (path.getParent() == null) {
            return Kind.FILESYSTEM_ROOT;
        }
        if (sameDirectory(path, template)) {
            return Kind.TEMPLATE_DIRECTORY;
        }
        if (!Files.exists(path)) {
            return Kind.MISSING;
        }
        if (!Files.isDirectory(path)) {
            return Kind.NOT_A_DIRECTORY;
        }
        return Files.exists(path.resolve(".git")) ? Kind.USABLE : Kind.NOT_A_GIT_WORKING_COPY;
    }

    @Override
    public Optional<String> overlap() {
        if (configured == null || configured.isBlank()) {
            return Optional.empty();
        }
        Path target = realOrNormalized(Path.of(configured));
        Path source = realOrNormalized(template);
        if (target.startsWith(source)) {
            return Optional.of("The publication target '" + target + "' is inside the template directory '"
                    + source + "'.");
        }
        if (source.startsWith(target)) {
            return Optional.of("The template directory '" + source + "' is the publication target '" + target
                    + "' or inside it.");
        }
        Path data = datasource == null ? null : realOrNormalized(datasource);
        if (data != null && data.startsWith(target)) {
            return Optional.of("The datasource '" + data + "' is the publication target '" + target
                    + "' or inside it.");
        }
        return Optional.empty();
    }

    @Override
    public boolean sameFile(String first, String second) {
        Path a = inside(first);
        Path b = inside(second);
        try {
            return Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
        } catch (IOException failure) {
            return false;
        }
    }

    @Override
    public void verifyWritable(String path) {
        refuseLinks(inside(path), true);
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(inside(path));
    }

    @Override
    public boolean hasContent(String path, byte[] content) {
        Path file = inside(path);
        try {
            return Files.isRegularFile(file) && Files.size(file) == content.length
                    && Arrays.equals(Files.readAllBytes(file), content);
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read " + file + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public List<String> files(String directory) {
        return FileTree.files(root(), directory);
    }

    @Override
    public void write(String path, byte[] content) {
        Path file = inside(path);
        refuseLinks(file, true);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content);
        } catch (IOException failure) {
            throw new StoreFailure("Cannot write " + file + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public void delete(String path) {
        Path file = deletable(path);
        refuseLinks(file, false);
        try {
            Files.deleteIfExists(file);
            Path assets = root().resolve("assets");
            for (Path parent = file.getParent(); parent != null && parent.startsWith(assets)
                    && !parent.equals(assets) && isEmptyDirectory(parent); parent = parent.getParent()) {
                Files.delete(parent);
            }
        } catch (IOException failure) {
            throw new StoreFailure("Cannot delete " + file + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public void deleteTree(String directory) {
        Path start = deletable(directory);
        refuseLinks(start, false);
        if (!directory.equals("datasource")) {
            throw new StoreFailure("Refusing to delete a directory tree other than datasource: " + start, null);
        }
        if (!Files.isDirectory(start)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(start)) {
            for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException failure) {
            throw new StoreFailure("Cannot delete " + start + ": " + failure.getMessage(), failure);
        }
    }

    // ---------- bounds ----------

    private Path resolved() {
        return realOrNormalized(Path.of(configured));
    }

    /** The real path where the path exists; otherwise the real path of its nearest existing parent plus the rest. */
    private static Path realOrNormalized(Path given) {
        Path path = given.toAbsolutePath().normalize();
        Path existing = path;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return path;
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(path)).normalize();
        } catch (IOException failure) {
            return path;
        }
    }

    /** Refuses a path with a symbolic link on the way from the target root, the last name included on request. */
    private void refuseLinks(Path file, boolean includingLast) {
        Path relative = root().relativize(file);
        Path current = root();
        for (int i = 0; i < relative.getNameCount(); i++) {
            current = current.resolve(relative.getName(i));
            boolean last = i == relative.getNameCount() - 1;
            if ((includingLast || !last) && Files.isSymbolicLink(current)) {
                throw new StoreFailure("Refusing to go through the symbolic link " + current
                        + " inside the publication target; replace it with a real directory or file.", null);
            }
        }
    }

    private Path root() {
        return resolved();
    }

    private static boolean sameDirectory(Path a, Path b) {
        try {
            if (Files.exists(a) && Files.exists(b)) {
                return Files.isSameFile(a, b);
            }
        } catch (IOException ignored) {
            // fall back to the path text
        }
        return a.equals(b.toAbsolutePath().normalize());
    }

    /** A path inside the target, never the target itself and never inside .git. */
    private Path inside(String relative) {
        Path root = root();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root) || path.startsWith(root.resolve(".git"))) {
            throw new StoreFailure("Refusing a path outside the publication target: " + path, null);
        }
        return path;
    }

    private Path deletable(String relative) {
        Path path = inside(relative);
        Path rest = root().relativize(path);
        String first = rest.getName(0).toString();
        if (!first.equals("assets") && !first.equals("datasource")) {
            throw new StoreFailure("Refusing to delete outside assets and datasource: " + path, null);
        }
        return path;
    }

    private static boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isEmpty();
        }
    }
}
