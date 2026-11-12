package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads the template directory. It never writes. */
public class FileSiteSource implements SiteSource {

    private final Path root;

    public FileSiteSource(Path root) {
        this.root = root;
    }

    @Override
    public String location() {
        return root.toString();
    }

    @Override
    public boolean isFile(String path) {
        return Files.isRegularFile(root.resolve(path));
    }

    @Override
    public boolean isDirectory(String path) {
        return Files.isDirectory(root.resolve(path));
    }

    @Override
    public byte[] read(String path) {
        try {
            return Files.readAllBytes(root.resolve(path));
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read " + root.resolve(path) + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public List<String> files(String directory) {
        return FileTree.files(root, directory);
    }
}
