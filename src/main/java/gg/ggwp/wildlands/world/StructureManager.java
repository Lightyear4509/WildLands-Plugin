package gg.ggwp.wildlands.world;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/** Ruins are clipped to one chunk; no neighbor reads, entities, loot or live-world edits. */
public final class StructureManager {
    private StructureManager() {}
    public static void ruin(long seed, LandmarkPlanner.Candidate candidate, int cx, int cz, ChunkData data) {
        if (candidate.kind() != LandmarkKind.RUINS) return;
        int centerX = candidate.x() - cx * 16, centerZ = candidate.z() - cz * 16, floor = candidate.y();
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            int x = centerX + dx, z = centerZ + dz;
            if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
            for (int y = floor - 3; y <= floor + 5; y++) {
                if (y < data.getMinHeight() || y >= data.getMaxHeight()) continue;
                Material material;
                if (y < floor) material = Material.COBBLESTONE;
                else if (y == floor) material = TerrainNoise.unit(seed + 617, candidate.x() + dx, y, candidate.z() + dz) < .45
                        ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS;
                else {
                    boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                    boolean entrance = Math.abs(dx) <= 1 && dz == -5;
                    int height = 1 + (int) (TerrainNoise.unit(seed + 619, candidate.x() + dx, 0, candidate.z() + dz) * 4);
                    material = edge && !entrance && y <= floor + height ? Material.MOSSY_STONE_BRICKS : Material.AIR;
                }
                data.setBlock(x, y, z, material);
            }
        }
    }
}
