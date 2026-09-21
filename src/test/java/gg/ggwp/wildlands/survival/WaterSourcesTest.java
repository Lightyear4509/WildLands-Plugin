package gg.ggwp.wildlands.survival;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterSourcesTest {
    @Test void distinguishesFreshSaltAndContaminatedSources() {
        assertEquals(WaterQuality.CLEAN, WaterSources.classify("minecraft:jungle", true));
        assertEquals(WaterQuality.UNTREATED, WaterSources.classify("minecraft:river", false));
        assertEquals(WaterQuality.QUESTIONABLE, WaterSources.classify("minecraft:jungle", false));
        assertEquals(WaterQuality.CONTAMINATED, WaterSources.classify("minecraft:mangrove_swamp", false));
        for (String biome : new String[]{"ocean", "warm_ocean", "deep_cold_ocean", "frozen_ocean"}) {
            assertEquals(WaterQuality.SALT, WaterSources.classify("minecraft:" + biome, false));
            assertEquals(WaterQuality.SALT, WaterSources.classify("minecraft:" + biome, true));
        }
        assertEquals(WaterQuality.QUESTIONABLE, WaterSources.classify("custom:unknown", false));
    }
}
