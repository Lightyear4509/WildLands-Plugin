package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LandmarkSettingsTest {
    @TempDir Path directory;
    @Test void validatesSamplingCapsDistancesAndCoordinatesAndUpgradesRemainOptIn() throws Exception {
        var snapshot = new ConfigurationManager(directory).load(); assertTrue(snapshot.landmarks().showCoordinates());
        var yaml = new YamlConfiguration(); yaml.load(directory.resolve("landmarks.yml").toFile());
        for (Object[] change : new Object[][]{{"sample-seconds", 0}, {"batch-size", 999}, {"discovery-radius", -1},
                {"max-natural-per-world", 99999}, {"expedition.depart-distance", 1}, {"show-coordinates", "true"}}) {
            var copy = new YamlConfiguration(); copy.loadFromString(yaml.saveToString()); copy.set((String) change[0], change[1]);
            assertThrows(IllegalArgumentException.class, () -> LandmarkSettings.parse(copy));
        }
        var config = new YamlConfiguration(); config.load(directory.resolve("config.yml").toFile()); config.set("modules.landmarks", null);
        assertFalse(Settings.parse(config).modules().get("landmarks")); assertEquals(2, snapshot.worldgen().version());
    }
}
