package gg.ggwp.wildlands.ui;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.seasons.Season;
import org.junit.jupiter.api.Test;

class HudFormatterTest {
    @Test void allSeasonsAndLongRoutesStayBoundedWithoutHidingCoreState() {
        for (var season : Season.values()) {
            String text = HudFormatter.format(100.0, -100.0, 100.0, season, "NE 60000000m");
            assertTrue(text.length() <= HudFormatter.MAX_LENGTH);
            assertTrue(text.contains("Hydration 100%")); assertTrue(text.contains("Temp -100°C"));
            assertTrue(text.contains("Wet 100%")); assertTrue(text.contains("Route NE"));
        }
        assertTrue(HudFormatter.format(10.0, 30.0, 50.0, Season.MONSOON, "X".repeat(100)).length() <= HudFormatter.MAX_LENGTH);
    }
    @Test void disabledFieldsStayAbsentAndCrossWorldRoutesAreCompact() {
        assertEquals("Temp 24°C | Wet 15% | Season To wet | Route other world",
                HudFormatter.format(null, 24.0, 15.0, Season.TRANSITION_TO_WET, "In very_long_world_name; travel to that world first."));
        assertEquals("Hydration 82%", HudFormatter.format(82.0, null, null, null, null));
        assertEquals("", HudFormatter.format(null, null, null, null, null));
    }
}
