package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.wildlife.Species;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

public record WildlifeExpansionSettings(Map<Species, Animal> animals) {
    public record Animal(boolean enabled, int maxPerWorld, int maxPerCell, double health,
                         double spawnChance, double avoidanceRange, double roamRadius, double fleeSeconds) {
        public Animal {
            if (maxPerWorld < 1 || maxPerWorld > 64 || maxPerCell < 1 || maxPerCell > 4
                    || !Double.isFinite(health) || health < 10 || health > 32
                    || !Double.isFinite(spawnChance) || spawnChance < 0 || spawnChance > 1
                    || !Double.isFinite(avoidanceRange) || avoidanceRange < 2 || avoidanceRange > 12
                    || !Double.isFinite(roamRadius) || roamRadius < 8 || roamRadius > 48
                    || !Double.isFinite(fleeSeconds) || fleeSeconds < 3 || fleeSeconds > 30)
                throw new IllegalArgumentException("Invalid passive wildlife settings");
        }
    }
    public WildlifeExpansionSettings { animals = Map.copyOf(animals); }
    public static WildlifeExpansionSettings disabled() {
        return new WildlifeExpansionSettings(Map.of(Species.CAPYBARA, new Animal(false, 12, 4, 16, .12, 4, 24, 10),
                Species.TAPIR, new Animal(false, 8, 2, 24, .08, 6, 32, 12)));
    }
    public Animal animal(Species species) { return Objects.requireNonNull(animals.get(species), "No settings for " + species); }
    public static WildlifeExpansionSettings parse(ConfigurationSection yaml) {
        if (!(yaml.get("schema-version") instanceof Integer version) || version != 1) throw new IllegalArgumentException("wildlife-expansion.yml schema-version must be 1");
        var animals = new EnumMap<Species, Animal>(Species.class);
        for (Species species : List.of(Species.CAPYBARA, Species.TAPIR)) {
            String p = species.key() + ".";
            if (!(yaml.get(p + "enabled") instanceof Boolean enabled)) throw new IllegalArgumentException(p + "enabled must be boolean");
            animals.put(species, new Animal(enabled, integer(yaml, p+"max-per-world"), integer(yaml,p+"max-per-cell"),
                    number(yaml,p+"health"), number(yaml,p+"spawn-chance"), number(yaml,p+"avoidance-range"),
                    number(yaml,p+"roam-radius"), number(yaml,p+"flee-seconds")));
        }
        return new WildlifeExpansionSettings(animals);
    }
    private static int integer(ConfigurationSection yaml, String key) {
        if (!(yaml.get(key) instanceof Integer value)) throw new IllegalArgumentException(key + " must be an integer"); return value;
    }
    private static double number(ConfigurationSection yaml, String key) {
        if (!(yaml.get(key) instanceof Number value)) throw new IllegalArgumentException(key + " must be numeric"); return value.doubleValue();
    }
}
