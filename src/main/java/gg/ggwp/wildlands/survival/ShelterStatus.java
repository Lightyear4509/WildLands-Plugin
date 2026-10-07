package gg.ggwp.wildlands.survival;

/** A small, player-local shelter sample. No structure shape is prescribed. */
public record ShelterStatus(boolean roofed, boolean enclosed, boolean dryGround, boolean nearbyCampfire) {
    public boolean protectedFromRain() { return roofed; }
    public boolean shaded() { return roofed; }
    public String label() {
        if (roofed && enclosed && dryGround) return "Sheltered";
        if (roofed) return "Protected";
        if (enclosed) return "Partially enclosed";
        return "Exposed";
    }
}
