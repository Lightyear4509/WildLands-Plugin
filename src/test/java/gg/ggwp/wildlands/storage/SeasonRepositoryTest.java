package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.seasons.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SeasonRepositoryTest {
    @TempDir Path directory;

    @Test void distinctWorldClocksSurviveReopen() throws Exception {
        Path file = directory.resolve("worlds.db");
        var first = new SeasonRecord(UUID.randomUUID(), new SeasonClock.State(Season.MONSOON, 12345));
        var second = new SeasonRecord(UUID.randomUUID(), new SeasonClock.State(Season.DRY, 42));
        try (var database = new Database()) {
            database.open(file);
            new SeasonRepository(database).saveBatch(List.of(first, second));
        }
        try (var database = new Database()) {
            database.open(file);
            assertEquals(first, new SeasonRepository(database).find(first.worldUuid()).orElseThrow());
            assertEquals(second, new SeasonRepository(database).find(second.worldUuid()).orElseThrow());
        }
    }

    @Test void migrationPreservesEnvironmentAndRejectsMalformedPreviousSchema() throws Exception {
        for (boolean valid : new boolean[]{true, false}) {
            Path file = directory.resolve("schema-3-" + valid + ".db");
            UUID id = UUID.randomUUID();
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE players(uuid TEXT PRIMARY KEY,last_known_name TEXT,first_seen INTEGER,last_seen INTEGER)");
                statement.execute("CREATE TABLE hydration_players(uuid TEXT PRIMARY KEY,hydration REAL,hud_enabled INTEGER,dry_seconds REAL)");
                statement.execute(valid ? "CREATE TABLE environment_players(uuid TEXT PRIMARY KEY,temperature REAL,wetness REAL)"
                        : "CREATE TABLE environment_players(uuid TEXT PRIMARY KEY)");
                if (valid) {
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO environment_players VALUES(?,?,?)")) {
                        insert.setString(1, id.toString()); insert.setDouble(2, 25); insert.setDouble(3, 80); insert.executeUpdate();
                    }
                }
                statement.execute("PRAGMA user_version=3");
            }
            try (var database = new Database()) {
                if (valid) {
                    database.open(file);
                    assertEquals(new EnvironmentRecord(id, 25, 80), new EnvironmentRepository(database).find(id).orElseThrow());
                } else assertThrows(SQLException.class, () -> database.open(file));
            }
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 Statement statement = connection.createStatement()) {
                try (ResultSet rows = statement.executeQuery("PRAGMA user_version")) {
                    assertEquals(valid ? 7 : 3, rows.getInt(1));
                }
                try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='season_state'")) {
                    assertEquals(valid ? 1 : 0, rows.getInt(1));
                }
            }
        }
    }
}
