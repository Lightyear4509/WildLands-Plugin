package gg.ggwp.wildlands.wildlife;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.Test;

class WildlifeLocomotionTest {
    @Test void shorelineLandingAcceptsAirAboveWaterButNeverReadsUnloadedBlocks() throws Exception {
        var method = WildlifeManager.class.getDeclaredMethod("waterLanding", Location.class); method.setAccessible(true);
        var world = mock(World.class); var border = mock(WorldBorder.class); when(world.getWorldBorder()).thenReturn(border);
        when(world.getMinHeight()).thenReturn(-64); when(world.getMaxHeight()).thenReturn(320); when(border.isInside(any())).thenReturn(true);
        Location landing = new Location(world, 10, 64, 10);
        assertFalse((boolean) method.invoke(null, landing)); verify(world, never()).getBlockAt(anyInt(),anyInt(),anyInt());
        when(world.isChunkLoaded(0,0)).thenReturn(true);
        Block feet = mock(Block.class), below = mock(Block.class), above = mock(Block.class);
        when(world.getBlockAt(anyInt(),anyInt(),anyInt())).thenReturn(feet);
        when(world.getBlockAt(any(Location.class))).thenReturn(feet);
        when(feet.getType()).thenReturn(Material.AIR); when(feet.isPassable()).thenReturn(true);
        when(feet.getRelative(0,-1,0)).thenReturn(below); when(feet.getRelative(0,1,0)).thenReturn(above);
        when(below.getType()).thenReturn(Material.WATER); when(above.isPassable()).thenReturn(true);
        assertTrue((boolean) method.invoke(null, landing));
        when(below.getType()).thenReturn(Material.LAVA); assertFalse((boolean) method.invoke(null, landing));
        when(feet.getType()).thenReturn(Material.WATER); assertTrue((boolean) method.invoke(null, landing));
        when(above.isPassable()).thenReturn(false); assertFalse((boolean) method.invoke(null, landing));
    }
    @Test void disablingWildlifeRestoresBirdGravityAndClearsCachedMotion() throws Exception {
        var plugin = mock(WildlandsPlugin.class); var storage = mock(StorageService.class);
        when(storage.submit(any())).thenReturn(CompletableFuture.completedFuture(null));
        var manager = new WildlifeManager(plugin, storage, List.of());
        Mob bird = mock(Mob.class); var path = mock(com.destroystokyo.paper.entity.Pathfinder.class); when(bird.getPathfinder()).thenReturn(path);
        var record = new WildlifeRecord(UUID.randomUUID(), UUID.randomUUID(), 0,64,0,true,Species.TOUCAN);
        var type = Class.forName(WildlifeManager.class.getName()+"$Session");
        var constructor = type.getDeclaredConstructor(Mob.class, WildlifeRecord.class, long.class); constructor.setAccessible(true);
        Object session = constructor.newInstance(bird, record, 0L);
        var goal = type.getDeclaredField("navigationGoal"); goal.setAccessible(true); goal.set(session,new Location(null,5,68,5));
        var field = WildlifeManager.class.getDeclaredField("loaded"); field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<UUID,Object> loaded = (Map<UUID,Object>)field.get(manager); loaded.put(record.uuid(),session);
        manager.disable(); verify(path).stopPathfinding(); verify(bird).setTarget(null); verify(bird).setGravity(true);
        verify(bird).setVelocity(new org.bukkit.util.Vector()); assertNull(goal.get(session)); assertTrue(loaded.isEmpty());
    }
}
