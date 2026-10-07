package gg.ggwp.wildlands.seasons;

/** The rainforest cycle, in progression order. */
public enum Season {
    DRY, TRANSITION_TO_WET, WET, MONSOON, TRANSITION_TO_DRY;

    public Season next() { return values()[(ordinal() + 1) % values().length]; }
}
