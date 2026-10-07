package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.seasons.Season;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

/** Per-season balance and an explicit set of managed Overworld names. */
public record SeasonSettings(Set<String> worlds, int clockSeconds, int weatherSeconds,
        Map<Season, Profile> profiles) {
    public record Profile(int days, double temperatureDelta, double rainChance,
            double thunderChance, double cropMultiplier) {}

    public SeasonSettings {
        worlds = Set.copyOf(worlds);
        profiles = Map.copyOf(profiles);
    }

    public Map<Season, Long> durations() {
        var result = new EnumMap<Season, Long>(Season.class);
        profiles.forEach((season, profile) -> result.put(season, profile.days() * 24000L));
        return Map.copyOf(result);
    }

    public static SeasonSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        Object rawWorlds = yaml.get("worlds");
        if (!(rawWorlds instanceof List<?> list) || list.isEmpty() || list.size() > 32)
            throw new IllegalArgumentException("seasons.worlds must contain 1–32 world names");
        var worlds = new HashSet<String>();
        for (Object value : list) {
            if (!(value instanceof String name) || name.isBlank() || name.length() > 128 || !worlds.add(name))
                throw new IllegalArgumentException("Season world names must be unique nonblank text");
        }
        var profiles = new EnumMap<Season, Profile>(Season.class);
        ConfigurationSection seasons = yaml.getConfigurationSection("seasons");
        if (seasons == null) throw new IllegalArgumentException("Missing seasons profiles");
        var keys = new HashSet<String>();
        for (Season season : Season.values()) {
            String key = season.name().toLowerCase(Locale.ROOT).replace('_', '-');
            keys.add(key);
            profiles.put(season, new Profile(integer(seasons, key + ".days", 1, 3650),
                    number(seasons, key + ".temperature-delta", -20, 20),
                    number(seasons, key + ".rain-chance", 0, 1),
                    number(seasons, key + ".thunder-chance", 0, 1),
                    number(seasons, key + ".crop-multiplier", 0, 2)));
        }
        if (!seasons.getKeys(false).equals(keys)) throw new IllegalArgumentException("Unknown season profile");
        return new SeasonSettings(worlds, integer(yaml, "clock-seconds", 5, 300),
                integer(yaml, "weather-seconds", 30, 3600), profiles);
    }

    private static double number(ConfigurationSection yaml, String key, double min, double max) {
        if (!(yaml.get(key) instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() < min || number.doubleValue() > max)
            throw new IllegalArgumentException(key + " must be finite and between " + min + " and " + max);
        return number.doubleValue();
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer)) throw new IllegalArgumentException(key + " must be an integer");
        return (int) number(yaml, key, min, max);
    }
}
