package gg.ggwp.wildlands.survival;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShelterBoundsTest {
    @Test void unloadedAdjacentChunkIsNeverRead() {
        World world = mock(World.class);
        Block center = center(world);
        when(center.getX()).thenReturn(15);
        assertNull(ShelterService.loadedRelative(center, 1, 0, 0));
        verify(world).isChunkLoaded(1, 0);
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }

    @Test void buildLimitsAreExclusiveAtTopAndNegativeChunksUseFloorDivision() {
        World world = mock(World.class);
        Block center = center(world);
        assertNull(ShelterService.loadedRelative(center, 0, 220, 0));
        assertNull(ShelterService.loadedRelative(center, 0, -165, 0));
        verify(world, never()).isChunkLoaded(anyInt(), anyInt());
        Block target = mock(Block.class);
        when(world.isChunkLoaded(-1, -1)).thenReturn(true);
        when(world.getBlockAt(-1, 100, -1)).thenReturn(target);
        assertSame(target, ShelterService.loadedRelative(center, -1, 0, -1));
    }

    private Block center(World world) {
        Block center = mock(Block.class);
        when(center.getWorld()).thenReturn(world);
        when(center.getY()).thenReturn(100);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        return center;
    }
}
