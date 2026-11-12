package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mbero.mb2e.domain.model.Assembly;
import io.github.mbero.mb2e.domain.model.BuildReport;
import io.github.mbero.mb2e.domain.model.ConfiguredPaths;
import io.github.mbero.mb2e.domain.model.Vault;
import io.github.mbero.mb2e.domain.model.VaultNote;
import io.github.mbero.mb2e.domain.port.out.SiteSource;
import io.github.mbero.mb2e.domain.port.out.VaultReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildServiceTest {

    private static final ConfiguredPaths PATHS = new ConfiguredPaths("/vault", "/template", null);

    private static VaultNote note(String path, String body, String... tags) {
        String front = "---\ntags: [" + String.join(", ", tags) + "]\n---\n";
        return new VaultNote(path, (front + body).getBytes(StandardCharsets.UTF_8));
    }

    private static VaultReader vaultOf(List<VaultNote> notes, String... files) {
        Vault vault = new Vault("/vault", Vault.Status.OK, notes, List.of(files));
        return new VaultReader() {
            @Override
            public Vault read() {
                return vault;
            }

            @Override
            public byte[] readFile(String path) {
                return new byte[0];
            }
        };
    }

    private static BuildService service(VaultReader vault, String defaultNote) {
        return new BuildService(vault, new CompleteTemplate(), PATHS, "t", false, defaultNote);
    }

    private static BuildService service(List<VaultNote> notes, String defaultNote, String... files) {
        return service(vaultOf(notes, files), defaultNote);
    }

    @Test
    void aVaultWithNoTaggedNoteIsRefusedAndNamesTheTag() {
        BuildReport report = service(List.of(new VaultNote("x.md", "plain".getBytes(StandardCharsets.UTF_8))), "")
                .build(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("'t'");
        assertThat(report.vaultPath()).isEqualTo("/vault");
    }

    @Test
    void aMissingVaultIsRefusedAndNamesThePath() {
        VaultReader missing = new VaultReader() {
            @Override
            public Vault read() {
                return Vault.unusable("/nope", Vault.Status.MISSING);
            }

            @Override
            public byte[] readFile(String path) {
                return new byte[0];
            }
        };
        BuildReport report = service(missing, "").build(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.title()).isEqualTo("Vault directory not found");
        assertThat(report.detail()).contains("/nope");
    }

    @Test
    void anUnsetVaultOrTemplatePathIsRefusedByItsPropertyName() {
        BuildService noVault = new BuildService(vaultOf(List.of()), new CompleteTemplate(),
                new ConfiguredPaths(" ", "/template", null), "t", false, "");
        assertThat(noVault.build(false).detail()).contains("mb2e.vault.path");
        BuildService noTemplate = new BuildService(vaultOf(List.of()), new CompleteTemplate(),
                new ConfiguredPaths("/vault", null, null), "t", false, "");
        assertThat(noTemplate.build(false).detail()).contains("mb2e.template.path");
    }

    @Test
    void theTargetIsNotNeededToBuild() {
        BuildReport report = service(List.of(note("x.md", "Tekst.", "t")), "").build(false);
        assertThat(report.isRefused()).isFalse();
        assertThat(report.noteCount()).isEqualTo(1);
    }

    @Test
    void aDryRunReportsTheSameCountsAndIsMarked() {
        Assembly real = service(List.of(note("x.md", "Tekst.", "t")), "").assemble(false);
        Assembly dry = service(List.of(note("x.md", "Tekst.", "t")), "").assemble(true);
        assertThat(dry.report().dryRun()).isTrue();
        assertThat(real.report().dryRun()).isFalse();
        assertThat(dry.report().outputBytes()).isEqualTo(real.report().outputBytes());
        assertThat(dry.site().dataScript()).isEqualTo(real.site().dataScript());
    }

    @Test
    void theStartNoteFallsBackWithAWarningAndAMissingAboutPageWarnsFirst() {
        BuildReport report = service(List.of(note("x.md", "Tekst.", "t")), "nope").build(false);
        assertThat(report.defaultNote()).isEqualTo("x");
        assertThat(report.warnings()).hasSize(2);
        assertThat(report.warnings().get(0)).contains("about.md");
        assertThat(report.warnings().get(1)).isEqualTo("Configured start note 'nope' does not exist; using 'x'");
    }

    @Test
    void anEmptyConfiguredStartNoteTakesTheFirstNoteWithoutAWarning() {
        BuildReport report = service(List.of(note("x.md", "Tekst.", "t"), note("about.md", "Ja.", "t")), "")
                .build(false);
        assertThat(report.defaultNote()).isEqualTo("x");
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void theAboutNoteIsServedButNotListedInTheTree() {
        Assembly built = service(List.of(note("about.md", "Ja.", "t"), note("A/x.md", "Tekst.", "t")), "about")
                .assemble(false);
        String script = built.site().dataScript();
        assertThat(script).startsWith("window.BLOG_DATA = {\"defaultNote\":\"about\",\"TREE\":[{\"name\":\"A\","
                + "\"children\":[{\"note\":\"A/x\"}]}],\"NOTES\":{\"A/x\":");
        assertThat(script).contains("\"about\":{\"title\":\"about\"").endsWith("}};\n");
        assertThat(built.report().warnings()).isEmpty();
    }

    @Test
    void anAboutNoteInAFolderIsAnOrdinaryNote() {
        Assembly built = service(List.of(note("Inne/about.md", "Ja.", "t")), "about").assemble(false);
        assertThat(built.site().dataScript()).contains("{\"note\":\"Inne/about\"}");
        assertThat(built.report().defaultNote()).isEqualTo("Inne/about");
    }

    @Test
    void aFolderWithNoSelectedNoteIsNotInTheTree() {
        Assembly built = service(List.of(note("A/x.md", "Tekst.", "t"),
                new VaultNote("B/y.md", "plain".getBytes(StandardCharsets.UTF_8))), "").assemble(false);
        assertThat(built.site().dataScript()).contains("\"name\":\"A\"").doesNotContain("\"name\":\"B\"");
    }

    @Test
    void aLeadingH1EqualToTheTitleIsLeftOutAndNoLeadIsLiftedOut() {
        Assembly built = service(List.of(note("x.md", "# x\n\nPierwszy.\n\nDrugi.", "t")), "").assemble(false);
        assertThat(built.site().dataScript()).contains("\"html\":\"<p>Pierwszy.</p>\\n<p>Drugi.</p>\"")
                .doesNotContain("\"lead\"").doesNotContain("\"minutes\"");
    }

    @Test
    void anUnresolvedLinkAndAnEmbedAreCountedAndTheImageIsPublishedUnderTheNoteImages() {
        Assembly built = service(List.of(note("x.md", "[[nie ma]] ![[a b.png]] ![[brak.png]]", "t")), "",
                "img/a b.png").assemble(false);
        BuildReport report = built.report();
        assertThat(report.missingLinks()).containsExactly("x -> [[brak.png]]", "x -> [[nie ma]]");
        assertThat(report.missingAssets()).containsExactly("x -> [[brak.png]]");
        assertThat(report.attachments()).containsExactly("img/a b.png");
        assertThat(built.site().images()).containsExactly("img/a b.png");
        assertThat(built.site().dataScript()).contains("src=\\\"assets/img/notes/img/a%20b.png\\\"");
    }

    @Test
    void anImageIsFoundNextToTheNoteBeforeAnywhereElse() {
        Assembly built = service(List.of(note("A/x.md", "![[p.png]]", "t")), "", "B/p.png", "A/p.png")
                .assemble(false);
        assertThat(built.report().attachments()).containsExactly("A/p.png");
    }

    @Test
    void twoRunsCarryDifferentTraceIds() {
        BuildService service = service(List.of(note("x.md", "T.", "t")), "");
        assertThat(service.build(true).traceId()).isNotEqualTo(service.build(true).traceId());
    }

    @Test
    void aTemplateThatFailsTheCheckRefusesTheBuild() {
        BuildService service = new BuildService(vaultOf(List.of(note("x.md", "T.", "t"))), new CompleteTemplate() {
            @Override
            public boolean isFile(String path) {
                return !path.equals("assets/js/app.js");
            }
        }, PATHS, "t", false, "");
        BuildReport report = service.build(false);
        assertThat(report.isRefused()).isTrue();
        assertThat(report.detail()).contains("assets/js/app.js");
    }

    /** A template directory that holds everything the template check asks for. */
    static class CompleteTemplate implements SiteSource {
        @Override
        public String location() {
            return "/template";
        }

        @Override
        public boolean isFile(String path) {
            return true;
        }

        @Override
        public boolean isDirectory(String path) {
            return true;
        }

        @Override
        public byte[] read(String path) {
            return "switch (b.type) { case 'html': return b.html; }".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public List<String> files(String directory) {
            return List.of();
        }
    }
}
