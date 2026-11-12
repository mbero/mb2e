package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Vault;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileVaultReaderTest {

    @TempDir
    Path temp;

    @Test
    void housekeepingDirectoriesAndSymbolicLinksAreNotWalked() throws Exception {
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.writeString(outside.resolve("Secret.md"), "---\ntags: [t]\n---\n");
        Path vault = Files.createDirectories(temp.resolve("vault"));
        Files.createDirectories(vault.resolve(".trash"));
        Files.writeString(vault.resolve(".trash/Gone.md"), "x");
        Files.createDirectories(vault.resolve("node_modules"));
        Files.writeString(vault.resolve("node_modules/Dep.md"), "x");
        Files.writeString(vault.resolve(".hidden.md"), "x");
        Files.writeString(vault.resolve(".hidden.png"), "x");
        Files.writeString(vault.resolve("Real.md"), "x");
        Files.writeString(vault.resolve("pic.png"), "x");
        Files.createSymbolicLink(vault.resolve("Link"), outside);

        Vault read = new FileVaultReader(vault).read();

        assertThat(read.notes()).extracting(n -> n.path()).containsExactly("Real.md");
        assertThat(read.files()).containsExactly("pic.png");
    }

    @Test
    void aMissingPathAndAFileAreToldApart() throws Exception {
        assertThat(new FileVaultReader(temp.resolve("nope")).read().status()).isEqualTo(Vault.Status.MISSING);
        Path file = Files.writeString(temp.resolve("f"), "x");
        assertThat(new FileVaultReader(file).read().status()).isEqualTo(Vault.Status.NOT_A_DIRECTORY);
    }
}
