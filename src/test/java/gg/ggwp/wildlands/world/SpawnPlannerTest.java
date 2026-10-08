package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

class SpawnPlannerTest {
    @Test void multipleSeedsFindDeterministicDryBoundedSpawnWithoutSurfaceCaves() {
        var settings = new WorldgenSettings(2, 63, .8, true, true);
        for (long seed : new long[]{0, 1, -1, 4509, Long.MIN_VALUE, Long.MAX_VALUE}) {
            var point = SpawnPlanner.find(seed, settings, -64, 320);
            assertEquals(point, SpawnPlanner.find(seed, settings, -64, 320));
            assertTrue(Math.abs(point.x()) <= 256 && Math.abs(point.z()) <= 256);
            var terrain = new TerrainModel(seed, 63, -64, 320); var column = terrain.column(point.x(), point.z());
            assertFalse(column.submerged()); assertFalse(terrain.cave(point.x(), column.groundY(), point.z(), column));
        }
    }
    @Test void fixedSpawnInspectsOnlyTheChosenColumnAndDoesNotForceChunks() {
        var leaves = mock(BlockData.class); when(leaves.clone()).thenReturn(leaves);
        var settings = new WorldgenSettings(2, 63, .8, true, true); var generator = new WorldGenerator(settings, leaves);
        var world = mock(World.class); when(world.getSeed()).thenReturn(4509L); when(world.getMinHeight()).thenReturn(-64); when(world.getMaxHeight()).thenReturn(320);
        var point = SpawnPlanner.find(4509L, settings, -64, 320);
        when(world.getHighestBlockYAt(point.x(), point.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES)).thenReturn(80);
        var location = generator.getFixedSpawnLocation(world, new Random());
        assertEquals(81, location.getY()); assertEquals(point.x() + .5, location.getX());
        verify(world, times(1)).getHighestBlockYAt(anyInt(), anyInt(), any(HeightMap.class));
        verify(world, never()).getChunkAt(anyInt(), anyInt());
    }
    @Test void optimizedSurfaceNoiseMatchesTheOriginalThreeDimensionalSliceExactly() {
        for (long seed : new long[]{0, 4509, -913}) for (int x : new int[]{-30000000, -513, -1, 0, 1, 718, 30000000})
            for (int z : new int[]{-1103, -1, 0, 813}) for (double scale : new double[]{95, 105, 160, 230, 310, 360, 620})
                assertEquals(TerrainNoise.sample(seed, x / scale, 0, z / scale), TerrainNoise.surface(seed, x, z, scale));
    }
}
