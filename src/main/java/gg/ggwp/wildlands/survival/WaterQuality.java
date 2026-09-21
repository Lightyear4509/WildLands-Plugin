package gg.ggwp.wildlands.survival;

/** Water provenance, ordered conservatively for mixing in a container. */
public enum WaterQuality {
    CLEAN, UNTREATED, QUESTIONABLE, CONTAMINATED, SALT;

    public boolean canBoil() { return this != SALT; }

    public WaterQuality boiled() {
        // Boiling is not distillation: salt remains in the vessel.
        return canBoil() ? CLEAN : SALT;
    }

    public WaterQuality mix(WaterQuality other) {
        java.util.Objects.requireNonNull(other, "other");
        return ordinal() >= other.ordinal() ? this : other;
    }
}
