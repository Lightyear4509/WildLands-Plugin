package gg.ggwp.wildlands.seasons;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeasonClockTest {
    private Map<Season, Long> durations() {
        var durations = new EnumMap<Season, Long>(Season.class);
        long length = 10;
        for (Season season : Season.values()) durations.put(season, length++);
        return durations;
    }

    @Test void everyBoundaryAdvancesInOrderAndWraps() {
        var durations = durations();
        for (Season season : Season.values()) {
            assertEquals(new SeasonClock.State(season, durations.get(season) - 1),
                    SeasonClock.advance(new SeasonClock.State(season, 0), durations.get(season) - 1, durations));
            assertEquals(new SeasonClock.State(season.next(), 0),
                    SeasonClock.advance(new SeasonClock.State(season, 0), durations.get(season), durations));
        }
    }

    @Test void largeIncrementsAndChangedDurationsNormalizeWithoutOverflow() {
        var durations = durations(); // Total cycle: 60 ticks.
        assertEquals(new SeasonClock.State(Season.DRY, 7),
                SeasonClock.advance(new SeasonClock.State(Season.DRY, 0), Long.MAX_VALUE, durations));
        assertEquals(new SeasonClock.State(Season.TRANSITION_TO_WET, 0),
                SeasonClock.advance(new SeasonClock.State(Season.DRY, 70), 0, durations));
        assertEquals(new SeasonClock.State(Season.WET, 3),
                SeasonClock.advance(new SeasonClock.State(Season.WET, 3), 60, durations));
    }

    @Test void invalidDurationsAndNegativeTimeAreRejected() {
        var state = new SeasonClock.State(Season.DRY, 0);
        assertThrows(IllegalArgumentException.class, () -> SeasonClock.advance(state, -1, durations()));
        assertThrows(IllegalArgumentException.class, () -> SeasonClock.advance(state, 0, Map.of()));
        var invalid = durations(); invalid.put(Season.WET, 0L);
        assertThrows(IllegalArgumentException.class, () -> SeasonClock.advance(state, 0, invalid));
    }
}
