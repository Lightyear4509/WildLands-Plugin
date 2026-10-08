package gg.ggwp.wildlands.wildlife;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.world.Region;
import org.junit.jupiter.api.Test;

class HabitatManagerTest {
    @Test void ecologicalRegionsAndNegativeTerritoriesUseStableCells() {
        assertTrue(HabitatManager.jaguar(Region.DENSE_RAINFOREST)); assertTrue(HabitatManager.jaguar(Region.BAMBOO_FORESTS));
        assertFalse(HabitatManager.jaguar(Region.TROPICAL_RIVERS)); assertFalse(HabitatManager.jaguar(Region.CAVE_NETWORKS));
        assertEquals(HabitatManager.cell(-1, -1), HabitatManager.cell(-128, -128));
        assertNotEquals(HabitatManager.cell(-1, -1), HabitatManager.cell(0, 0));
        assertNotEquals(HabitatManager.cell(0, 0), HabitatManager.cell(128, 0));
    }
}
