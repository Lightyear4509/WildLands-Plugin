package gg.ggwp.wildlands.wildlife;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.storage.WildlifeRecord;
import gg.ggwp.wildlands.world.Region;
import java.util.*;
import org.junit.jupiter.api.Test;

class PopulationRulesTest {
    private WildlifeRecord animal(UUID world, Species species, int x, int z) { return new WildlifeRecord(UUID.randomUUID(), world, x, 65, z, true, species); }
    @Test void totalCapCountsAllSpeciesAndUnloadedRecords() {
        UUID world = UUID.randomUUID(); var records = List.of(animal(world, Species.JAGUAR, 0, 0), animal(world, Species.CAPYBARA, 256, 256));
        assertFalse(PopulationRules.permits(records, world, Species.TAPIR, 512, 512, 2, 8, 2));
        assertTrue(PopulationRules.permits(records, UUID.randomUUID(), Species.TAPIR, 0, 0, 2, 8, 2));
    }
    @Test void smallGroupCapAndSpeciesCapApplyWithoutPreventingOtherSpecies() {
        UUID world = UUID.randomUUID(); var records = new ArrayList<WildlifeRecord>();
        for (int i = 0; i < 4; i++) records.add(animal(world, Species.CAPYBARA, -1, -1));
        assertFalse(PopulationRules.permits(records, world, Species.CAPYBARA, -64, -64, 32, 12, 4));
        assertFalse(PopulationRules.permits(records, world, Species.CAPYBARA, 256, 256, 32, 4, 4));
        assertTrue(PopulationRules.permits(records, world, Species.TAPIR, -64, -64, 32, 8, 2));
    }
    @Test void habitatsSeparateWetlandGroupsFromForestForagers() {
        assertTrue(HabitatManager.accepts(Species.CAPYBARA, Region.WETLANDS));
        assertFalse(HabitatManager.accepts(Species.CAPYBARA, Region.ROCKY_ESCARPMENTS));
        assertTrue(HabitatManager.accepts(Species.TAPIR, Region.DENSE_RAINFOREST));
        assertFalse(HabitatManager.accepts(Species.TAPIR, Region.TROPICAL_RIVERS));
    }
}
