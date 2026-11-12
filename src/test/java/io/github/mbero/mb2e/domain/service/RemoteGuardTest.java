package io.github.mbero.mb2e.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RemoteGuardTest {

    @Test
    void thePagesRepositoryOfItsOwnerPassesInBothUrlForms() {
        assertThat(RemoteGuard.isPagesRepository("git@github.com:mbero/mbero.github.io.git")).isTrue();
        assertThat(RemoteGuard.isPagesRepository("git@github.com:mbero/mbero.github.io")).isTrue();
        assertThat(RemoteGuard.isPagesRepository("https://github.com/someone/someone.github.io.git")).isTrue();
        assertThat(RemoteGuard.isPagesRepository("https://github.com/someone/someone.github.io")).isTrue();
    }

    @Test
    void letterCaseDoesNotMatter() {
        assertThat(RemoteGuard.isPagesRepository("https://github.com/MBero/mbero.github.io")).isTrue();
        assertThat(RemoteGuard.isPagesRepository("git@github.com:MBERO/mbero.GitHub.io.git")).isTrue();
    }

    @Test
    void anotherRepositoryAnotherOwnerAndAnotherUrlFormAreRefused() {
        assertThat(RemoteGuard.isPagesRepository("git@github.com:mbero/jakis-inny-projekt.git")).isFalse();
        assertThat(RemoteGuard.isPagesRepository("git@github.com:someone/mbero.github.io.git")).isFalse();
        assertThat(RemoteGuard.isPagesRepository("ssh://git@github.com/mbero/mbero.github.io.git")).isFalse();
        assertThat(RemoteGuard.isPagesRepository("https://example.com/mbero/mbero.github.io")).isFalse();
        assertThat(RemoteGuard.isPagesRepository("https://github.com/mbero/mbero.github.io/extra")).isFalse();
    }
}
