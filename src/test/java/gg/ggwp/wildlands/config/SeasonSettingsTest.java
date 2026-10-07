package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.seasons.Season;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SeasonSettingsTest {
    @TempDir Path directory;

    @Test void bundledProfilesCoverTheCycleAndKeepScopeExplicit() throws Exception {
        var settings = new ConfigurationManager(directory).load().seasons();
        assertEquals(5, settings.profiles().size());
        assertEquals(14 * 24000L, settings.durations().get(Season.DRY));
        assertTrue(settings.worlds().contains("world"));
        assertTrue(settings.profiles().get(Season.DRY).rainChance() < settings.profiles().get(Season.MONSOON).rainChance());
        assertThrows(UnsupportedOperationException.class, () -> settings.worlds().add("other"));
    }

    @Test void badProfilesAndWorldListsFailRatherThanSilentlyDefaulting() throws Exception {
        new ConfigurationManager(directory).load();
        for (Object[] change : new Object[][] {
                {"worlds", List.of("world", "world")}, {"worlds", List.of()}, {"clock-seconds", 0},
                {"seasons.dry.days", 1.5}, {"seasons.dry.rain-chance", Double.NaN},
                {"seasons.dry.thunder-chance", 1.1}, {"seasons.dry.crop-multiplier", -1},
                {"seasons.dry.temperature-delta", "3"}, {"seasons.winter.days", 5}}) {
            var yaml = new YamlConfiguration();
            yaml.load(directory.resolve("seasons.yml").toFile());
            yaml.set((String) change[0], change[1]);
            assertThrows(IllegalArgumentException.class, () -> SeasonSettings.parse(yaml), change[0].toString());
        }
    }
}
