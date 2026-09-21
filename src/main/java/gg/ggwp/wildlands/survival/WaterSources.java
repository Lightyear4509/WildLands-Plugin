package gg.ggwp.wildlands.survival;

/** Source classification is independent of visuals and the player's edition. */
public final class WaterSources {
    private WaterSources() {}
    public static WaterQuality classify(String biomeKey, boolean spring) {
        // Ocean water cannot become fresh merely because a marker overlaps it.
        if (biomeKey.contains("ocean")) return WaterQuality.SALT;
        if (spring) return WaterQuality.CLEAN;
        if (biomeKey.equals("minecraft:swamp") || biomeKey.equals("minecraft:mangrove_swamp"))
            return WaterQuality.CONTAMINATED;
        if (biomeKey.equals("minecraft:river") || biomeKey.equals("minecraft:frozen_river"))
            return WaterQuality.UNTREATED;
        return WaterQuality.QUESTIONABLE;
    }
}
