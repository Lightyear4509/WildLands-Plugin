package gg.ggwp.wildlands.wildlife.behaviors;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.*;
import org.junit.jupiter.api.Test;

class JaguarBehaviorTest {
    private final Limits limits = new Limits(20, 8, 6, 40, 5, 20, 30, 3);
    private Observation player(double distance, boolean night) { return new Observation(distance, 0, 1, false, night, false, true, false); }
    @Test void playersReceiveWarningBeforeAttackAndCanBackAway() {
        var memory = JaguarBehavior.step(Memory.idle(), player(4, true), limits, 1);
        assertEquals(Phase.WARNING, memory.phase());
        for (int second = 0; second < 4; second++) {
            memory = JaguarBehavior.step(memory, player(4, true), limits, 1); assertEquals(Phase.WARNING, memory.phase());
        }
        assertEquals(Phase.ATTACKING, JaguarBehavior.step(memory, player(4, true), limits, 1).phase());
        assertEquals(Phase.RETREATING, JaguarBehavior.step(memory, player(9, true), limits, 1).phase());
    }
    @Test void daylightIsNeutralAndNightStalkingAndPursuitAreBounded() {
        assertEquals(Phase.IDLE, JaguarBehavior.step(Memory.idle(), player(15, false), limits, 1).phase());
        assertEquals(Phase.STALKING, JaguarBehavior.step(Memory.idle(), player(15, true), limits, 1).phase());
        assertEquals(Phase.RETREATING, JaguarBehavior.step(new Memory(Phase.STALKING, 19), player(15, true), limits, 1).phase());
        assertEquals(Phase.RETREATING, JaguarBehavior.step(new Memory(Phase.ATTACKING, 19), player(4, true), limits, 1).phase());
        assertEquals(Phase.RETREATING, JaguarBehavior.step(new Memory(Phase.ATTACKING, 2), player(21, true), limits, 1).phase());
    }
    @Test void fireGroupsTerritoryAndLineOfSightStopPursuitAndRetreatHasCooldown() {
        for (var observation : new Observation[]{
                new Observation(4, 0, 1, true, true, false, true, false),
                new Observation(4, 0, 3, false, true, false, true, false),
                new Observation(4, 41, 1, false, true, false, true, false),
                new Observation(4, 0, 1, false, true, false, false, false)})
            assertEquals(Phase.RETREATING, JaguarBehavior.step(new Memory(Phase.ATTACKING, 2), observation, limits, 1).phase());
        assertEquals(Phase.RETREATING, JaguarBehavior.step(new Memory(Phase.RETREATING, 2), player(4, true), limits, 1).phase());
        assertEquals(Phase.IDLE, JaguarBehavior.step(new Memory(Phase.RETREATING, 29), player(4, true), limits, 1).phase());
    }
    @Test void preyHuntingDoesNotRequireHumanWarningAndLimitsRejectInvalidValues() {
        var prey = new Observation(6, 0, 0, false, true, false, true, true);
        assertEquals(Phase.ATTACKING, JaguarBehavior.step(Memory.idle(), prey, limits, 1).phase());
        assertThrows(IllegalArgumentException.class, () -> new Limits(100, 8, 6, 40, 5, 20, 30, 3));
        assertThrows(IllegalArgumentException.class, () -> JaguarBehavior.step(Memory.idle(), prey, limits, 0));
    }
}
