package gg.ggwp.wildlands.storage;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HydrationStorageTest {
    @TempDir Path directory;

    @Test void failedHydrationIsReadableAndMetadataSuccessDoesNotHideItsFailure() throws Exception {
        Path file = directory.resolve("retry.db");
        StorageService storage = new StorageService(Logger.getAnonymousLogger());
        HydrationRecord state = new HydrationRecord(UUID.randomUUID(), 21, false, 0);
        try {
            storage.submit(() -> { storage.open(file); return null; }).get(5, TimeUnit.SECONDS);
            sql(file, "CREATE TRIGGER reject_hydration BEFORE INSERT ON hydration_players BEGIN SELECT RAISE(ABORT,'failure'); END");
            assertThrows(ExecutionException.class, () -> storage.submit(() -> {
                storage.saveHydration(List.of(state)); return null;
            }).get(5, TimeUnit.SECONDS));
            storage.submit(() -> { storage.save(List.of(new PlayerRecord(state.uuid(), "name", 1, 1))); return null; }).get(5, TimeUnit.SECONDS);
            assertEquals("WRITE_FAILED", storage.health());
            assertEquals(1, storage.pendingCount());
            assertEquals(state, storage.submit(() -> storage.findHydration(state.uuid())).get(5, TimeUnit.SECONDS).orElseThrow());
            sql(file, "DROP TRIGGER reject_hydration");
            // Shutdown must retry pending hydration even without another gameplay save.
        } finally { storage.close(); }
        try (Database db = new Database()) {
            db.open(file);
            assertEquals(state, new HydrationRepository(db).find(state.uuid()).orElseThrow());
        }
    }

    @Test void newerSnapshotReplacesFailedSnapshot() throws Exception {
        Path file = directory.resolve("replace.db");
        StorageService storage = new StorageService(Logger.getAnonymousLogger());
        HydrationRecord first = new HydrationRecord(UUID.randomUUID(), 30, true, 0);
        HydrationRecord next = new HydrationRecord(first.uuid(), 55, false, 0);
        try {
            storage.submit(() -> { storage.open(file); return null; }).get(5, TimeUnit.SECONDS);
            sql(file, "CREATE TRIGGER reject_hydration BEFORE INSERT ON hydration_players BEGIN SELECT RAISE(ABORT,'failure'); END");
            assertThrows(ExecutionException.class, () -> storage.submit(() -> { storage.saveHydration(List.of(first)); return null; }).get());
            sql(file, "DROP TRIGGER reject_hydration");
            storage.submit(() -> { storage.saveHydration(List.of(next)); return null; }).get(5, TimeUnit.SECONDS);
            assertEquals("OK", storage.health());
            assertEquals(0, storage.pendingCount());
            assertEquals(next, storage.submit(() -> storage.findHydration(first.uuid())).get().orElseThrow());
        } finally { storage.close(); }
    }

    private static void sql(Path file, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             Statement statement = connection.createStatement()) { statement.execute(query); }
    }
}
