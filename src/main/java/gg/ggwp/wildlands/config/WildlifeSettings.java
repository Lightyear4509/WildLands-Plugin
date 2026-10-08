package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

public record WildlifeSettings(Set<String> worlds, int sampleTicks, int batchSize, int spawnSeconds,
        int maxPerWorld, int maxLoaded, double spawnChance, double health, double damage, double biteRange,
        double biteSeconds, double fireRadius, boolean huntPrey, JaguarBehavior.Limits limits) {
    public WildlifeSettings { worlds = Set.copyOf(worlds); Objects.requireNonNull(limits); }
    public static WildlifeSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        Object value = yaml.get("worlds");
        if (!(value instanceof List<?> list) || list.isEmpty() || list.size() > 32)
            throw new IllegalArgumentException("wildlife.worlds must list 1–32 world names");
        var worlds = new HashSet<String>();
        for (Object item : list) if (!(item instanceof String name) || name.isBlank() || name.length() > 128 || !worlds.add(name))
            throw new IllegalArgumentException("Wildlife world names must be unique nonblank strings");
        return new WildlifeSettings(worlds, integer(yaml, "sample-ticks", 10, 100), integer(yaml, "batch-size", 1, 32),
                integer(yaml, "spawn-seconds", 30, 600), integer(yaml, "max-per-world", 1, 128), integer(yaml, "max-loaded", 1, 256),
                number(yaml, "spawn-chance", 0, 1), number(yaml, "jaguar.health", 10, 40), number(yaml, "jaguar.damage", 1, 8),
                number(yaml, "jaguar.bite-range", 1, 3), number(yaml, "jaguar.bite-seconds", 1, 10),
                number(yaml, "jaguar.fire-radius", 2, 8), bool(yaml, "jaguar.hunt-prey"),
                new JaguarBehavior.Limits(number(yaml, "jaguar.detection-range", 8, 32), number(yaml, "jaguar.warning-range", 4, 16),
                        number(yaml, "jaguar.engagement-range", 2, 12), number(yaml, "jaguar.territory-radius", 16, 96),
                        number(yaml, "jaguar.warning-seconds", 2, 15), number(yaml, "jaguar.chase-seconds", 5, 60),
                        number(yaml, "jaguar.retreat-seconds", 5, 300), integer(yaml, "jaguar.deterrent-group-size", 2, 8)));
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer value) || value < min || value > max)
            throw new IllegalArgumentException("wildlife." + key + " must be an integer in " + min + ".." + max);
        return value;
    }
    private static double number(ConfigurationSection yaml, String key, double min, double max) {
        if (!(yaml.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue()) || value.doubleValue() < min || value.doubleValue() > max)
            throw new IllegalArgumentException("wildlife." + key + " must be a finite number in " + min + ".." + max);
        return value.doubleValue();
    }
    private static boolean bool(ConfigurationSection yaml, String key) {
        if (!(yaml.get(key) instanceof Boolean value)) throw new IllegalArgumentException("wildlife." + key + " must be boolean");
        return value;
    }
}
