package gg.ggwp.wildlands.storage;

import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot; safe to hand from the server thread to the storage worker. */
public record HydrationRecord(UUID uuid, double hydration, boolean hudEnabled, double drySeconds) {
    public HydrationRecord {
        Objects.requireNonNull(uuid, "uuid");
        if (!Double.isFinite(hydration) || hydration < 0 || hydration > 100)
            throw new IllegalArgumentException("Hydration must be finite and between 0 and 100");
        if (!Double.isFinite(drySeconds) || drySeconds < 0)
            throw new IllegalArgumentException("Dry duration must be finite and nonnegative");
    }

    public HydrationRecord withHud(boolean enabled) {
        return new HydrationRecord(uuid, hydration, enabled, drySeconds);
    }
}
