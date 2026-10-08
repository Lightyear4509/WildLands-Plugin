package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.world.LandmarkKind;
import java.util.*;

public record LandmarkRecord(UUID id, UUID worldUuid, String name, LandmarkKind kind, int x, int y, int z, UUID owner) {
    public LandmarkRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(worldUuid); Objects.requireNonNull(kind);
        if (name == null || !name.matches("[a-z][a-z0-9-]{0,47}") || Math.abs((long) x) > 30_000_000
                || Math.abs((long) z) > 30_000_000 || y < -2048 || y > 2048 || (kind == LandmarkKind.CAMP) != (owner != null))
            throw new IllegalArgumentException("Invalid landmark record");
    }
}
