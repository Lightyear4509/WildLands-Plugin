package gg.ggwp.wildlands.storage;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldRepositoryTest {
    @TempDir Path directory;
    @Test void reservationIdentityAndFrozenProfileSurviveReopen() throws Exception {
        Path file = directory.resolve("worlds.db");
        var reserved = new WorldRecord("rainforest", null, Long.MIN_VALUE, new WorldgenSettings(1, 63, .8, true, true));
        var identified = reserved.withUuid(UUID.randomUUID());
        try (var db = new Database()) {
            db.open(file); var repo = new WorldRepository(db); repo.reserve(reserved);
            assertEquals(reserved, repo.all().getFirst());
            assertThrows(SQLException.class, () -> repo.reserve(reserved));
            repo.identify(identified);
            assertThrows(SQLException.class, () -> repo.identify(reserved.withUuid(UUID.randomUUID())));
        }
        try (var db = new Database()) {
            db.open(file); assertEquals(identified, new WorldRepository(db).all().getFirst());
        }
    }
    @Test void unsafeNamesAndUnsupportedProfilesAreRejected() {
        var profile = new WorldgenSettings(1, 63, .8, true, true);
        for (String name : new String[]{"../world", "world/child", "CON", "con", "", "with space", "MixedCase", "overworld", "the_nether", "the_end"})
            assertThrows(IllegalArgumentException.class, () -> new WorldRecord(name, null, 1, profile));
        assertThrows(IllegalArgumentException.class, () -> new WorldgenSettings(2, 63, .8, true, true));
        assertThrows(IllegalArgumentException.class, () -> new WorldgenSettings(1, 63, Double.NaN, true, true));
    }
}
