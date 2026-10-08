package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.storage.ExpeditionRecord;

public final class ExplorationRules {
    private ExplorationRules() {}
    public static String bearing(double dx, double dz) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz)) throw new IllegalArgumentException("Invalid direction");
        if (dx == 0 && dz == 0) return "HERE";
        double angle = Math.atan2(dx, -dz);
        int octant = Math.floorMod((int) Math.round(angle / (Math.PI / 4)), 8);
        return new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"}[octant];
    }
    public static ExpeditionRecord advance(ExpeditionRecord record, double campDistance, int discoveries, double departAt, double returnAt) {
        if (!Double.isFinite(campDistance) || campDistance < 0 || campDistance > 100_000_000 || discoveries < 0
                || !Double.isFinite(departAt) || !Double.isFinite(returnAt) || returnAt < 0 || departAt <= returnAt)
            throw new IllegalArgumentException("Invalid expedition sample");
        boolean active = record.active(); int trips = record.completedTrips();
        if (campDistance >= departAt) active = true;
        else if (active && campDistance <= returnAt) { active = false; trips = Math.min(10_000_000, trips + 1); }
        double longest = Math.max(record.longestDistance(), campDistance);
        return progress(new ExpeditionRecord(record.player(), record.target(), longest, trips, active, record.rank()), discoveries);
    }
    public static ExpeditionRecord progress(ExpeditionRecord record, int discoveries) {
        if (discoveries < 0) throw new IllegalArgumentException("Invalid discovery count");
        var rank = discoveries >= 10 && record.completedTrips() >= 1 && record.longestDistance() >= 1024 ? ExpeditionRecord.Rank.PATHFINDER
                : discoveries >= 5 ? ExpeditionRecord.Rank.EXPLORER : discoveries >= 1 ? ExpeditionRecord.Rank.SCOUT : ExpeditionRecord.Rank.NOVICE;
        if (record.rank().ordinal() > rank.ordinal()) rank = record.rank();
        return new ExpeditionRecord(record.player(), record.target(), record.longestDistance(), record.completedTrips(), record.active(), rank);
    }
}
