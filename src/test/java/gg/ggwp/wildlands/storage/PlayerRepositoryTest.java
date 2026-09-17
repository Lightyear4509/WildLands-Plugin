package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerRepositoryTest {
    @TempDir Path directory;
    @Test void persistsUuidAcrossRestartAndRenameWithoutMergingNames() throws Exception {
        Path file = directory.resolve("players.db");
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try (var db = new Database()) {
            db.open(file);
            var repository = new PlayerRepository(db);
            repository.saveBatch(List.of(new PlayerRecord(first, ".bedrock", 100, 100),
                    new PlayerRecord(second, ".bedrock", 110, 110)));
            repository.saveBatch(List.of(new PlayerRecord(first, "renamed", 200, 200)));
            repository.saveBatch(List.of(new PlayerRecord(first, "stale", 120, 120)));
        }
        try (var db = new Database()) {
            db.open(file);
            var repository = new PlayerRepository(db);
            assertEquals(new PlayerRecord(first, "renamed", 100, 200), repository.find(first).orElseThrow());
            assertEquals(".bedrock", repository.find(second).orElseThrow().lastKnownName());
            assertTrue(repository.find(UUID.randomUUID()).isEmpty());
        }
    }
    @Test void batchFailureRollsBackAllRows() throws Exception {
        try (var db = new Database()) {
            db.open(directory.resolve("players.db"));
            try (var statement = db.connection().createStatement()) {
                statement.execute("""
                    CREATE TRIGGER reject_name BEFORE INSERT ON players
                    WHEN NEW.last_known_name = 'reject'
                    BEGIN SELECT RAISE(ABORT, 'test failure'); END
                    """);
            }
            var repository = new PlayerRepository(db);
            UUID accepted = UUID.randomUUID();
            assertThrows(SQLException.class, () -> repository.saveBatch(List.of(
                    new PlayerRecord(accepted, "accepted", 1, 1),
                    new PlayerRecord(UUID.randomUUID(), "reject", 1, 1))));
            assertTrue(repository.find(accepted).isEmpty());
            repository.saveBatch(List.of(new PlayerRecord(accepted, "'; DROP TABLE players; --", 1, 2)));
            assertTrue(repository.find(accepted).isPresent());
        }
    }
    @Test void rejectsFutureSchemaWithoutChangingVersion() throws Exception {
        Path file = directory.resolve("future.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement()) { statement.execute("PRAGMA user_version=99"); }
        try (var db = new Database()) { assertThrows(SQLException.class, () -> db.open(file)); }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA user_version")) {
            assertEquals(99, result.getInt(1));
        }
    }
}
