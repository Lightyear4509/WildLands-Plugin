package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WildlifeSettingsTest {
    private YamlConfiguration defaults() throws Exception {
        var yaml = new YamlConfiguration();
        try (var input = new InputStreamReader(getClass().getResourceAsStream("/wildlife.yml"), StandardCharsets.UTF_8)) { yaml.load(input); }
        return yaml;
    }
    @Test void defaultSpeciesIsBoundedAndConfigRejectsInvalidProbabilitiesWorldsAndOrdering() throws Exception {
        var yaml = defaults(); var profile = WildlifeSettings.parse(yaml);
        assertEquals(20, profile.health()); assertEquals(20, profile.limits().detectionRange()); assertEquals(8, profile.batchSize());
        for (Object[] change : new Object[][]{{"spawn-chance", 2}, {"worlds", java.util.List.of("wildlands", "wildlands")},
                {"jaguar.warning-range", 2}, {"sample-ticks", "10"}, {"jaguar.hunt-prey", "yes"}, {"jaguar.damage", Double.NaN}}) {
            var candidate = defaults(); candidate.set((String) change[0], change[1]);
            assertThrows(IllegalArgumentException.class, () -> WildlifeSettings.parse(candidate));
        }
    }
}
