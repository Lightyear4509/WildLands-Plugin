package gg.ggwp.wildlands.wildlife.behaviors;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.config.WildlifeExpansionSettings;
import gg.ggwp.wildlands.wildlife.Species;
import org.junit.jupiter.api.Test;

class PassiveBehaviorTest {
    private PassiveBehavior.Observation safe(boolean night) { return new PassiveBehavior.Observation(20, 0, false, false, false, false, night, false); }
    @Test void tapirRestsInDayForagesAtNightAndDoesNotAttack() {
        var config = WildlifeExpansionSettings.disabled().animal(Species.TAPIR);
        assertEquals(PassiveBehavior.Phase.RESTING, PassiveBehavior.step(Species.TAPIR, PassiveBehavior.Memory.idle(), safe(false), config, 1).phase());
        assertEquals(PassiveBehavior.Phase.FORAGING, PassiveBehavior.step(Species.TAPIR, PassiveBehavior.Memory.idle(), safe(true), config, 1).phase());
    }
    @Test void capybaraSeeksWaterAndFormsGroups() {
        var config = WildlifeExpansionSettings.disabled().animal(Species.CAPYBARA);
        assertEquals(PassiveBehavior.Phase.SWIMMING, PassiveBehavior.step(Species.CAPYBARA, PassiveBehavior.Memory.idle(), new PassiveBehavior.Observation(20, 0, false, false, true, true, false, false), config, 1).phase());
        assertEquals(PassiveBehavior.Phase.GROUPING, PassiveBehavior.step(Species.CAPYBARA, PassiveBehavior.Memory.idle(), new PassiveBehavior.Observation(20, 0, false, false, true, false, false, false), config, 1).phase());
    }
    @Test void dangerStartsBoundedRetreatAndSafeConditionsReleaseIt() {
        for (Species species : new Species[]{Species.CAPYBARA, Species.TAPIR}) {
            var config = WildlifeExpansionSettings.disabled().animal(species);
            var memory = PassiveBehavior.step(species, PassiveBehavior.Memory.idle(), new PassiveBehavior.Observation(2, 0, false, false, false, false, false, false), config, 1);
            assertEquals(PassiveBehavior.Phase.RETREATING, memory.phase());
            for (int i = 0; i < 31; i++) memory = PassiveBehavior.step(species, memory, safe(false), config, 1);
            assertNotEquals(PassiveBehavior.Phase.RETREATING, memory.phase()); assertEquals(0, memory.fleeRemaining());
        }
    }
    @Test void exceedingHomeRadiusReturnsHomeEvenDuringFear() {
        var config = WildlifeExpansionSettings.disabled().animal(Species.CAPYBARA);
        var memory = PassiveBehavior.step(Species.CAPYBARA, new PassiveBehavior.Memory(PassiveBehavior.Phase.RETREATING, 10), new PassiveBehavior.Observation(0, 30, true, true, true, true, false, true), config, 1);
        assertEquals(PassiveBehavior.Phase.RETURNING, memory.phase());
    }
    @Test void invalidStepsAndPredatorUseAreRejected() {
        var config = WildlifeExpansionSettings.disabled().animal(Species.CAPYBARA);
        assertThrows(IllegalArgumentException.class, () -> PassiveBehavior.step(Species.JAGUAR, PassiveBehavior.Memory.idle(), safe(false), config, 1));
        assertThrows(IllegalArgumentException.class, () -> PassiveBehavior.step(Species.CAPYBARA, PassiveBehavior.Memory.idle(), safe(false), config, Double.NaN));
    }
}
