package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.wildlife.Species;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WildlifeSpeciesTest {
    @TempDir Path directory;
    @Test void migrationDefaultsExistingJaguarWithoutChangingItsUuidWorldHomeOrLife() throws Exception {
        Path file = directory.resolve("old.db"); var record = new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), 12, 76, -34, true);
        try (var db = new Database()) {
            db.open(file); new WildlifeRepository(db).saveBatch(List.of(record));
            try (var sql = db.connection().createStatement()) { sql.execute("ALTER TABLE wildlife DROP COLUMN species"); sql.execute("PRAGMA user_version=7"); }
        }
        try (var db = new Database()) {
            db.open(file); assertEquals(List.of(record), new WildlifeRepository(db).living());
            try (var sql = db.connection().createStatement(); var row = sql.executeQuery("PRAGMA user_version")) { assertEquals(8, row.getInt(1)); }
        }
    }
    @Test void everySpeciesSurvivesReopenAndDeathPreservesSpeciesIdentity() throws Exception {
        Path file = directory.resolve("all.db"); var records = new ArrayList<WildlifeRecord>();
        for (var species : Species.values()) records.add(new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), 1, 65, 2, true, species));
        try (var db = new Database()) { db.open(file); new WildlifeRepository(db).saveBatch(records); }
        try (var db = new Database()) {
            db.open(file); assertEquals(new HashSet<>(records), new HashSet<>(new WildlifeRepository(db).living()));
            var dead = records.get(1).dead(); assertEquals(Species.CAPYBARA, dead.species());
            new WildlifeRepository(db).saveBatch(List.of(dead)); assertEquals(2, new WildlifeRepository(db).living().size());
        }
    }
    @Test void unknownPersistedSpeciesFailsClosedInsteadOfBecomingDifferentAnimal() throws Exception {
        try (var db = new Database()) {
            db.open(directory.resolve("invalid.db")); var record = new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), 0, 65, 0, true);
            new WildlifeRepository(db).saveBatch(List.of(record));
            try (var sql = db.connection().createStatement()) { sql.execute("UPDATE wildlife SET species='UNKNOWN'"); }
            assertThrows(SQLException.class, () -> new WildlifeRepository(db).living());
        }
    }
}
