package gg.ggwp.wildlands.items;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.survival.WaterQuality;
import org.junit.jupiter.api.Test;

class StationRulesTest {
    @Test void saltNeverPurifiesAndBasicFilterStillNeedsBoiling() {
        for (boolean improved : new boolean[]{false, true}) assertTrue(StationRules.filter(WaterQuality.SALT, improved).isEmpty());
        assertEquals(WaterQuality.QUESTIONABLE, StationRules.filter(WaterQuality.CONTAMINATED, false).orElseThrow());
        assertEquals(WaterQuality.UNTREATED, StationRules.filter(WaterQuality.QUESTIONABLE, false).orElseThrow());
        assertTrue(StationRules.filter(WaterQuality.UNTREATED, false).isEmpty());
        for (var quality : new WaterQuality[]{WaterQuality.CONTAMINATED, WaterQuality.QUESTIONABLE, WaterQuality.UNTREATED})
            assertEquals(WaterQuality.CLEAN, StationRules.filter(quality, true).orElseThrow());
        assertTrue(StationRules.filter(WaterQuality.CLEAN, true).isEmpty());
    }
    @Test void cloakReducesOnlyRainGainAndDryBiomesDoNotCollectRain() {
        assertEquals(23.5, StationRules.cloakWetness(20, 30, true, false, 0.35));
        assertEquals(30, StationRules.cloakWetness(20, 30, true, true, 0.35));
        assertEquals(10, StationRules.cloakWetness(20, 10, true, false, 0.35));
        assertEquals(30, StationRules.cloakWetness(20, 30, false, false, 0.35));
        assertFalse(StationRules.rainBiome("savanna")); assertFalse(StationRules.rainBiome("desert"));
        assertTrue(StationRules.rainBiome("jungle"));
    }
}
