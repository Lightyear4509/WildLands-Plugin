package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvironmentRepositoryTest {
    @TempDir Path directory;
    @Test void upgradesVersionTwoWithoutChangingIdentityHydrationOrHudPreferences() throws Exception {
        Path file = directory.resolve("upgrade-v2.db");
        UUID id = UUID.fromString("00000000-0000-0000-0009-01fe76725ed6");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players(uuid TEXT PRIMARY KEY,last_known_name TEXT,first_seen INTEGER,last_seen INTEGER)");
            statement.execute("CREATE TABLE hydration_players(uuid TEXT PRIMARY KEY,hydration REAL,hud_enabled INTEGER,dry_seconds REAL)");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO players VALUES(?,?,?,?)")) {
                insert.setString(1, id.toString()); insert.setString(2, ".ExistingPlayer");
                insert.setLong(3, 100); insert.setLong(4, 200); insert.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO hydration_players VALUES(?,?,?,?)")) {
                insert.setString(1, id.toString()); insert.setDouble(2, 0);
                insert.setInt(3, 0); insert.setDouble(4, 125.5); insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version=2");
        }
        for (int reopen = 0; reopen < 2; reopen++) {
            try (Database database = new Database()) {
                database.open(file);
                assertEquals(new PlayerRecord(id, ".ExistingPlayer", 100, 200), new PlayerRepository(database).find(id).orElseThrow());
                assertEquals(new HydrationRecord(id, 0, false, 125.5), new HydrationRepository(database).find(id).orElseThrow());
                assertTrue(new EnvironmentRepository(database).find(id).isEmpty());
                try (Statement statement = database.connection().createStatement();
                     ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                    assertEquals(4, result.getInt(1));
                }
            }
        }
    }
    @Test void malformedVersionTwoIsRejectedBeforeMigration() throws Exception {
        Path file = directory.resolve("malformed-v2.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players(uuid TEXT PRIMARY KEY,last_known_name TEXT,first_seen INTEGER,last_seen INTEGER)");
            statement.execute("CREATE TABLE hydration_players(uuid TEXT PRIMARY KEY)");
            statement.execute("PRAGMA user_version=2");
        }
        try (Database database = new Database()) {
            assertThrows(SQLException.class, () -> database.open(file));
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                assertEquals(2, result.getInt(1));
            }
            try (ResultSet result = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='environment_players'")) {
                assertEquals(0, result.getInt(1));
            }
        }
    }
    @Test void snapshotsSurviveReopenAndAreKeyedByUuid() throws Exception {
        UUID javaId = UUID.randomUUID();
        UUID bedrockId = UUID.fromString("00000000-0000-0000-0009-01fe76725ed6");
        var javaState = new EnvironmentRecord(javaId, 34.5, 72);
        var bedrockState = new EnvironmentRecord(bedrockId, 18, 5);
        Path file = directory.resolve("environment.db");
        try (Database database = new Database()) {
            database.open(file);
            new EnvironmentRepository(database).saveBatch(List.of(javaState, bedrockState));
        }
        try (Database database = new Database()) {
            database.open(file);
            var repository = new EnvironmentRepository(database);
            assertEquals(javaState, repository.find(javaId).orElseThrow());
            assertEquals(bedrockState, repository.find(bedrockId).orElseThrow());
        }
    }
}
