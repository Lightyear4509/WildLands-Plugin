package gg.ggwp.wildlands.storage;

import java.util.*;

public record WildlifeRecord(UUID uuid, UUID worldUuid, double homeX, double homeY, double homeZ, boolean alive) {
    public WildlifeRecord {
        Objects.requireNonNull(uuid); Objects.requireNonNull(worldUuid);
        if (!Double.isFinite(homeX) || !Double.isFinite(homeY) || !Double.isFinite(homeZ)
                || Math.abs(homeX) > 30_000_000 || Math.abs(homeZ) > 30_000_000 || homeY < -2048 || homeY > 2048)
            throw new IllegalArgumentException("Invalid wildlife home coordinates");
    }
    public WildlifeRecord dead() { return new WildlifeRecord(uuid, worldUuid, homeX, homeY, homeZ, false); }
}
