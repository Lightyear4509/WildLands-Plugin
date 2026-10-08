package gg.ggwp.wildlands.world;

/** Immutable world-seed model. All values use absolute block coordinates. */
public final class TerrainModel {
    public record Column(int groundY, int waterY, Region region, double moisture) {
        public boolean submerged() { return waterY > groundY; }
    }
    private final long seed;
    private final int seaLevel, minHeight, maxHeight;
    public TerrainModel(long seed, int seaLevel, int minHeight, int maxHeight) {
        if (maxHeight - minHeight < 128 || seaLevel < minHeight + 32 || seaLevel > maxHeight - 64)
            throw new IllegalArgumentException("World height must leave space for terrain, caves and canopy");
        this.seed = seed; this.seaLevel = seaLevel; this.minHeight = minHeight; this.maxHeight = maxHeight;
    }
    public Column column(int x, int z) {
        double continental = TerrainNoise.surface(seed + 11, x, z, 620);
        double detail = TerrainNoise.surface(seed + 29, x, z, 95);
        double moisture = TerrainNoise.surface(seed + 43, x, z, 310);
        double ridge = Math.abs(TerrainNoise.surface(seed + 71, x, z, 230));
        // Zero contours of the same continuous field span neighboring chunks and form branching waterways.
        double river = Math.abs(TerrainNoise.surface(seed + 101, x, z, 360)
                + .16 * TerrainNoise.surface(seed + 103, x, z, 105));
        double uplift = Math.max(0, continental - .10) * 100;
        double escarpment = continental > .25 && ridge > .55 ? (ridge - .55) * 85 : 0;
        int natural = (int) Math.round(seaLevel + 9 + continental * 16 + detail * 7 + uplift + escarpment);
        int ground = natural, water = seaLevel;
        Region region;
        if (river < .035) {
            // Elevated river terraces drop toward the lowland level, producing waterfall steps.
            int terrace = (int) (uplift / 12) * 9;
            water = seaLevel + terrace;
            ground = water - 3 - (int) Math.round((.035 - river) * 110);
            region = terrace > 0 ? Region.WATERFALL_VALLEYS : Region.TROPICAL_RIVERS;
        } else if (river < .105) {
            double bank = (river - .035) / .070;
            int terrace = (int) (uplift / 12) * 9;
            ground = (int) Math.round((seaLevel + terrace + 1) * (1 - bank) + natural * bank);
            region = Region.FLOODPLAINS;
        } else if (continental < -.15 && moisture > .15) {
            ground = seaLevel - 1 + (int) Math.round(detail * 3);
            region = Region.WETLANDS;
        } else if (escarpment > 5) region = Region.ROCKY_ESCARPMENTS;
        else if (uplift > 24) region = Region.RAINFOREST_HIGHLANDS;
        else {
            double vegetation = TerrainNoise.surface(seed + 151, x, z, 160);
            region = vegetation > .35 ? Region.BAMBOO_FORESTS : vegetation < -.40
                    ? Region.JUNGLE_CLEARINGS : Region.DENSE_RAINFOREST;
        }
        ground = Math.clamp(ground, minHeight + 24, maxHeight - 40);
        water = Math.clamp(water, minHeight + 24, maxHeight - 40);
        return new Column(ground, water, region, moisture);
    }
    public boolean cave(int x, int y, int z, Column column) {
        if (y < minHeight + 8 || y > column.groundY()) return false;
        // Rare rocky openings connect surface exploration to the same underground tunnel field.
        if (y > column.groundY() - 10 && (column.region() != Region.ROCKY_ESCARPMENTS
                || TerrainNoise.unit(seed + 227, Math.floorDiv(x, 16), 0, Math.floorDiv(z, 16)) >= .12)) return false;
        // Intersect two broad sheets to create connected tunnels instead of isolated air bubbles.
        double first = TerrainNoise.sample(seed + 211, x / 43.0, y / 29.0, z / 43.0);
        if (Math.abs(first) >= .12) return false;
        double second = TerrainNoise.sample(seed + 223, x / 57.0, y / 37.0, z / 57.0);
        return Math.abs(second) < .18;
    }
    public Region region(int x, int y, int z) {
        Column column = column(x, z);
        return cave(x, y, z, column) ? Region.CAVE_NETWORKS : column.region();
    }
}
