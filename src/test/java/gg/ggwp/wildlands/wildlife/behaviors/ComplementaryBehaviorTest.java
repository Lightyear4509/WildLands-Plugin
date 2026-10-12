package gg.ggwp.wildlands.wildlife.behaviors;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.config.*;
import gg.ggwp.wildlands.wildlife.Species;
import org.junit.jupiter.api.Test;

class ComplementaryBehaviorTest {
    private PassiveBehavior.Observation safe(boolean night, boolean swimming, boolean group) { return new PassiveBehavior.Observation(20, 0, false, swimming, group, swimming, night, false); }
    @Test void toucanFlitsInDayAndPerchesAtNight() {
        var config = WildlifeExpansionSettings.disabled().animal(Species.TOUCAN);
        assertEquals(PassiveBehavior.Phase.FLITTING, PassiveBehavior.step(Species.TOUCAN, PassiveBehavior.Memory.idle(), safe(false,false,false), config, 1).phase());
        assertEquals(PassiveBehavior.Phase.PERCHING, PassiveBehavior.step(Species.TOUCAN, PassiveBehavior.Memory.idle(), safe(true,false,false), config, 1).phase());
    }
    @Test void monkeyGroupsWithoutStealingAndFrogHopsOrSwims() {
        var settings = WildlifeExpansionSettings.disabled();
        assertEquals(PassiveBehavior.Phase.GROUPING, PassiveBehavior.step(Species.MONKEY, PassiveBehavior.Memory.idle(), safe(false,false,true), settings.animal(Species.MONKEY), 1).phase());
        assertEquals(PassiveBehavior.Phase.RESTING, PassiveBehavior.step(Species.MONKEY, PassiveBehavior.Memory.idle(), safe(true,false,true), settings.animal(Species.MONKEY), 1).phase());
        assertEquals(PassiveBehavior.Phase.HOPPING, PassiveBehavior.step(Species.FROG, PassiveBehavior.Memory.idle(), safe(false,false,false), settings.animal(Species.FROG), 1).phase());
        assertEquals(PassiveBehavior.Phase.SWIMMING, PassiveBehavior.step(Species.FROG, PassiveBehavior.Memory.idle(), safe(false,true,false), settings.animal(Species.FROG), 1).phase());
    }
    @Test void caimanWarnsBeforePlayerBiteAndRetreatsAtBoundedChaseOrFire() {
        var limits = WildlifeExpansionSettings.disabled().caiman().limits();
        var near = new JaguarBehavior.Observation(2,0,1,false,true,false,true,false);
        var memory = JaguarBehavior.step(JaguarBehavior.Memory.idle(), near, limits, 1);
        assertEquals(JaguarBehavior.Phase.WARNING, memory.phase());
        for (int second = 0; second < 4; second++) memory = JaguarBehavior.step(memory, near, limits, 1);
        assertEquals(JaguarBehavior.Phase.ATTACKING, memory.phase());
        for (int second = 0; second < 12; second++) memory = JaguarBehavior.step(memory, near, limits, 1);
        assertEquals(JaguarBehavior.Phase.RETREATING, memory.phase());
        assertEquals(JaguarBehavior.Phase.RETREATING, JaguarBehavior.step(JaguarBehavior.Memory.idle(), new JaguarBehavior.Observation(2,0,1,true,true,false,true,false), limits, 1).phase());
        assertFalse(Species.CAIMAN.passive());
    }
}
