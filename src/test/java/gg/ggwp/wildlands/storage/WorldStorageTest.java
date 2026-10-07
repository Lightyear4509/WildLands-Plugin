package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldStorageTest {
    @TempDir Path directory;
    @Test void failedIdentityWritesRemainPendingAndRetryWithoutChangingFrozenProfile() throws Exception {
        Path file = directory.resolve("worlds.db");
        var storage = new StorageService(Logger.getAnonymousLogger());
        var reserved = new WorldRecord("rainforest", null, 4509, new WorldgenSettings(1, 63, .8, true, true));
        var identified = reserved.withUuid(UUID.randomUUID());
        try {
            storage.submit(() -> { storage.open(file); storage.reserveWorld(reserved); return null; }).get(5, TimeUnit.SECONDS);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER reject_world_id BEFORE UPDATE ON wildlands_worlds BEGIN SELECT RAISE(ABORT,'test'); END");
                assertThrows(Exception.class, () -> storage.submit(() -> { storage.identifyWorld(identified); return null; }).get(5, TimeUnit.SECONDS));
                assertEquals("WRITE_FAILED", storage.health()); assertEquals(1, storage.pendingCount());
                statement.execute("DROP TRIGGER reject_world_id");
            }
            storage.submit(() -> { storage.flushWorldIdentities(); return null; }).get(5, TimeUnit.SECONDS);
            assertEquals("OK", storage.health()); assertEquals(0, storage.pendingCount());
            assertEquals(identified, storage.submit(storage::worlds).get(5, TimeUnit.SECONDS).getFirst());
        } finally { storage.close(); }
    }
}
