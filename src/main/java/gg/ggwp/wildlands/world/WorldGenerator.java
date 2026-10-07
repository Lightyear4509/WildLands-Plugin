package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.Random;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.*;

/** Generates only its supplied chunk buffer, never reads or loads neighboring chunks. */
public final class WorldGenerator extends ChunkGenerator {
    private final WorldgenSettings settings;
    private final BlockData canopy;
    public WorldGenerator(WorldgenSettings settings) {
        this(settings, Material.JUNGLE_LEAVES.createBlockData(data -> ((Leaves) data).setPersistent(true)));
    }
    public WorldGenerator(WorldgenSettings settings, BlockData canopy) {
        this.settings = java.util.Objects.requireNonNull(settings);
        this.canopy = java.util.Objects.requireNonNull(canopy).clone();
    }
    public WorldgenSettings settings() { return settings; }
    private TerrainModel model(WorldInfo info) {
        return new TerrainModel(info.getSeed(), settings.seaLevel(), info.getMinHeight(), info.getMaxHeight());
    }
    @Override public void generateNoise(WorldInfo info, Random ignored, int chunkX, int chunkZ, ChunkData data) {
        var terrain = model(info);
        long seed = info.getSeed();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = chunkX * 16 + x, wz = chunkZ * 16 + z;
            var column = terrain.column(wx, wz);
            data.setRegion(x, data.getMinHeight(), z, x + 1, column.groundY() + 1, z + 1, Material.STONE);
            for (int y = data.getMinHeight(); y <= column.groundY(); y++) {
                if (y < data.getMinHeight() + 1 + TerrainNoise.unit(seed, wx, y, wz) * 3) {
                    data.setBlock(x, y, z, Material.BEDROCK); continue;
                }
                if (settings.caves() && terrain.cave(wx, y, wz, column)) {
                    data.setBlock(x, y, z, y < data.getMinHeight() + 12 ? Material.LAVA : Material.CAVE_AIR); continue;
                }
                if (y < 0) data.setBlock(x, y, z, Material.DEEPSLATE);
                if (settings.ores() && y < column.groundY() - 7) {
                    Material ore = ore(seed, wx, y, wz);
                    if (ore != null) data.setBlock(x, y, z, ore);
                }
            }
            Material surface = column.submerged() ? (column.region() == Region.WETLANDS ? Material.MUD : Material.GRAVEL)
                    : column.region() == Region.ROCKY_ESCARPMENTS ? Material.STONE : Material.GRASS_BLOCK;
            for (int y = column.groundY() - 3; y <= column.groundY(); y++)
                if (!settings.caves() || !terrain.cave(wx, y, wz, column))
                    data.setBlock(x, y, z, y == column.groundY() ? surface : surface == Material.STONE ? Material.STONE : Material.DIRT);
            if (column.submerged()) data.setRegion(x, column.groundY() + 1, z, x + 1, column.waterY() + 1, z + 1, Material.WATER);
        }
        decorate(info, terrain, chunkX, chunkZ, data);
    }
    private static Material ore(long seed, int x, int y, int z) {
        // Coarse lattice clusters provide mineable veins, with independent fine noise for their edges.
        double cluster = TerrainNoise.unit(seed + 307, Math.floorDiv(x, 4), Math.floorDiv(y, 4), Math.floorDiv(z, 4));
        if (cluster > .10 || TerrainNoise.unit(seed + 311, x, y, z) > .55) return null;
        boolean deep = y < 0;
        if (cluster < .004 && y < -16) return Material.DEEPSLATE_DIAMOND_ORE;
        if (cluster < .012 && y < 32) return deep ? Material.DEEPSLATE_GOLD_ORE : Material.GOLD_ORE;
        if (cluster < .022 && y < 16) return deep ? Material.DEEPSLATE_REDSTONE_ORE : Material.REDSTONE_ORE;
        if (cluster < .030 && y < 48) return deep ? Material.DEEPSLATE_LAPIS_ORE : Material.LAPIS_ORE;
        if (cluster < .060) return deep ? Material.DEEPSLATE_IRON_ORE : Material.IRON_ORE;
        if (cluster < .080 && y > -16) return deep ? Material.DEEPSLATE_COPPER_ORE : Material.COPPER_ORE;
        return deep ? Material.DEEPSLATE_COAL_ORE : Material.COAL_ORE;
    }
    private void decorate(WorldInfo info, TerrainModel terrain, int cx, int cz, ChunkData data) {
        int originX = cx * 16, originZ = cz * 16;
        // Every chunk reproduces candidates from the same surrounding lattice and clips writes locally.
        for (int gx = Math.floorDiv(originX - 8, 9); gx <= Math.floorDiv(originX + 23, 9); gx++)
            for (int gz = Math.floorDiv(originZ - 8, 9); gz <= Math.floorDiv(originZ + 23, 9); gz++) {
                int tx = gx * 9 + (int) (TerrainNoise.unit(info.getSeed() + 401, gx, 0, gz) * 7);
                int tz = gz * 9 + (int) (TerrainNoise.unit(info.getSeed() + 403, gx, 0, gz) * 7);
                var ground = terrain.column(tx, tz);
                double density = settings.treeDensity() * switch (ground.region()) {
                    case DENSE_RAINFOREST -> 1.0;
                    case BAMBOO_FORESTS -> .35;
                    case WETLANDS, RAINFOREST_HIGHLANDS -> .55;
                    case FLOODPLAINS -> .20;
                    default -> .03;
                };
                if (ground.submerged() || settings.caves() && terrain.cave(tx, ground.groundY(), tz, ground)
                        || TerrainNoise.unit(info.getSeed() + 409, gx, 0, gz) >= density) continue;
                int height = 11 + (int) (TerrainNoise.unit(info.getSeed() + 419, gx, 0, gz) * 14);
                if (ground.region() == Region.DENSE_RAINFOREST && TerrainNoise.unit(info.getSeed() + 421, gx, 0, gz) < .035) height = 32;
                int base = ground.groundY() + 1, top = base + height;
                boolean giant = height >= 21;
                int crown = height == 32 ? 8 : 6;
                for (int x = -crown; x <= crown; x++) for (int z = -crown; z <= crown; z++) for (int y = -3; y <= 3; y++) {
                    if (x * x / (double) (crown * crown) + z * z / (double) (crown * crown) + y * y / 9.0 <= 1)
                        place(data, tx + x - originX, top + y, tz + z - originZ, canopy);
                }
                for (int y = base; y <= top; y++) for (int dx = 0; dx <= (giant ? 1 : 0); dx++)
                    for (int dz = 0; dz <= (giant ? 1 : 0); dz++)
                        place(data, tx + dx - originX, y, tz + dz - originZ, Material.JUNGLE_LOG);
                // Short radial branches make the crowns read as broad rainforest trees.
                for (int offset = -4; offset <= 4; offset++) {
                    place(data, tx + offset - originX, top - 1, tz - originZ, Material.JUNGLE_LOG);
                    place(data, tx - originX, top - 1, tz + offset - originZ, Material.JUNGLE_LOG);
                }
            }
        for (int gx = Math.floorDiv(originX - 4, 32); gx <= Math.floorDiv(originX + 19, 32); gx++)
            for (int gz = Math.floorDiv(originZ - 4, 32); gz <= Math.floorDiv(originZ + 19, 32); gz++) {
                int rx = gx * 32 + 12, rz = gz * 32 + 12;
                var column = terrain.column(rx, rz);
                if (column.submerged() || TerrainNoise.unit(info.getSeed() + 429, gx, 0, gz) > .20) continue;
                if (column.region() != Region.ROCKY_ESCARPMENTS && column.region() != Region.RAINFOREST_HIGHLANDS) continue;
                int radius = 2 + (int) (TerrainNoise.unit(info.getSeed() + 431, gx, 0, gz) * 3);
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = 0; dy <= radius; dy++) if (dx * dx + dz * dz + dy * dy <= radius * radius)
                        place(data, rx + dx - originX, column.groundY() + dy + 1, rz + dz - originZ, Material.MOSSY_COBBLESTONE);
            }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            var column = terrain.column(originX + x, originZ + z);
            if (column.submerged()) continue;
            int y = column.groundY() + 1;
            if (settings.caves() && terrain.cave(originX + x, column.groundY(), originZ + z, column)) continue;
            if (!air(data.getType(x, y, z))) continue;
            double roll = TerrainNoise.unit(info.getSeed() + 433, originX + x, 0, originZ + z);
            if (column.region() == Region.BAMBOO_FORESTS && roll < .10) {
                int height = 5 + (int) (roll * 60);
                for (int dy = 0; dy < height; dy++) if (air(data.getType(x, y + dy, z)))
                    data.setBlock(x, y + dy, z, Material.BAMBOO);
            } else if (roll < .08 && column.region() != Region.ROCKY_ESCARPMENTS)
                data.setBlock(x, y, z, Material.FERN);
        }
    }
    private static void place(ChunkData data, int x, int y, int z, Material material) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= data.getMinHeight() && y < data.getMaxHeight()
                && (air(data.getType(x, y, z)) || data.getType(x, y, z) == Material.JUNGLE_LEAVES))
            data.setBlock(x, y, z, material);
    }
    private static void place(ChunkData data, int x, int y, int z, BlockData block) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= data.getMinHeight() && y < data.getMaxHeight()
                && air(data.getType(x, y, z))) data.setBlock(x, y, z, block);
    }
    private static boolean air(Material material) { return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR; }
    @Override public BiomeProvider getDefaultBiomeProvider(WorldInfo info) { return new BiomeManager(settings); }
    @Override public int getBaseHeight(WorldInfo info, Random random, int x, int z, HeightMap map) {
        var column = model(info).column(x, z);
        return (map == HeightMap.OCEAN_FLOOR || map == HeightMap.OCEAN_FLOOR_WG
                ? column.groundY() : Math.max(column.groundY(), column.waterY())) + 1;
    }
    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateBedrock() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateMobs() { return true; }
    @Override public boolean isParallelCapable() { return true; }
}
