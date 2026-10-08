package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WildlifeStorageTest {
    @TempDir Path directory;
    @Test void deathReplacesFailedRegistrationAndRetryDoesNotResurrectAnimal() throws Exception {
        Path file = directory.resolve("wildlife.db"); var storage = new StorageService(Logger.getAnonymousLogger());
        var record = new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), -1, 65, -1, true);
        try {
            storage.submit(() -> { storage.open(file); return null; }).get(5, TimeUnit.SECONDS);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER reject_wildlife BEFORE INSERT ON wildlife BEGIN SELECT RAISE(ABORT,'test'); END");
                assertThrows(Exception.class, () -> storage.submit(() -> { storage.saveWildlife(List.of(record)); return null; }).get(5, TimeUnit.SECONDS));
                assertEquals("WRITE_FAILED", storage.health()); assertEquals(1, storage.pendingCount());
                assertThrows(Exception.class, () -> storage.submit(() -> { storage.saveWildlife(List.of(record.dead())); return null; }).get(5, TimeUnit.SECONDS));
                statement.execute("DROP TRIGGER reject_wildlife");
            }
            storage.submit(() -> { storage.saveWildlife(List.of()); return null; }).get(5, TimeUnit.SECONDS);
            assertEquals("OK", storage.health()); assertEquals(0, storage.pendingCount());
            assertTrue(storage.submit(storage::wildlife).get(5, TimeUnit.SECONDS).isEmpty());
        } finally { storage.close(); }
    }
}
