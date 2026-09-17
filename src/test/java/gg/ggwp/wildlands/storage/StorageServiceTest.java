package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageServiceTest {
    @TempDir Path directory;
    @Test void shutdownDrainsQueuedWritesAndRejectsNewWork() throws Exception {
        var storage = new StorageService(Logger.getAnonymousLogger());
        Path file = directory.resolve("players.db");
        UUID id = UUID.randomUUID();
        storage.submit(() -> { storage.open(file); return null; }).get(5, TimeUnit.SECONDS);
        storage.submit(() -> { storage.save(List.of(new PlayerRecord(id, "name", 1, 2))); return null; });
        storage.close();
        assertThrows(ExecutionException.class, () -> storage.submit(() -> 1).get());
        try (var db = new Database()) {
            db.open(file);
            assertTrue(new PlayerRepository(db).find(id).isPresent());
        }
    }
    @Test void failedWritesRemainPendingAndRetryOnNextBatch() throws Exception {
        Path file = directory.resolve("retry.db");
        var storage = new StorageService(Logger.getAnonymousLogger());
        try {
            storage.submit(() -> { storage.open(file); return null; }).get(5, TimeUnit.SECONDS);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER reject_all BEFORE INSERT ON players BEGIN SELECT RAISE(ABORT, 'failure'); END");
            }
            UUID id = UUID.randomUUID();
            assertThrows(ExecutionException.class, () -> storage.submit(() -> {
                storage.save(List.of(new PlayerRecord(id, "name", 1, 1))); return null;
            }).get(5, TimeUnit.SECONDS));
            assertEquals(1, storage.pendingCount());
            assertEquals("WRITE_FAILED", storage.health());
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement()) { statement.execute("DROP TRIGGER reject_all"); }
            storage.submit(() -> { storage.save(List.of()); return null; }).get(5, TimeUnit.SECONDS);
            assertEquals(0, storage.pendingCount());
            assertEquals("OK", storage.health());
            assertTrue(storage.submit(() -> storage.find(id)).get(5, TimeUnit.SECONDS).isPresent());
        } finally { storage.close(); }
    }
}
