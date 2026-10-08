package gg.ggwp.wildlands.world;

public enum LandmarkKind {
    RUINS, RIVER_BEND, WATERFALL, WETLAND_POOL, HIGHLAND_LOOKOUT, ROCKY_ESCARPMENT,
    BAMBOO_GROVE, CLEARING, RAINFOREST_GROVE, CAMP;
    public String label() { return name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '); }
}
