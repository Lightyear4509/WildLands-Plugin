package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.UUID;

/** Null UUID denotes a reserved world whose first creation has not completed yet. */
public record WorldRecord(String name, UUID uuid, long seed, WorldgenSettings settings) {
    public WorldRecord {
        if (name == null || !name.matches("[a-z][a-z0-9_-]{0,47}")
                || name.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9]|overworld|the_nether|the_end)"))
            throw new IllegalArgumentException("World name must be a safe lowercase directory name");
        java.util.Objects.requireNonNull(settings);
    }
    public WorldRecord withUuid(UUID uuid) { return new WorldRecord(name, java.util.Objects.requireNonNull(uuid), seed, settings); }
}
