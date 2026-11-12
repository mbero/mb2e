package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.VaultNote;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads the vault and never writes it.
 * <p>The walk does not follow symbolic links: a link is neither descended into nor read, so a
 * link out of the vault cannot add a note. Names that begin with a dot are skipped, and so is node_modules.
 */
public class FileVaultReader implements VaultReader {

    private static final String DEPENDENCIES = "node_modules";

    private final Path root;

    public FileVaultReader(Path root) {
        this.root = root;
    }

    @Override
    public Vault read() {
        String shown = root.toString();
        if (!Files.exists(root)) {
            return Vault.unusable(shown, Vault.Status.MISSING);
        }
        if (!Files.isDirectory(root)) {
            return Vault.unusable(shown, Vault.Status.NOT_A_DIRECTORY);
        }
        List<VaultNote> notes = new ArrayList<>();
        List<String> files = new ArrayList<>();
        walk(root, "", notes, files);
        return new Vault(shown, Vault.Status.OK, notes, files);
    }

    private void walk(Path dir, String relative, List<VaultNote> notes, List<String> files) {
        List<Path> entries;
        try (Stream<Path> list = Files.list(dir)) {
            entries = list.sorted().toList();
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read the vault directory " + dir + ": " + failure.getMessage(), failure);
        }
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            String rel = relative.isEmpty() ? name : relative + "/" + name;
            if (name.startsWith(".")) {
                continue;
            }
            if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                if (!name.equals(DEPENDENCIES)) {
                    walk(entry, rel, notes, files);
                }
            } else if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                if (name.toLowerCase(Locale.ROOT).endsWith(".md")) {
                    notes.add(new VaultNote(rel, bytes(entry)));
                } else {
                    files.add(rel);
                }
            }
        }
    }

    private static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException failure) {
            throw new StoreFailure("Cannot read " + file + ": " + failure.getMessage(), failure);
        }
    }

    @Override
    public byte[] readFile(String path) {
        return bytes(root.resolve(path));
    }
}
