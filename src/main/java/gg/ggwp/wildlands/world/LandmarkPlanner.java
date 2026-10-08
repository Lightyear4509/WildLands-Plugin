package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.config.WorldgenSettings;
import gg.ggwp.wildlands.storage.LandmarkRecord;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One seed-derived landmark candidate per 256-block cell, computed without reading world blocks. */
public final class LandmarkPlanner {
    public record Candidate(int x, int y, int z, LandmarkKind kind) {
        public LandmarkRecord record(UUID world) {
            UUID id = UUID.nameUUIDFromBytes((world + ":" + kind + ":" + x + ":" + z).getBytes(StandardCharsets.UTF_8));
            String suffix = id.toString().replace("-", "").substring(0, 12);
            return new LandmarkRecord(id, world, kind.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + suffix, kind, x, y, z, null);
        }
    }
    private LandmarkPlanner() {}
    public static Optional<Candidate> inChunk(long seed, WorldgenSettings settings, int minHeight, int maxHeight, int cx, int cz) {
        int cellX = Math.floorDiv(cx, 16), cellZ = Math.floorDiv(cz, 16);
        int x = cellX * 256 + 32 + (int) (TerrainNoise.unit(seed + 601, cellX, 0, cellZ) * 192);
        int z = cellZ * 256 + 32 + (int) (TerrainNoise.unit(seed + 607, cellX, 0, cellZ) * 192);
        x = Math.floorDiv(x, 16) * 16 + 8; z = Math.floorDiv(z, 16) * 16 + 8;
        if (Math.floorDiv(x, 16) != cx || Math.floorDiv(z, 16) != cz) return Optional.empty();
        var terrain = new TerrainModel(seed, settings.seaLevel(), minHeight, maxHeight);
        var column = terrain.column(x, z);
        LandmarkKind kind = switch (column.region()) {
            case TROPICAL_RIVERS, FLOODPLAINS -> LandmarkKind.RIVER_BEND;
            case WATERFALL_VALLEYS -> LandmarkKind.WATERFALL;
            case WETLANDS -> LandmarkKind.WETLAND_POOL;
            case RAINFOREST_HIGHLANDS -> LandmarkKind.HIGHLAND_LOOKOUT;
            case ROCKY_ESCARPMENTS -> LandmarkKind.ROCKY_ESCARPMENT;
            case BAMBOO_FORESTS -> LandmarkKind.BAMBOO_GROVE;
            case JUNGLE_CLEARINGS -> LandmarkKind.CLEARING;
            default -> LandmarkKind.RAINFOREST_GROVE;
        };
        boolean flat = !column.submerged();
        for (int dx : new int[]{-5, 0, 5}) for (int dz : new int[]{-5, 0, 5}) {
            var other = terrain.column(x + dx, z + dz);
            if (other.submerged() || Math.abs(other.groundY() - column.groundY()) > 2
                    || settings.caves() && terrain.cave(x + dx, other.groundY(), z + dz, other)) flat = false;
        }
        if (settings.version() >= 2 && flat && TerrainNoise.unit(seed + 613, cellX, 0, cellZ) < .30) kind = LandmarkKind.RUINS;
        return Optional.of(new Candidate(x, Math.max(column.groundY(), column.waterY()) + 1, z, kind));
    }
}
