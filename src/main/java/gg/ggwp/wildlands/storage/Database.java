package gg.ggwp.wildlands.storage;

import java.nio.file.Path;
import java.sql.*;

public final class Database implements AutoCloseable {
    private Connection connection;

    public void open(Path file) throws SQLException {
        if (connection != null) throw new IllegalStateException("Database already open");
        // Explicit driver use avoids relying on Paper's thread context classloader for JDBC discovery.
        connection = new org.sqlite.JDBC().connect("jdbc:sqlite:" + file.toAbsolutePath(), new java.util.Properties());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            int version;
            try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                version = result.getInt(1);
            }
            if (version > 2 || version < 0)
                throw new SQLException("Unsupported database schema " + version + "; refusing migration");
            if (version > 0) verifyPlayers(statement);
            if (version < 2) {
                connection.setAutoCommit(false);
                try {
                    if (version == 0) statement.execute("""
                        CREATE TABLE players (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          last_known_name TEXT NOT NULL,
                          first_seen INTEGER NOT NULL CHECK(first_seen >= 0),
                          last_seen INTEGER NOT NULL CHECK(last_seen >= first_seen)
                        )
                        """);
                    statement.execute("""
                        CREATE TABLE hydration_players (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          hydration REAL NOT NULL CHECK(hydration >= 0 AND hydration <= 100),
                          hud_enabled INTEGER NOT NULL CHECK(hud_enabled IN (0,1)),
                          dry_seconds REAL NOT NULL CHECK(dry_seconds >= 0)
                        )
                        """);
                    statement.execute("PRAGMA user_version=2");
                    connection.commit();
                } catch (SQLException failure) {
                    connection.rollback();
                    throw failure;
                } finally { connection.setAutoCommit(true); }
            }
            try (ResultSet ignored = statement.executeQuery(
                    "SELECT uuid,hydration,hud_enabled,dry_seconds FROM hydration_players LIMIT 0")) {
                // Verify an existing schema before reporting startup success.
            }
        } catch (SQLException failure) {
            try { close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    private static void verifyPlayers(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery(
                "SELECT uuid,last_known_name,first_seen,last_seen FROM players LIMIT 0")) {
            // Validate the old schema before changing its version or adding tables.
        }
    }

    Connection connection() {
        if (connection == null) throw new IllegalStateException("Database is not open");
        return connection;
    }

    @Override public void close() throws SQLException {
        if (connection != null) {
            try { connection.close(); } finally { connection = null; }
        }
    }
}
