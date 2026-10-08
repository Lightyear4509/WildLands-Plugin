package gg.ggwp.wildlands.config;

import org.bukkit.configuration.ConfigurationSection;

public record LandmarkSettings(int sampleSeconds, int batchSize, int discoveryRadius, int verticalRange,
        int maxPerWorld, int departAt, int returnAt, boolean showCoordinates) {
    public static LandmarkSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        if (!(yaml.get("show-coordinates") instanceof Boolean coordinates)) throw new IllegalArgumentException("landmarks.show-coordinates must be boolean");
        int depart = integer(yaml, "expedition.depart-distance", 128, 4096), back = integer(yaml, "expedition.return-distance", 8, 64);
        return new LandmarkSettings(integer(yaml, "sample-seconds", 1, 10), integer(yaml, "batch-size", 1, 64),
                integer(yaml, "discovery-radius", 8, 64), integer(yaml, "vertical-range", 4, 32),
                integer(yaml, "max-natural-per-world", 64, 4096), depart, back, coordinates);
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer value) || value < min || value > max)
            throw new IllegalArgumentException("landmarks." + key + " must be an integer in " + min + ".." + max);
        return value;
    }
}
