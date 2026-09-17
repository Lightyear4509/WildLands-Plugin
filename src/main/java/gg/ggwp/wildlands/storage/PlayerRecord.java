package gg.ggwp.wildlands.storage;

import java.util.UUID;

public record PlayerRecord(UUID uuid, String lastKnownName, long firstSeen, long lastSeen) {
    public PlayerRecord {
        java.util.Objects.requireNonNull(uuid);
        java.util.Objects.requireNonNull(lastKnownName);
        if (firstSeen < 0 || lastSeen < firstSeen) throw new IllegalArgumentException("Invalid record timestamps");
    }
}
