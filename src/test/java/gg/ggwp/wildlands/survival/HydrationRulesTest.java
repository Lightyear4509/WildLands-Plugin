package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.HydrationSettings;
import gg.ggwp.wildlands.storage.HydrationRecord;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HydrationRulesTest {
    private YamlConfiguration yaml() throws Exception {
        var yaml = new YamlConfiguration();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/hydration.yml"), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }
        return yaml;
    }
    private HydrationSettings settings() throws Exception { return HydrationSettings.parse(yaml()); }
    private HydrationRecord state(double hydration, double dry) {
        return new HydrationRecord(UUID.randomUUID(), hydration, true, dry);
    }

    @Test void springsRequireUnambiguousValidLocations() throws Exception {
        var yaml = yaml();
        var location = java.util.Map.of("world", "world", "x", 10, "y", 70, "z", 20);
        yaml.set("springs", java.util.List.of(location));
        assertTrue(HydrationSettings.parse(yaml).springs().contains(new HydrationSettings.Spring("world", 10, 70, 20)));
        yaml.set("springs", java.util.List.of(location, location));
        assertThrows(IllegalArgumentException.class, () -> HydrationSettings.parse(yaml));
        yaml.set("springs", java.util.List.of(java.util.Map.of("world", "world", "x", "10", "y", 70, "z", 20)));
        assertThrows(IllegalArgumentException.class, () -> HydrationSettings.parse(yaml));
    }

    @Test void normalLossIsSlowAndConcurrentActivitiesDoNotMultiply() throws Exception {
        var settings = settings();
        var resting = HydrationRules.advance(state(100, 0), settings, 5, false, false, false, 1);
        var active = HydrationRules.advance(state(100, 0), settings, 5, true, true, true, 1);
        assertEquals(99.875, resting.state().hydration());
        assertEquals(99.75, active.state().hydration());
        assertFalse(active.damageDue());
    }

    @Test void onlyTimeActuallySpentSeverelyDehydratedCountsTowardDanger() throws Exception {
        var step = HydrationRules.advance(state(0.1, 0), settings(), 5, false, false, false, 1);
        assertEquals(0, step.state().hydration());
        assertEquals(1, step.state().drySeconds(), 0.000001);
        assertFalse(step.damageDue());
    }

    @Test void damageIsDelayedAndOccursOnlyWhenCrossingAnInterval() throws Exception {
        var settings = settings();
        var first = HydrationRules.advance(state(0, 119), settings, 5, false, false, false, 1);
        assertTrue(first.damageDue());
        var second = HydrationRules.advance(first.state(), settings, 5, false, false, false, 1);
        assertFalse(second.damageDue());
        var third = HydrationRules.advance(state(0, 149), settings, 5, false, false, false, 1);
        assertTrue(third.damageDue());
        assertEquals(0, HydrationRules.drink(first.state(), WaterQuality.CLEAN, settings).drySeconds());
    }

    @Test void drinksClampAndUnsafeWaterIsProbabilistic() throws Exception {
        var settings = settings();
        assertEquals(100, HydrationRules.drink(state(95, 0), WaterQuality.CLEAN, settings).hydration());
        assertEquals(0, HydrationRules.drink(state(5, 0), WaterQuality.SALT, settings).hydration());
        assertFalse(HydrationRules.illness(WaterQuality.CLEAN, settings, 0));
        assertTrue(HydrationRules.illness(WaterQuality.UNTREATED, settings, 0.05));
        assertFalse(HydrationRules.illness(WaterQuality.UNTREATED, settings, 0.5));
    }

    @Test void invalidConfigurationCannotCreateImpossibleOrPunitiveStates() throws Exception {
        for (Object invalid : new Object[] {Double.NaN, Double.POSITIVE_INFINITY, -1, "0.025"}) {
            var yaml = yaml();
            yaml.set("depletion.per-second", invalid);
            assertThrows(IllegalArgumentException.class, () -> HydrationSettings.parse(yaml));
        }
        var yaml = yaml();
        yaml.set("water.salt.restore", 20);
        assertThrows(IllegalArgumentException.class, () -> HydrationSettings.parse(yaml));
        yaml.set("water.salt.restore", -8);
        yaml.set("dehydration.severe-at", 30);
        assertThrows(IllegalArgumentException.class, () -> HydrationSettings.parse(yaml));
    }
}
