package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.storage.ExpeditionRecord;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExplorationRulesTest {
    @Test void compassBearingsHandleAllOctantsAndArrival() {
        assertEquals("N", ExplorationRules.bearing(0, -10)); assertEquals("S", ExplorationRules.bearing(0, 10));
        assertEquals("E", ExplorationRules.bearing(10, 0)); assertEquals("W", ExplorationRules.bearing(-10, 0));
        assertEquals("NW", ExplorationRules.bearing(-10, -10)); assertEquals("SE", ExplorationRules.bearing(10, 10));
        assertEquals("HERE", ExplorationRules.bearing(0, 0));
        assertThrows(IllegalArgumentException.class, () -> ExplorationRules.bearing(Double.NaN, 0));
    }
    @Test void onlyReturningFromAnActualExpeditionCountsAndProgressNeverRegresses() {
        var record = ExpeditionRecord.initial(UUID.randomUUID());
        record = ExplorationRules.advance(record, 10, 0, 256, 32); assertEquals(0, record.completedTrips());
        record = ExplorationRules.advance(record, 1024, 10, 256, 32); assertTrue(record.active()); assertEquals(0, record.completedTrips());
        record = ExplorationRules.advance(record, 200, 10, 256, 32); assertTrue(record.active());
        record = ExplorationRules.advance(record, 32, 10, 256, 32); assertEquals(1, record.completedTrips()); assertFalse(record.active());
        assertEquals(ExpeditionRecord.Rank.PATHFINDER, record.rank()); assertEquals(1024, record.longestDistance());
        record = ExplorationRules.advance(record, 0, 0, 256, 32); assertEquals(1, record.completedTrips()); assertEquals(ExpeditionRecord.Rank.PATHFINDER, record.rank());
        assertThrows(IllegalArgumentException.class, () -> ExplorationRules.advance(ExpeditionRecord.initial(UUID.randomUUID()), -1, 0, 256, 32));
    }
}
