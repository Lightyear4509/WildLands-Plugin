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
            if (version > 7 || version < 0)
                throw new SQLException("Unsupported database schema " + version + "; refusing migration");
            if (version > 0) verifyPlayers(statement);
            if (version >= 2) verifyHydration(statement);
            if (version >= 3) verifyEnvironment(statement);
            if (version >= 4) verifySeasons(statement);
            if (version >= 5) verifyWorlds(statement);
            if (version >= 6) verifyWildlife(statement);
            if (version >= 7) verifyExploration(statement);
            if (version < 7) {
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
                    if (version < 2) statement.execute("""
                        CREATE TABLE hydration_players (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          hydration REAL NOT NULL CHECK(hydration >= 0 AND hydration <= 100),
                          hud_enabled INTEGER NOT NULL CHECK(hud_enabled IN (0,1)),
                          dry_seconds REAL NOT NULL CHECK(dry_seconds >= 0)
                        )
                        """);
                    if (version < 3) statement.execute("""
                        CREATE TABLE environment_players (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          temperature REAL NOT NULL CHECK(temperature >= -100 AND temperature <= 100),
                          wetness REAL NOT NULL CHECK(wetness >= 0 AND wetness <= 100)
                        )
                        """);
                    if (version < 4) statement.execute("""
                        CREATE TABLE season_state (
                          world_uuid TEXT PRIMARY KEY NOT NULL,
                          season TEXT NOT NULL CHECK(season IN ('DRY','TRANSITION_TO_WET','WET','MONSOON','TRANSITION_TO_DRY')),
                          elapsed_ticks INTEGER NOT NULL CHECK(elapsed_ticks >= 0)
                        )
                        """);
                    if (version < 5) statement.execute("""
                        CREATE TABLE wildlands_worlds (
                          name TEXT PRIMARY KEY COLLATE NOCASE NOT NULL,
                          uuid TEXT UNIQUE,
                          seed INTEGER NOT NULL,
                          generator_version INTEGER NOT NULL CHECK(generator_version=1),
                          sea_level INTEGER NOT NULL CHECK(sea_level BETWEEN 32 AND 128),
                          tree_density REAL NOT NULL CHECK(tree_density BETWEEN 0 AND 1),
                          caves INTEGER NOT NULL CHECK(caves IN (0,1)),
                          ores INTEGER NOT NULL CHECK(ores IN (0,1))
                        )
                        """);
                    if (version < 6) {
                    statement.execute("""
                        CREATE TABLE wildlife (
                          uuid TEXT PRIMARY KEY NOT NULL,
                          world_uuid TEXT NOT NULL,
                          home_x REAL NOT NULL,
                          home_y REAL NOT NULL,
                          home_z REAL NOT NULL,
                          alive INTEGER NOT NULL CHECK(alive IN (0,1))
                        )
                        """);
                    statement.execute("CREATE INDEX wildlife_world_idx ON wildlife(world_uuid)");
                    }
                    statement.execute("""
                        CREATE TABLE wildlands_worlds_v7 (
                          name TEXT PRIMARY KEY COLLATE NOCASE NOT NULL,
                          uuid TEXT UNIQUE, seed INTEGER NOT NULL,
                          generator_version INTEGER NOT NULL CHECK(generator_version IN (1,2)),
                          sea_level INTEGER NOT NULL CHECK(sea_level BETWEEN 32 AND 128),
                          tree_density REAL NOT NULL CHECK(tree_density BETWEEN 0 AND 1),
                          caves INTEGER NOT NULL CHECK(caves IN (0,1)), ores INTEGER NOT NULL CHECK(ores IN (0,1))
                        )
                        """);
                    statement.execute("INSERT INTO wildlands_worlds_v7 SELECT name,uuid,seed,generator_version,sea_level,tree_density,caves,ores FROM wildlands_worlds");
                    statement.execute("DROP TABLE wildlands_worlds");
                    statement.execute("ALTER TABLE wildlands_worlds_v7 RENAME TO wildlands_worlds");
                    statement.execute("""
                        CREATE TABLE landmarks (
                          id TEXT PRIMARY KEY NOT NULL, world_uuid TEXT NOT NULL,
                          name TEXT NOT NULL, kind TEXT NOT NULL,
                          x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, owner_uuid TEXT
                        )
                        """);
                    statement.execute("CREATE UNIQUE INDEX landmarks_name_idx ON landmarks(world_uuid,name)");
                    statement.execute("CREATE UNIQUE INDEX landmarks_camp_idx ON landmarks(owner_uuid) WHERE owner_uuid IS NOT NULL");
                    statement.execute("""
                        CREATE TABLE discoveries (
                          player_uuid TEXT NOT NULL, landmark_id TEXT NOT NULL REFERENCES landmarks(id) ON DELETE CASCADE,
                          first_seen INTEGER NOT NULL CHECK(first_seen>=0), PRIMARY KEY(player_uuid,landmark_id)
                        )
                        """);
                    statement.execute("""
                        CREATE TABLE exploration_players (
                          uuid TEXT PRIMARY KEY NOT NULL, target_id TEXT REFERENCES landmarks(id) ON DELETE SET NULL,
                          longest_distance REAL NOT NULL CHECK(longest_distance>=0 AND longest_distance<=100000000),
                          completed_trips INTEGER NOT NULL CHECK(completed_trips BETWEEN 0 AND 10000000),
                          active INTEGER NOT NULL CHECK(active IN (0,1)),
                          rank TEXT NOT NULL CHECK(rank IN ('NOVICE','SCOUT','EXPLORER','PATHFINDER'))
                        )
                        """);
                    statement.execute("PRAGMA user_version=7");
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
            try (ResultSet ignored = statement.executeQuery(
                    "SELECT uuid,temperature,wetness FROM environment_players LIMIT 0")) {
                // Verify the environment schema before reporting startup success.
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

    private static void verifyHydration(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery(
                "SELECT uuid,hydration,hud_enabled,dry_seconds FROM hydration_players LIMIT 0")) {
        }
    }

    private static void verifyEnvironment(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery("SELECT uuid,temperature,wetness FROM environment_players LIMIT 0")) {}
    }

    private static void verifySeasons(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery("SELECT world_uuid,season,elapsed_ticks FROM season_state LIMIT 0")) {}
    }
    private static void verifyWorlds(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery("SELECT name,uuid,seed,generator_version,sea_level,tree_density,caves,ores FROM wildlands_worlds LIMIT 0")) {}
    }
    private static void verifyWildlife(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery("SELECT uuid,world_uuid,home_x,home_y,home_z,alive FROM wildlife LIMIT 0")) {}
    }
    private static void verifyExploration(Statement statement) throws SQLException {
        try (ResultSet ignored = statement.executeQuery("SELECT id,world_uuid,name,kind,x,y,z,owner_uuid FROM landmarks LIMIT 0")) {}
        try (ResultSet ignored = statement.executeQuery("SELECT player_uuid,landmark_id,first_seen FROM discoveries LIMIT 0")) {}
        try (ResultSet ignored = statement.executeQuery("SELECT uuid,target_id,longest_distance,completed_trips,active,rank FROM exploration_players LIMIT 0")) {}
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
