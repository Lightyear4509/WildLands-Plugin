package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerrainModelTest {
    @Test void deterministicAcrossOrderAndNegativeChunkBoundaries() {
        var first = new TerrainModel(4509, 63, -64, 320);
        var second = new TerrainModel(4509, 63, -64, 320);
        var points = List.of(-1048576, -513, -17, -16, -1, 0, 15, 16, 511, 1048576);
        for (int x : points) for (int z : points) {
            var expected = first.column(x, z);
            second.column(z, x);
            assertEquals(expected, second.column(x, z));
            assertTrue(expected.groundY() >= -40 && expected.groundY() <= 280);
            assertFalse(first.cave(x, -64, z, expected));
            if (expected.region() != Region.ROCKY_ESCARPMENTS) assertFalse(first.cave(x, expected.groundY(), z, expected));
        }
        assertNotEquals(first.column(300, 400), new TerrainModel(4510, 63, -64, 320).column(300, 400));
    }
    @Test void representativeAreaContainsEverySurfaceHabitatAndUndergroundCaves() {
        var model = new TerrainModel(4509, 63, -64, 320);
        var found = EnumSet.noneOf(Region.class);
        boolean cave = false;
        for (int x = -4096; x <= 4096; x += 32) for (int z = -4096; z <= 4096; z += 32) {
            var column = model.column(x, z); found.add(column.region());
            if (column.region() == Region.TROPICAL_RIVERS || column.region() == Region.WATERFALL_VALLEYS)
                assertTrue(column.submerged(), "River channels must contain water");
            for (int y = -40; y < column.groundY() - 10 && !cave; y += 8)
                cave |= model.cave(x, y, z, column);
        }
        found.add(Region.CAVE_NETWORKS);
        assertEquals(EnumSet.allOf(Region.class), found);
        assertTrue(cave);
    }
    @Test void noiseIsContinuousAcrossLatticeBoundariesAndBounded() {
        for (int coordinate = -20; coordinate <= 20; coordinate++) {
            double left = TerrainNoise.sample(100, coordinate - 1e-6, .25, -.75);
            double right = TerrainNoise.sample(100, coordinate + 1e-6, .25, -.75);
            assertEquals(left, right, 1e-5);
            assertTrue(left >= -1 && left <= 1);
        }
        assertThrows(IllegalArgumentException.class, () -> new TerrainModel(1, 300, -64, 320));
    }
}
