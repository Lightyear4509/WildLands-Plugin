package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WorldgenSettingsTest {
    @Test void frozenProfileRoundTripsAndInvalidTypesFail() {
        var profile = new WorldgenSettings(1, 63, .8, true, true);
        var yaml = new YamlConfiguration(); profile.write(yaml);
        assertEquals(profile, WorldgenSettings.parse(yaml));
        yaml.set("caves", "true");
        assertThrows(IllegalArgumentException.class, () -> WorldgenSettings.parse(yaml));
        profile.write(yaml); yaml.set("sea-level", 200);
        assertThrows(IllegalArgumentException.class, () -> WorldgenSettings.parse(yaml));
    }
}
