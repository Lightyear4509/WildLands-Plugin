package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.EnvironmentSettings;
import gg.ggwp.wildlands.core.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;

/** Bounded local checks for ordinary player-built shelter and warmth. */
public final class ShelterService implements WildlandsModule {
    private final WildlandsPlugin plugin;
    private boolean enabled;
    public ShelterService(WildlandsPlugin plugin) { this.plugin = plugin; }
    @Override public String id() { return "shelter"; }
    @Override public void enable() { enabled = true; }
    @Override public void disable() { enabled = false; }
    public boolean enabled() { return enabled; }
    public ShelterStatus assess(Player player) {
        EnvironmentSettings settings = plugin.configuration().environment();
        Location location = player.getLocation();
        Block feet = location.getBlock();
        boolean roofed = false;
        for (int offset = 1; offset <= settings.roofSearchHeight(); offset++) {
            Block block = loadedRelative(feet, 0, offset, 0);
            if (block == null) break;
            if (!block.isPassable()) { roofed = true; break; }
        }
        int walls = 0;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            for (int distance = 1; distance <= settings.enclosureRadius(); distance++) {
                Block block = loadedRelative(feet, face.getModX() * distance, 0, face.getModZ() * distance);
                if (block == null) break;
                Block upper = loadedRelative(block, 0, 1, 0);
                if (!block.isPassable() && upper != null && upper.getType().isSolid()) { walls++; break; }
            }
        }
        Block ground = loadedRelative(feet, 0, -1, 0);
        boolean dryGround = ground != null && ground.getType().isSolid() && !ground.isLiquid() && !feet.isLiquid();
        return new ShelterStatus(roofed, walls >= 3, dryGround, campfireNearby(feet, settings));
    }
    private static boolean campfireNearby(Block center, EnvironmentSettings settings) {
        int radius = settings.campfireRadius();
        for (int y = -settings.campfireSearchHeight(); y <= settings.campfireSearchHeight(); y++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z > radius * radius) continue;
                    Block block = loadedRelative(center, x, y, z);
                    if (block == null) continue;
                    if (block.getBlockData() instanceof Campfire campfire && campfire.isLit()) return true;
                }
        return false;
    }
    static Block loadedRelative(Block center, int offsetX, int offsetY, int offsetZ) {
        World world = center.getWorld();
        int blockX = center.getX() + offsetX;
        int blockY = center.getY() + offsetY;
        int blockZ = center.getZ() + offsetZ;
        if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()
                || !world.isChunkLoaded(blockX >> 4, blockZ >> 4)) return null;
        return world.getBlockAt(blockX, blockY, blockZ);
    }
}
