package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.config.WorldgenSettings;

/** Bounded model-only search; does not generate or inspect any live chunk. */
public final class SpawnPlanner {
    public record Point(int x, int z) {}
    private SpawnPlanner() {}
    public static Point find(long seed, WorldgenSettings settings, int minHeight, int maxHeight) {
        var terrain = new TerrainModel(seed, settings.seaLevel(), minHeight, maxHeight);
        Point fallback = null;
        for (int radius = 0; radius <= 256; radius += 16)
            for (int x = -radius; x <= radius; x += 16) for (int z = -radius; z <= radius; z += 16) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                var column = terrain.column(x, z);
                if (column.submerged() || settings.caves() && terrain.cave(x, column.groundY(), z, column)) continue;
                if (fallback == null) fallback = new Point(x, z);
                if (column.region() == Region.JUNGLE_CLEARINGS) return new Point(x, z);
            }
        if (fallback != null) return fallback;
        throw new IllegalStateException("No dry spawn was found; choose another seed or set a safe spawn manually");
    }
}
