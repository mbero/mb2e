package io.github.mbero.mb2e.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteDeploymentHistoryTest {

    @TempDir
    Path dir;

    private static Deployment deployment(int second, String outcome, String cause) {
        return new Deployment(Instant.parse("2026-11-26T10:00:00Z").plusSeconds(second), outcome, "push", 1,
                "src" + second, outcome.equals("deployed") ? "tgt" + second : null, cause);
    }

    @Test
    void theNewestDeploymentComesFirstWithEveryFieldIntact() {
        SqliteDeploymentHistory history = new SqliteDeploymentHistory(dir.resolve("history.db"), 500);
        history.record(deployment(1, "deployed", null));
        history.record(deployment(2, "failed", "Git failed\nłódź: token expired"));
        assertThat(history.newest(10)).containsExactly(deployment(2, "failed", "Git failed\nłódź: token expired"),
                deployment(1, "deployed", null));
    }

    @Test
    void anEmptyHistoryGivesAnEmptyList() {
        assertThat(new SqliteDeploymentHistory(dir.resolve("history.db"), 500).newest(50)).isEmpty();
    }

    @Test
    void theLimitCutsTheList() {
        SqliteDeploymentHistory history = new SqliteDeploymentHistory(dir.resolve("history.db"), 500);
        for (int i = 0; i < 5; i++) {
            history.record(deployment(i, "up_to_date", null));
        }
        assertThat(history.newest(2)).extracting(Deployment::sourceCommit).containsExactly("src4", "src3");
    }

    @Test
    void onlyTheNewestEntriesWithinTheRetentionLimitStay() {
        SqliteDeploymentHistory history = new SqliteDeploymentHistory(dir.resolve("history.db"), 3);
        for (int i = 0; i < 7; i++) {
            history.record(deployment(i, "up_to_date", null));
        }
        assertThat(history.newest(100)).extracting(Deployment::sourceCommit).containsExactly("src6", "src5", "src4");
    }

    @Test
    void theHistorySurvivesANewInstanceOnTheSameFile() {
        Path file = dir.resolve("history.db");
        new SqliteDeploymentHistory(file, 500).record(deployment(1, "deployed", null));
        assertThat(new SqliteDeploymentHistory(file, 500).newest(10)).hasSize(1);
    }

    @Test
    void anUnusableFileFailsTheCallWithAStoreFailureThatNamesIt() throws Exception {
        Path notAFile = Files.createDirectory(dir.resolve("history.db"));
        SqliteDeploymentHistory history = new SqliteDeploymentHistory(notAFile, 500);
        assertThatThrownBy(() -> history.record(deployment(1, "deployed", null)))
                .isInstanceOf(StoreFailure.class).hasMessageContaining("history.db");
        assertThatThrownBy(() -> history.newest(1)).isInstanceOf(StoreFailure.class);
    }
}
