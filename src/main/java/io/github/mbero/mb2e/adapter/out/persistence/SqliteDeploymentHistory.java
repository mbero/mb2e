package io.github.mbero.mb2e.adapter.out.persistence;

import io.github.mbero.mb2e.domain.model.Deployment;
import io.github.mbero.mb2e.domain.port.out.DeploymentHistory;
import io.github.mbero.mb2e.domain.port.out.StoreFailure;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the deployment history in one SQLite file and drops the entries beyond the retention limit.
 * <p>The schema is created on first use, so a broken file fails a call, never the service start-up.
 */
public class SqliteDeploymentHistory implements DeploymentHistory {

    private static final String SCHEMA = "/db/deployments.sql";
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;

    private final Path file;
    private final int keep;
    private boolean ready;

    public SqliteDeploymentHistory(Path file, int keep) {
        if (keep < 1) {
            throw new IllegalArgumentException("keep must be at least 1");
        }
        this.file = file.toAbsolutePath();
        this.keep = keep;
    }

    @Override
    public synchronized void record(Deployment deployment) {
        try (Connection connection = open();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO deployments (finished_at, "
                        + "outcome, trigger_name, attempts, source_commit, target_commit, cause) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)");
                PreparedStatement prune = connection.prepareStatement("DELETE FROM deployments WHERE id NOT IN "
                        + "(SELECT id FROM deployments ORDER BY id DESC LIMIT ?)")) {
            insert.setString(1, deployment.finishedAt().toString());
            insert.setString(2, deployment.outcome());
            insert.setString(3, deployment.trigger());
            insert.setInt(4, deployment.attempts());
            insert.setString(5, deployment.sourceCommit());
            insert.setString(6, deployment.targetCommit());
            insert.setString(7, deployment.cause());
            insert.executeUpdate();
            prune.setInt(1, keep);
            prune.executeUpdate();
        } catch (SQLException failure) {
            throw new StoreFailure("The deployment history cannot be written: " + file, failure);
        }
    }

    @Override
    public synchronized List<Deployment> newest(int limit) {
        try (Connection connection = open();
                PreparedStatement select = connection.prepareStatement("SELECT finished_at, outcome, trigger_name, "
                        + "attempts, source_commit, target_commit, cause FROM deployments ORDER BY id DESC LIMIT ?")) {
            select.setInt(1, Math.max(0, limit));
            List<Deployment> out = new ArrayList<>();
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    out.add(new Deployment(Instant.parse(rows.getString(1)), rows.getString(2), rows.getString(3),
                            rows.getInt(4), rows.getString(5), rows.getString(6), rows.getString(7)));
                }
            }
            return out;
        } catch (SQLException failure) {
            throw new StoreFailure("The deployment history cannot be read: " + file, failure);
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS);
            if (!ready) {
                for (String sql : schema().split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
                ready = true;
            }
            return connection;
        } catch (SQLException | RuntimeException failure) {
            connection.close();
            throw failure;
        }
    }

    private static String schema() {
        try (InputStream in = SqliteDeploymentHistory.class.getResourceAsStream(SCHEMA)) {
            if (in == null) {
                throw new StoreFailure("The schema script is missing: " + SCHEMA, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
        } catch (IOException failure) {
            throw new StoreFailure("The schema script cannot be read: " + SCHEMA, failure);
        }
    }
}
