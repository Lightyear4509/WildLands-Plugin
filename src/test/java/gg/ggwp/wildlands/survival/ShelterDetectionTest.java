package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.config.EnvironmentSettings;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShelterDetectionTest {
    private final World world = mock(World.class);
    private final Map<String, Block> blocks = new HashMap<>();
    private final Player player = mock(Player.class);
    private final Material solidMaterial = mock(Material.class);
    private final Material airMaterial = mock(Material.class);
    private final ShelterService service;

    ShelterDetectionTest() {
        when(solidMaterial.isSolid()).thenReturn(true);
        var settings = new EnvironmentSettings(5, 26, 63, .04, 3, 4, 2,
                8, 6, 31, 12, 8, 1.35, 8, 18, 2, 2, 12, 3, 2, .75, 3, 2);
        var config = mock(ConfigurationManager.Snapshot.class);
        var plugin = mock(WildlandsPlugin.class);
        when(config.environment()).thenReturn(settings);
        when(plugin.configuration()).thenReturn(config);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call ->
                block(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        when(world.getBlockAt(any(Location.class))).thenAnswer(call -> {
            Location location = call.getArgument(0);
            return block(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        });
        when(player.getLocation()).thenReturn(new Location(world, 0.5, 100, 0.5));
        solid(0, 99, 0);
        service = new ShelterService(plugin);
    }

    @Test void openEntranceShelterNeedsRoofThreeWallsAndDryGround() {
        assertEquals(new ShelterStatus(false, false, true, false), service.assess(player));
        solid(0, 103, 0);
        assertEquals(new ShelterStatus(true, false, true, false), service.assess(player));
        for (int[] wall : new int[][]{{-2, 0}, {2, 0}, {0, -2}}) {
            solid(wall[0], 100, wall[1]);
            solid(wall[0], 101, wall[1]);
        }
        assertEquals(new ShelterStatus(true, true, true, false), service.assess(player));
        when(block(0, 100, 0).isLiquid()).thenReturn(true);
        assertFalse(service.assess(player).dryGround());
    }

    @Test void onlyLitCampfiresWithinConfiguredSearchBoundsSupplyWarmth() {
        var fire = mock(Campfire.class);
        when(fire.isLit()).thenReturn(true);
        when(block(7, 100, 0).getBlockData()).thenReturn(fire);
        assertFalse(service.assess(player).nearbyCampfire());
        when(block(6, 100, 0).getBlockData()).thenReturn(fire);
        assertTrue(service.assess(player).nearbyCampfire());
        when(fire.isLit()).thenReturn(false);
        assertFalse(service.assess(player).nearbyCampfire());
    }

    private void solid(int x, int y, int z) {
        Block block = block(x, y, z);
        when(block.isPassable()).thenReturn(false);
        when(block.getType()).thenReturn(solidMaterial);
    }

    private Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(x + ":" + y + ":" + z, key -> {
            Block block = mock(Block.class);
            when(block.getWorld()).thenReturn(world);
            when(block.getX()).thenReturn(x);
            when(block.getY()).thenReturn(y);
            when(block.getZ()).thenReturn(z);
            when(block.getType()).thenReturn(airMaterial);
            when(block.isPassable()).thenReturn(true);
            return block;
        });
    }
}
