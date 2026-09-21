package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.storage.HydrationRecord;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HydrationStateTest {
    @Test void rejectsInvalidPersistentState() {
        UUID id = UUID.randomUUID();
        for (double value : new double[] {-1, 101, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new HydrationRecord(id, value, true, 0));
        }
        for (double duration : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new HydrationRecord(id, 50, true, duration));
        }
        assertThrows(NullPointerException.class, () -> new HydrationRecord(null, 50, true, 0));
    }

    @Test void hudPreferencePreservesSurvivalStateAndIdentity() {
        HydrationRecord original = new HydrationRecord(UUID.randomUUID(), 0, true, 125);
        HydrationRecord hidden = original.withHud(false);
        assertEquals(original.uuid(), hidden.uuid());
        assertEquals(original.hydration(), hidden.hydration());
        assertEquals(original.drySeconds(), hidden.drySeconds());
        assertFalse(hidden.hudEnabled());
        assertTrue(original.hudEnabled());
    }

    @Test void boilingTreatsFreshWaterButCannotDesalinate() {
        for (WaterQuality quality : WaterQuality.values()) {
            assertEquals(quality == WaterQuality.SALT ? WaterQuality.SALT : WaterQuality.CLEAN,
                    quality.boiled());
        }
        assertFalse(WaterQuality.SALT.canBoil());
    }

    @Test void mixingNeverLaundersUnsafeWaterAndIsOrderIndependent() {
        for (WaterQuality a : WaterQuality.values()) {
            for (WaterQuality b : WaterQuality.values()) {
                assertEquals(a.mix(b), b.mix(a));
                assertTrue(a.mix(b).ordinal() >= a.ordinal());
                assertTrue(a.mix(b).ordinal() >= b.ordinal());
            }
        }
    }
}
