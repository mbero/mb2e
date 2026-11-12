package io.github.mbero.mb2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies that the only configuration sources are the packaged file and SPRING_APPLICATION_JSON keys under mb2e. */
class Mb2eEnvironmentTest {

    @Test
    void keysUnderMb2eAreAccepted_flatOrNested() {
        Mb2eEnvironment flat = Mb2eEnvironment.create("{\"mb2e.vault.path\":\"/v\",\"mb2e.force\":true}");
        assertThat(flat.getProperty("mb2e.vault.path")).isEqualTo("/v");
        assertThat(flat.getProperty("mb2e.force", Boolean.class)).isTrue();
        Mb2eEnvironment nested = Mb2eEnvironment.create("{\"mb2e\":{\"vault\":{\"path\":\"/n\"}}}");
        assertThat(nested.getProperty("mb2e.vault.path")).isEqualTo("/n");
    }

    @Test
    void aKeyOutsideMb2eIsRefusedByName() {
        for (String key : new String[] {"spring.main.sources", "spring.shell.history.enabled", "logging.level.root"}) {
            assertThatThrownBy(() -> Mb2eEnvironment.create("{\"" + key + "\":\"x\"}"))
                    .isInstanceOf(Mb2eEnvironment.Refused.class).hasMessageContaining(key);
        }
        assertThatThrownBy(() -> Mb2eEnvironment.create("{\"spring\":{\"main\":{\"sources\":\"x\"}}}"))
                .hasMessageContaining("spring.main.sources");
        assertThatThrownBy(() -> Mb2eEnvironment.create("not json")).isInstanceOf(Mb2eEnvironment.Refused.class);
    }

    @Test
    void noSystemPropertyAndNoOsEnvironmentReachesIt() {
        System.setProperty("spring.main.banner-mode", "console");
        try {
            Mb2eEnvironment environment = Mb2eEnvironment.create(null);
            assertThat(environment.getProperty("spring.main.banner-mode")).isNull();
            assertThat(environment.getProperty("PATH")).isNull();
            assertThat(environment.getProperty("spring.config.location"))
                    .isEqualTo("classpath:/application.yaml,optional:file:application_local_paths.yaml");
        } finally {
            System.clearProperty("spring.main.banner-mode");
        }
    }

    @Test
    void aSystemPropertyForASpringKeyDoesNotChangeARun() {
        System.setProperty("spring.shell.command.history.enabled", "true");
        System.setProperty("spring.main.sources", "org.springframework.shell.boot.StandardCommandsAutoConfiguration");
        try {
            assertThat(Mb2eApplication.run("history")).isEqualTo(1);
            assertThat(Mb2eApplication.run("version")).isEqualTo(1);
        } finally {
            System.clearProperty("spring.shell.command.history.enabled");
            System.clearProperty("spring.main.sources");
        }
    }

    @Test
    void mb2eConfigNamesTheLocalPathsFile() {
        assertThat(Mb2eEnvironment.create(null, "/etc/mb2e/paths.yaml").getProperty("spring.config.location"))
                .isEqualTo("classpath:/application.yaml,optional:file:/etc/mb2e/paths.yaml");
    }

    @Test
    void aWorkingDirectoryApplicationFileDoesNotChangeARun(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        // Only the packaged file and the local paths file are configuration locations.
        assertThat(Mb2eEnvironment.create(null).getProperty("spring.config.location"))
                .isEqualTo("classpath:/application.yaml,optional:file:application_local_paths.yaml");
        assertThat(Mb2eEnvironment.create(null).getProperty("spring.config.additional-location")).isNull();
        assertThat(Mb2eEnvironment.create(null).getProperty("spring.profiles.active")).isNull();
        assertThat(dir).isDirectory();
    }
}
