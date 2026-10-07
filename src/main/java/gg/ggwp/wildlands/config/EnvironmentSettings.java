package gg.ggwp.wildlands.config;

import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

/** Validated environment tuning. Temperatures are Celsius-like gameplay values. */
public record EnvironmentSettings(int sampleSeconds, double defaultTemperature, double seaLevel,
        double elevationCoolingPerBlock, double rainCooling, double waterCooling, double shadeCooling,
        double campfireWarmth, int campfireRadius, double hotAt, double coldAt, double wetColdAt,
        double hotHydrationMultiplier, double wetnessRainPerSecond, double wetnessSwimPerSecond,
        double wetnessDryPerSecond, double wetnessShelterDryMultiplier, int roofSearchHeight,
        int enclosureRadius, int campfireSearchHeight, double coldWetRecoveryMultiplier, double dailyTemperatureAmplitude,
        double sprintWarmth) {
    public static EnvironmentSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        double cold = number(yaml, "temperature.cold-at", -50, 40);
        double hot = number(yaml, "temperature.hot-at", cold + 1, 60);
        return new EnvironmentSettings(integer(yaml, "sampling.seconds", 1, 10),
                number(yaml, "temperature.default", -20, 50), number(yaml, "temperature.sea-level", -64, 320),
                number(yaml, "temperature.elevation-cooling-per-block", 0, 1),
                number(yaml, "temperature.rain-cooling", 0, 20), number(yaml, "temperature.water-cooling", 0, 20),
                number(yaml, "temperature.shade-cooling", 0, 20), number(yaml, "temperature.campfire-warmth", 0, 30),
                integer(yaml, "temperature.campfire-radius", 1, 12), hot, cold,
                number(yaml, "temperature.wet-cold-at", -50, cold), number(yaml, "temperature.hot-hydration-multiplier", 1, 5),
                number(yaml, "wetness.rain-per-second", 0, 100), number(yaml, "wetness.swim-per-second", 0, 100),
                number(yaml, "wetness.dry-per-second", 0, 100), number(yaml, "wetness.shelter-dry-multiplier", 1, 10),
                integer(yaml, "shelter.roof-search-height", 1, 32), integer(yaml, "shelter.enclosure-radius", 1, 6),
                integer(yaml, "shelter.campfire-search-height", 0, 6),
                yaml.contains("temperature.cold-wet-recovery-multiplier")
                        ? number(yaml, "temperature.cold-wet-recovery-multiplier", 0, 1) : 0.75,
                yaml.contains("temperature.daily-amplitude") ? number(yaml, "temperature.daily-amplitude", 0, 20) : 3,
                yaml.contains("temperature.sprint-warmth") ? number(yaml, "temperature.sprint-warmth", 0, 10) : 2);
    }
    private static double number(ConfigurationSection yaml, String key, double min, double max) {
        Object raw = yaml.get(key);
        if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() < min || number.doubleValue() > max)
            throw new IllegalArgumentException(key + " must be a finite number between " + min + " and " + max);
        return number.doubleValue();
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer)) throw new IllegalArgumentException(key + " must be an integer");
        return (int) number(yaml, key, min, max);
    }
}
