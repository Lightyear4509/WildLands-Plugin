package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.wildlife.Species;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WildlifeExpansionSettingsTest {
    @TempDir Path directory;
    @Test void defaultsEnableSmallGroupsAndBoundedForagingWithoutExtraSchedulers() throws Exception {
        var config = new ConfigurationManager(directory).load();
        assertTrue(config.expansion().animal(Species.CAPYBARA).enabled());
        assertEquals(4, config.expansion().animal(Species.CAPYBARA).maxPerCell());
        assertEquals(8, config.expansion().animal(Species.TAPIR).maxPerWorld());
        assertEquals(12, config.visuals().maxAnimals()); assertEquals(6, config.visuals().animationTicks()); assertEquals(32, config.visuals().viewDistance());
    }
    @Test void invalidTypesAndUnboundedSettingsAreRejected() throws Exception {
        new ConfigurationManager(directory).load();
        for (Object[] change : new Object[][]{{"capybara.enabled", "true"}, {"capybara.max-per-cell", 5}, {"tapir.health", Double.NaN}, {"tapir.roam-radius", 1000}, {"tapir.flee-seconds", 0}}) {
            var yaml = new YamlConfiguration(); yaml.load(directory.resolve("wildlife-expansion.yml").toFile());
            yaml.set((String) change[0], change[1]); assertThrows(IllegalArgumentException.class, () -> WildlifeExpansionSettings.parse(yaml));
        }
        var visual = new YamlConfiguration(); visual.load(directory.resolve("wildlife-visuals.yml").toFile());
        visual.set("animate", "false"); assertThrows(IllegalArgumentException.class, () -> VisualSettings.parse(visual));
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "", "", 12, 1, 32, true));
    }
    @Test void oldFilesDoNotSilentlyEnableNewFaunaAndPartialPredatorSettingsFail() throws Exception {
        new ConfigurationManager(directory).load(); var yaml = new YamlConfiguration(); yaml.load(directory.resolve("wildlife-expansion.yml").toFile());
        for(String species : new String[]{"toucan","monkey","frog","caiman"}) yaml.set(species,null);
        yaml.set("jaguar-max-per-world",null); var old=WildlifeExpansionSettings.parse(yaml);
        assertFalse(old.animal(Species.TOUCAN).enabled()); assertFalse(old.animal(Species.CAIMAN).enabled()); assertEquals(32,old.jaguarMaxPerWorld());
        yaml.load(directory.resolve("wildlife-expansion.yml").toFile()); yaml.set("caiman.detection-range",null); yaml.set("caiman.damage",100);
        assertThrows(IllegalArgumentException.class,()->WildlifeExpansionSettings.parse(yaml));
        yaml.load(directory.resolve("wildlife-expansion.yml").toFile());
        for(String key : new String[]{"detection-range","warning-range","engagement-range","warning-seconds","chase-seconds","damage","bite-range","bite-seconds","fire-radius","deterrent-group-size"}) yaml.set("caiman."+key,null);
        assertThrows(IllegalArgumentException.class,()->WildlifeExpansionSettings.parse(yaml));
    }
}
