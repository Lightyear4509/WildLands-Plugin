package gg.ggwp.wildlands.storage;

import java.util.*;

public record ExpeditionRecord(UUID player, UUID target, double longestDistance, int completedTrips, boolean active, Rank rank) {
    public enum Rank { NOVICE, SCOUT, EXPLORER, PATHFINDER }
    public ExpeditionRecord {
        Objects.requireNonNull(player); Objects.requireNonNull(rank);
        if (!Double.isFinite(longestDistance) || longestDistance < 0 || longestDistance > 100_000_000
                || completedTrips < 0 || completedTrips > 10_000_000) throw new IllegalArgumentException("Invalid expedition record");
    }
    public static ExpeditionRecord initial(UUID player) { return new ExpeditionRecord(player, null, 0, 0, false, Rank.NOVICE); }
    public ExpeditionRecord tracking(UUID id) { return new ExpeditionRecord(player, id, longestDistance, completedTrips, active, rank); }
}
