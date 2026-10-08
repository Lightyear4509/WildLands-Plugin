package gg.ggwp.wildlands.storage;

import java.util.*;

public record DiscoveryRecord(UUID player, UUID landmark, long firstSeen) {
    public record Key(UUID player, UUID landmark) {}
    public DiscoveryRecord {
        Objects.requireNonNull(player); Objects.requireNonNull(landmark);
        if (firstSeen < 0) throw new IllegalArgumentException("Invalid discovery timestamp");
    }
    public Key key() { return new Key(player, landmark); }
}
