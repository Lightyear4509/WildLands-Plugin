package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WildlifeRepositoryTest {
    @TempDir Path directory;
    @Test void uuidHomesSurviveRestartAndDeathRemovesPopulationEntry() throws Exception {
        Path file = directory.resolve("wildlife.db");
        var record = new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), -128.5, 65, 12.5, true);
        try (var db = new Database()) { db.open(file); new WildlifeRepository(db).saveBatch(List.of(record)); }
        try (var db = new Database()) {
            db.open(file); var repo = new WildlifeRepository(db); assertEquals(List.of(record), repo.living());
            repo.saveBatch(List.of(record.dead())); assertTrue(repo.living().isEmpty());
            try (var statement = db.connection().createStatement(); var rows = statement.executeQuery("SELECT count(*) FROM wildlife")) { assertEquals(0, rows.getInt(1)); }
        }
        assertThrows(IllegalArgumentException.class, () -> new WildlifeRecord(record.uuid(), record.worldUuid(), Double.NaN, 65, 12, true));
    }
}
