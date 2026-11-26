package io.github.mbero.mb2e.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceLockTest {

    @TempDir
    Path workspace;

    @Test
    void aSecondLockOnTheSameDirectoryIsRefusedWithTheLockFileName() {
        try (WorkspaceLock first = WorkspaceLock.acquire(workspace)) {
            assertThat(first).isNotNull();
            assertThatThrownBy(() -> WorkspaceLock.acquire(workspace)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(".mb2e.lock");
        }
    }

    @Test
    void theLockCanBeTakenAgainAfterItIsReleased() {
        WorkspaceLock.acquire(workspace).close();
        try (WorkspaceLock again = WorkspaceLock.acquire(workspace)) {
            assertThat(again).isNotNull();
        }
    }

    @Test
    void aMissingWorkspaceDirectoryIsCreated() {
        Path nested = workspace.resolve("a/b");
        try (WorkspaceLock lock = WorkspaceLock.acquire(nested)) {
            assertThat(lock).isNotNull();
            assertThat(nested.resolve(".mb2e.lock")).exists();
        }
    }

    @Test
    void aWorkspaceThatIsAFileIsRefused() throws Exception {
        Path file = java.nio.file.Files.writeString(workspace.resolve("file"), "x");
        assertThatThrownBy(() -> WorkspaceLock.acquire(file)).isInstanceOf(IllegalStateException.class);
    }
}
