package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.List;
import org.bukkit.block.Biome;
import org.bukkit.generator.*;

/** Vanilla biome identities preserve climate, mob compatibility and crossplay visuals. */
public final class BiomeManager extends BiomeProvider {
    private final WorldgenSettings settings;
    public BiomeManager(WorldgenSettings settings) { this.settings = settings; }
    @Override public Biome getBiome(WorldInfo info, int x, int y, int z) {
        var model = new TerrainModel(info.getSeed(), settings.seaLevel(), info.getMinHeight(), info.getMaxHeight());
        return biome(settings.caves() ? model.region(x, y, z) : model.column(x, z).region());
    }
    public static Biome biome(Region region) {
        return switch (region) {
            case DENSE_RAINFOREST -> Biome.JUNGLE;
            case TROPICAL_RIVERS, WATERFALL_VALLEYS -> Biome.RIVER;
            case FLOODPLAINS, JUNGLE_CLEARINGS -> Biome.SPARSE_JUNGLE;
            case WETLANDS -> Biome.MANGROVE_SWAMP;
            case RAINFOREST_HIGHLANDS -> Biome.WINDSWEPT_FOREST;
            case BAMBOO_FORESTS -> Biome.BAMBOO_JUNGLE;
            case ROCKY_ESCARPMENTS -> Biome.STONY_PEAKS;
            case CAVE_NETWORKS -> Biome.LUSH_CAVES;
        };
    }
    @Override public List<Biome> getBiomes(WorldInfo info) {
        return List.of(Biome.JUNGLE, Biome.RIVER, Biome.SPARSE_JUNGLE, Biome.MANGROVE_SWAMP,
                Biome.WINDSWEPT_FOREST, Biome.BAMBOO_JUNGLE, Biome.STONY_PEAKS, Biome.LUSH_CAVES);
    }
}
