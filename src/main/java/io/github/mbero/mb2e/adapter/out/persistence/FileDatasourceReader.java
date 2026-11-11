package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.model.Datasource;
import io.github.mbero.mb2e.domain.model.SourceNote;
import io.github.mbero.mb2e.domain.port.out.DatasourceReader;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Reads the datasource directory: every directory, every .md file with its text, every other file. */
public class FileDatasourceReader implements DatasourceReader {

    private final Path root;

    public FileDatasourceReader(Path root) {
        this.root = root;
    }

    @Override
    public Datasource read() {
        List<String> directories = new ArrayList<>();
        List<SourceNote> notes = new ArrayList<>();
        List<String> attachments = new ArrayList<>();
        if (Files.isDirectory(root)) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path path : (Iterable<Path>) walk.sorted()::iterator) {
                    if (path.equals(root)) {
                        continue;
                    }
                    String relative = root.relativize(path).toString().replace('\\', '/');
                    if (hidden(relative)) {
                        continue;
                    }
                    if (Files.isDirectory(path)) {
                        directories.add(relative);
                    } else if (relative.toLowerCase(Locale.ROOT).endsWith(".md")) {
                        notes.add(new SourceNote(relative, Files.readString(path, StandardCharsets.UTF_8)));
                    } else {
                        attachments.add(relative);
                    }
                }
            } catch (IOException | java.io.UncheckedIOException failure) {
                throw new StoreFailure("Cannot read the datasource " + root + ": " + failure.getMessage(), failure);
            }
        }
        return new Datasource(root.toString(), directories, notes, attachments);
    }

    /** Returns whether any segment of the path begins with a dot; such entries are neither scanned nor read. */
    private static boolean hidden(String relative) {
        for (String segment : relative.split("/")) {
            if (segment.startsWith(".")) {
                return true;
            }
        }
        return false;
    }
}
