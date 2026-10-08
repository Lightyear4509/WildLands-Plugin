package gg.ggwp.wildlands.config;

import org.bukkit.configuration.ConfigurationSection;

public record CraftingSettings(int sampleSeconds, int batchSize, int maxLoadedStations,
        double cloakRainMultiplier, double stoveFuelMultiplier) {
    public static CraftingSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        return new CraftingSettings(integer(yaml, "sample-seconds", 5, 60), integer(yaml, "batch-size", 1, 32),
                integer(yaml, "max-loaded-stations", 16, 4096), number(yaml, "rain-cloak.multiplier", 0.1, 1),
                number(yaml, "improved-stove.fuel-multiplier", 1, 4));
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer value) || value < min || value > max)
            throw new IllegalArgumentException("crafting." + key + " must be an integer in " + min + ".." + max);
        return value;
    }
    private static double number(ConfigurationSection yaml, String key, double min, double max) {
        if (!(yaml.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue())
                || value.doubleValue() < min || value.doubleValue() > max)
            throw new IllegalArgumentException("crafting." + key + " must be a finite number in " + min + ".." + max);
        return value.doubleValue();
    }
}
