package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.SyncReport;
import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.VaultNote;
import io.github.mbero.mb2e.domain.port.out.DatasourceMirror;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SyncServiceTest {

    /** An in-memory datasource that records what was written. */
    static class Memory implements DatasourceMirror {
        final Map<String, byte[]> files = new HashMap<>();
        final List<String> writes = new ArrayList<>();

        @Override
        public Presence compare(String path, byte[] content) {
            byte[] have = files.get(path);
            return have == null ? Presence.ABSENT : Arrays.equals(have, content) ? Presence.SAME : Presence.DIFFERENT;
        }

        @Override
        public void write(String path, byte[] content) {
            files.put(path, content);
            writes.add(path);
        }
    }

    private static VaultNote note(String path, String text) {
        return new VaultNote(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static VaultReader reader(Vault vault) {
        return new VaultReader() {
            @Override
            public Vault read() {
                return vault;
            }

            @Override
            public byte[] readFile(String path) {
                return path.getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    private static Vault vault(List<VaultNote> notes, String... files) {
        return new Vault("/vault", Vault.Status.OK, notes, List.of(files));
    }

    @Test
    void allThreeSpellingsOfTheTagSelect() {
        assertThat(SyncService.readFrontMatterTags("---\ntags: [a, \"#Blog-Ready\"]\n---\nx")).contains("blog-ready");
        assertThat(SyncService.readFrontMatterTags("---\ntags: a, blog-ready\n---\nx")).contains("blog-ready");
        assertThat(SyncService.readFrontMatterTags("---\ntags:\n  - a\n  - Blog-Ready\ntitle: t\n---\nx"))
                .contains("blog-ready");
        assertThat(SyncService.readFrontMatterTags("---\ntitle: blog-ready\n---\nx")).isEmpty();
        assertThat(SyncService.readFrontMatterTags("no front matter")).isEmpty();
    }

    @Test
    void anInlineTagIsHonouredOnlyOutsideTheFrontMatter() {
        assertThat(SyncService.hasInlineTag("---\nt: x\n---\ntext #Blog-Ready here", "blog-ready")).isTrue();
        assertThat(SyncService.hasInlineTag("---\nt: #blog-ready\n---\ntext", "blog-ready")).isFalse();
    }

    @Test
    void aSyncAddsSecondSyncChangesNothingAndNothingIsEverDeleted() {
        Memory mirror = new Memory();
        mirror.files.put("Old.md", new byte[] {1});
        Vault vault = vault(List.of(note("A/x.md", "---\ntags: [t]\n---\nbody ![[p.png]] [[Elsewhere]]"),
                note("y.md", "no tag")), "A/p.png");
        SyncService service = new SyncService(reader(vault), mirror, "t", false);

        SyncReport first = service.sync(false);
        assertThat(first.added()).containsExactly("A/x.md", "A/p.png");
        assertThat(first.attachments()).containsExactly("A/p.png");
        assertThat(first.brokenLinks()).containsExactly("A/x.md -> [[Elsewhere]]");
        assertThat(first.scannedCount()).isEqualTo(2);
        assertThat(first.deletedCount()).isZero();
        assertThat(mirror.files).containsKey("Old.md");

        mirror.writes.clear();
        SyncReport second = service.sync(false);
        assertThat(second.addedCount() + second.updatedCount()).isZero();
        assertThat(second.unchangedCount()).isEqualTo(2);
        assertThat(mirror.writes).isEmpty();
    }

    @Test
    void aDryRunWritesNothing() {
        Memory mirror = new Memory();
        Vault vault = vault(List.of(note("x.md", "---\ntags: [t]\n---\n")));
        SyncReport report = new SyncService(reader(vault), mirror, "t", false).sync(true);
        assertThat(report.addedCount()).isEqualTo(1);
        assertThat(mirror.writes).isEmpty();
    }

    @Test
    void anEmptySelectionAndAMissingVaultAreRefused() {
        Memory mirror = new Memory();
        SyncReport none = new SyncService(reader(vault(List.of(note("x.md", "plain")))), mirror, "t", false)
                .sync(false);
        assertThat(none.isRefused()).isTrue();
        assertThat(none.detail()).contains("'t'");
        SyncReport gone = new SyncService(reader(Vault.unusable("/nope", Vault.Status.MISSING)), mirror, "t", false)
                .sync(false);
        assertThat(gone.isRefused()).isTrue();
        assertThat(gone.vaultPath()).isEqualTo("/nope");
        assertThat(mirror.writes).isEmpty();
    }

    @Test
    void aMarkdownEmbedKeepsASpaceInItsPathAndDropsATitle() {
        Memory mirror = new Memory();
        Vault vault = vault(List.of(note("x.md",
                "---\ntags: [t]\n---\n![a](Img/no file.png) ![b](Img/other.png \"title\")")));
        SyncReport report = new SyncService(reader(vault), mirror, "t", false).sync(true);
        assertThat(report.missingAssets()).containsExactly("x.md -> Img/no file.png", "x.md -> Img/other.png");
    }
}
