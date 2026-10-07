package gg.ggwp.wildlands.survival;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.config.EnvironmentSettings;
import gg.ggwp.wildlands.storage.EnvironmentRecord;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnvironmentRulesTest {
    private static final EnvironmentSettings SETTINGS = new EnvironmentSettings(5, 26, 63, .04, 3, 4, 2,
            8, 6, 31, 12, 8, 1.35, 8, 18, 2, 2, 12, 3, 2, .75, 3, 2);
    private static final ShelterStatus EXPOSED = new ShelterStatus(false, false, true, false);
    @Test void sprintWarmthCanPushWarmConditionsAcrossHeatThreshold() {
        assertEquals(0, EnvironmentRules.activityAdjustment(SETTINGS, false));
        double warm = EnvironmentRules.temperature(SETTINGS, 30, 63, false, false, EXPOSED);
        double active = EnvironmentRules.temperature(SETTINGS,
                30 + EnvironmentRules.activityAdjustment(SETTINGS, true), 63, false, false, EXPOSED);
        assertEquals(1, EnvironmentRules.hydrationMultiplier(new EnvironmentRecord(UUID.randomUUID(), warm, 0), SETTINGS));
        assertEquals(1.35, EnvironmentRules.hydrationMultiplier(new EnvironmentRecord(UUID.randomUUID(), active, 0), SETTINGS));
    }
    @Test void dailyTemperaturePeaksAtNoonAndWrapsAcrossDays() {
        assertEquals(3, EnvironmentRules.dailyAdjustment(SETTINGS, 6000, true), 0.000001);
        assertEquals(-3, EnvironmentRules.dailyAdjustment(SETTINGS, 18000, true), 0.000001);
        assertEquals(3, EnvironmentRules.dailyAdjustment(SETTINGS, 30000, true), 0.000001);
        assertEquals(-3, EnvironmentRules.dailyAdjustment(SETTINGS, -6000, true), 0.000001);
        assertEquals(0, EnvironmentRules.dailyAdjustment(SETTINGS, 6000, false));
        for (int ticks = 0; ticks < 24000; ticks += 100)
            assertTrue(Math.abs(EnvironmentRules.dailyAdjustment(SETTINGS, ticks, true)) <= 3);
    }
    @Test void temperatureCombinesBoundedLocalFactors() {
        assertEquals(26, EnvironmentRules.temperature(SETTINGS, 26, 63, false, false, EXPOSED));
        assertEquals(19, EnvironmentRules.temperature(SETTINGS, 26, 63, true, true, EXPOSED));
        assertEquals(32, EnvironmentRules.temperature(SETTINGS, 26, 63, false, false,
                new ShelterStatus(true, true, true, true)));
        assertEquals(22, EnvironmentRules.temperature(SETTINGS, 26, 163, false, false, EXPOSED));
    }
    @Test void wetnessRisesFromExposureAndDriesFasterUnderShelter() {
        var state = new EnvironmentRecord(UUID.randomUUID(), 26, 10);
        assertEquals(50, EnvironmentRules.wetness(state, SETTINGS, 5, true, false, EXPOSED).wetness());
        assertEquals(30, EnvironmentRules.wetness(new EnvironmentRecord(state.uuid(), 26, 50), SETTINGS, 5,
                false, false, new ShelterStatus(true, false, true, false)).wetness());
        assertEquals(100, EnvironmentRules.wetness(new EnvironmentRecord(state.uuid(), 26, 90), SETTINGS, 5,
                false, true, EXPOSED).wetness());
    }
    @Test void temperatureOnlyIncreasesHydrationLossWhenHot() {
        assertEquals(1, EnvironmentRules.hydrationMultiplier(new EnvironmentRecord(UUID.randomUUID(), 30, 0), SETTINGS));
        assertEquals(1.35, EnvironmentRules.hydrationMultiplier(new EnvironmentRecord(UUID.randomUUID(), 31, 0), SETTINGS));
        assertTrue(EnvironmentRules.coldWet(new EnvironmentRecord(UUID.randomUUID(), 8, 50), SETTINGS));
    }
}
