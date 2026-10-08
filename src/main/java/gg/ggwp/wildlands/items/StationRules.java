package gg.ggwp.wildlands.items;

import gg.ggwp.wildlands.survival.WaterQuality;
import java.util.Optional;

/** Filtration is a game abstraction; boiling remains an accessible route to safe water. */
public final class StationRules {
    private StationRules() {}
    public static boolean rainBiome(String name) {
        return !java.util.Set.of("desert", "badlands", "eroded_badlands", "wooded_badlands",
                "savanna", "savanna_plateau", "windswept_savanna").contains(name);
    }
    public static Optional<WaterQuality> filter(WaterQuality input, boolean improved) {
        if (input == WaterQuality.SALT || input == WaterQuality.CLEAN) return Optional.empty();
        if (improved) return Optional.of(WaterQuality.CLEAN);
        return input == WaterQuality.CONTAMINATED ? Optional.of(WaterQuality.QUESTIONABLE)
                : input == WaterQuality.QUESTIONABLE ? Optional.of(WaterQuality.UNTREATED) : Optional.empty();
    }
    public static double cloakWetness(double previous, double next, boolean raining, boolean immersed, double multiplier) {
        return raining && !immersed && next > previous ? previous + (next - previous) * multiplier : next;
    }
}
