package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CraftingSettingsTest {
    @TempDir Path directory;
    @Test void validatesBoundsAndExistingInstallsDoNotAutomaticallyEnableNewGameplay() throws Exception {
        var snapshot = new ConfigurationManager(directory).load();
        assertEquals(10, snapshot.crafting().sampleSeconds()); assertEquals(1, snapshot.food().preservedSaturation());
        var yaml = new YamlConfiguration(); yaml.load(directory.resolve("crafting.yml").toFile());
        for (Object[] entry : new Object[][]{{"sample-seconds", 0}, {"batch-size", 1000}, {"max-loaded-stations", 99999},
                {"rain-cloak.multiplier", Double.NaN}, {"improved-stove.fuel-multiplier", -1}}) {
            var copy = new YamlConfiguration(); copy.loadFromString(yaml.saveToString()); copy.set((String) entry[0], entry[1]);
            assertThrows(IllegalArgumentException.class, () -> CraftingSettings.parse(copy));
        }
        var config = new YamlConfiguration(); config.load(directory.resolve("config.yml").toFile());
        config.set("modules.crafting", null); config.set("modules.nutrition", null);
        assertFalse(Settings.parse(config).modules().get("crafting")); assertFalse(Settings.parse(config).modules().get("nutrition"));
    }
    @Test void rejectsNonfiniteFoodBonus() {
        var yaml = new YamlConfiguration(); yaml.set("schema-version", 1); yaml.set("preserved-saturation", Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> FoodSettings.parse(yaml));
    }
}
