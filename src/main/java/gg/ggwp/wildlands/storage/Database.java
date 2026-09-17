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
            if (version > 1 || version < 0)
                throw new SQLException("Unsupported database schema " + version + "; refusing migration");
            if (version == 0) {
                connection.setAutoCommit(false);
                try {
                    statement.execute("""
                        CREATE TABLE players (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          last_known_name TEXT NOT NULL,
                          first_seen INTEGER NOT NULL CHECK(first_seen >= 0),
                          last_seen INTEGER NOT NULL CHECK(last_seen >= first_seen)
                        )
                        """);
                    statement.execute("PRAGMA user_version=1");
                    connection.commit();
                } catch (SQLException failure) {
                    connection.rollback();
                    throw failure;
                } finally { connection.setAutoCommit(true); }
            }
            try (ResultSet ignored = statement.executeQuery(
                    "SELECT uuid,last_known_name,first_seen,last_seen FROM players LIMIT 0")) {
                // Verify an existing schema before reporting startup success.
            }
        } catch (SQLException failure) {
            try { close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
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
