package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.survival.WaterQuality;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

public record HydrationSettings(double lossPerSecond, double sprintMultiplier, double swimMultiplier,
        double combatMultiplier, int combatMemorySeconds, double recoveryBelow, double recoveryMultiplier,
        double severeAt, int damageDelaySeconds, int damageIntervalSeconds, double damage,
        int boilingSeconds, boolean defaultHud, double respawnHydration, Map<WaterQuality, Drink> water, Set<Spring> springs) {
    public record Drink(double restore, double illnessChance, int illnessSeconds) {}
    public record Spring(String world, int x, int y, int z) {}
    public HydrationSettings { water = Map.copyOf(water); springs = Set.copyOf(springs); }

    public static HydrationSettings parse(ConfigurationSection yaml) {
        integer(yaml, "schema-version", 1, 1);
        double recovery = number(yaml, "dehydration.recovery-below", 1, 100);
        double severe = number(yaml, "dehydration.severe-at", 0, 99);
        if (severe >= recovery) throw new IllegalArgumentException("Severe dehydration must be below recovery threshold");
        if (!(yaml.get("hud.default-enabled") instanceof Boolean))
            throw new IllegalArgumentException("hud.default-enabled must be true or false");
        Map<WaterQuality, Drink> drinks = new EnumMap<>(WaterQuality.class);
        for (WaterQuality quality : WaterQuality.values()) {
            String key = "water." + quality.name().toLowerCase(Locale.ROOT);
            double restore = number(yaml, key + ".restore", -100, 100);
            double chance = number(yaml, key + ".illness-chance", 0, 1);
            int duration = integer(yaml, key + ".illness-seconds", 0, 120);
            if (quality == WaterQuality.CLEAN && chance != 0)
                throw new IllegalArgumentException("Clean water must not cause illness");
            if (quality == WaterQuality.SALT && restore > 0)
                throw new IllegalArgumentException("Salt water must not restore hydration");
            drinks.put(quality, new Drink(restore, chance, duration));
        }
        return new HydrationSettings(number(yaml, "depletion.per-second", 0, 1),
                number(yaml, "depletion.sprint-multiplier", 1, 10),
                number(yaml, "depletion.swim-multiplier", 1, 10),
                number(yaml, "depletion.combat-multiplier", 1, 10),
                integer(yaml, "depletion.combat-memory-seconds", 1, 60), recovery,
                number(yaml, "dehydration.recovery-multiplier", 0, 1), severe,
                integer(yaml, "dehydration.damage-delay-seconds", 30, 3600),
                integer(yaml, "dehydration.damage-interval-seconds", 10, 600),
                number(yaml, "dehydration.damage", 0, 4), integer(yaml, "boiling-seconds", 1, 600),
                yaml.getBoolean("hud.default-enabled"), number(yaml, "respawn-hydration", 1, 100), drinks, springs(yaml));
    }

    private static Set<Spring> springs(ConfigurationSection yaml) {
        Object raw = yaml.get("springs");
        if (raw == null) return Set.of();
        if (!(raw instanceof List<?> entries) || entries.size() > 10000)
            throw new IllegalArgumentException("springs must be a list of at most 10000 locations");
        Set<Spring> result = new HashSet<>();
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> map) || !(map.get("world") instanceof String world)
                    || world.isBlank() || !(map.get("x") instanceof Integer x)
                    || !(map.get("y") instanceof Integer y) || !(map.get("z") instanceof Integer z)
                    || Math.abs((long) x) > 30000000 || Math.abs((long) z) > 30000000 || y < -2032 || y > 2031)
                throw new IllegalArgumentException("Each spring requires world and valid integer x/y/z coordinates");
            if (!result.add(new Spring(world, x, y, z))) throw new IllegalArgumentException("Duplicate spring location");
        }
        return result;
    }

    private static double number(ConfigurationSection yaml, String key, double min, double max) {
        Object raw = yaml.get(key);
        if (!(raw instanceof Number value) || !Double.isFinite(value.doubleValue())
                || value.doubleValue() < min || value.doubleValue() > max)
            throw new IllegalArgumentException(key + " must be a finite number between " + min + " and " + max);
        return ((Number) raw).doubleValue();
    }
    private static int integer(ConfigurationSection yaml, String key, int min, int max) {
        if (!(yaml.get(key) instanceof Integer)) throw new IllegalArgumentException(key + " must be an integer");
        return (int) number(yaml, key, min, max);
    }
}
