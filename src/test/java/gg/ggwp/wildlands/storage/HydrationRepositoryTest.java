package gg.ggwp.wildlands.storage;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HydrationRepositoryTest {
    @TempDir Path directory;

    @Test void migratesFoundationWithoutChangingPlayerIdentityOrTimestamps() throws Exception {
        Path file = directory.resolve("upgrade.db");
        UUID id = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players(uuid TEXT PRIMARY KEY,last_known_name TEXT,first_seen INTEGER,last_seen INTEGER)");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO players VALUES(?,?,?,?)")) {
                insert.setString(1, id.toString()); insert.setString(2, "PreviousPlayer");
                insert.setLong(3, 100); insert.setLong(4, 200); insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version=1");
        }
        try (Database db = new Database()) {
            db.open(file);
            assertEquals(new PlayerRecord(id, "PreviousPlayer", 100, 200), new PlayerRepository(db).find(id).orElseThrow());
            assertTrue(new HydrationRepository(db).find(id).isEmpty());
            try (Statement statement = db.connection().createStatement(); ResultSet row = statement.executeQuery("PRAGMA user_version")) {
                assertEquals(7, row.getInt(1));
            }
        }
    }

    @Test void snapshotsSurviveReopenAndRemainIsolatedByUuid() throws Exception {
        Path file = directory.resolve("hydration.db");
        UUID javaId = UUID.randomUUID();
        UUID bedrockId = UUID.fromString("00000000-0000-0000-0009-01fe76725ed6");
        HydrationRecord javaState = new HydrationRecord(javaId, 33.5, false, 0);
        HydrationRecord bedrockState = new HydrationRecord(bedrockId, 0, true, 123.5);
        try (Database db = new Database()) {
            db.open(file);
            HydrationRepository repository = new HydrationRepository(db);
            repository.saveBatch(List.of(new HydrationRecord(javaId, 100, true, 0), bedrockState));
            repository.saveBatch(List.of(javaState));
        }
        try (Database db = new Database()) {
            db.open(file);
            HydrationRepository repository = new HydrationRepository(db);
            assertEquals(javaState, repository.find(javaId).orElseThrow());
            assertEquals(bedrockState, repository.find(bedrockId).orElseThrow());
        }
    }

    @Test void failedBatchRollsBackEarlierRowsAndAllowsRetry() throws Exception {
        try (Database db = new Database()) {
            db.open(directory.resolve("rollback.db"));
            HydrationRepository repository = new HydrationRepository(db);
            HydrationRecord first = new HydrationRecord(UUID.randomUUID(), 50, true, 0);
            HydrationRecord rejected = new HydrationRecord(UUID.randomUUID(), 42, true, 0);
            try (Statement statement = db.connection().createStatement()) {
                statement.execute("CREATE TRIGGER reject_test BEFORE INSERT ON hydration_players WHEN NEW.hydration=42 BEGIN SELECT RAISE(ABORT,'test failure'); END");
            }
            assertThrows(SQLException.class, () -> repository.saveBatch(List.of(first, rejected)));
            assertTrue(repository.find(first.uuid()).isEmpty());
            assertTrue(db.connection().getAutoCommit());
            try (Statement statement = db.connection().createStatement()) { statement.execute("DROP TRIGGER reject_test"); }
            repository.saveBatch(List.of(first, rejected));
            assertEquals(first, repository.find(first.uuid()).orElseThrow());
        }
    }

    @Test void malformedFoundationIsNotAdvanced() throws Exception {
        Path file = directory.resolve("malformed.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) { statement.execute("PRAGMA user_version=1"); }
        try (Database db = new Database()) { assertThrows(SQLException.class, () -> db.open(file)); }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement(); ResultSet row = statement.executeQuery("PRAGMA user_version")) {
            assertEquals(1, row.getInt(1));
        }
    }
}
