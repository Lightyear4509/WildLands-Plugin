package gg.ggwp.wildlands.config;

import gg.ggwp.wildlands.wildlife.Species;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;

public record WildlifeExpansionSettings(Map<Species, Animal> animals, Predator caiman, int jaguarMaxPerWorld) {
    public record Predator(gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Limits limits, double damage, double biteRange, double biteSeconds, double fireRadius) {
        public Predator {
            Objects.requireNonNull(limits);
            if (!Double.isFinite(damage) || damage < 1 || damage > 4 || !Double.isFinite(biteRange) || biteRange < 1 || biteRange > 3
                    || !Double.isFinite(biteSeconds) || biteSeconds < 1 || biteSeconds > 10 || !Double.isFinite(fireRadius) || fireRadius < 2 || fireRadius > 8)
                throw new IllegalArgumentException("Invalid Caiman limits");
        }
        static Predator defaults() { return new Predator(new gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Limits(12, 6, 3, 24, 4, 12, 12, 3), 2, 1.8, 2, 5); }
    }
    public WildlifeExpansionSettings(Map<Species, Animal> animals) { this(animals, Predator.defaults(), 32); }
    public record Animal(boolean enabled, int maxPerWorld, int maxPerCell, double health,
                         double spawnChance, double avoidanceRange, double roamRadius, double fleeSeconds) {
        public Animal {
            if (maxPerWorld < 1 || maxPerWorld > 64 || maxPerCell < 1 || maxPerCell > 4
                    || !Double.isFinite(health) || health < 4 || health > 32
                    || !Double.isFinite(spawnChance) || spawnChance < 0 || spawnChance > 1
                    || !Double.isFinite(avoidanceRange) || avoidanceRange < 2 || avoidanceRange > 12
                    || !Double.isFinite(roamRadius) || roamRadius < 8 || roamRadius > 48
                    || !Double.isFinite(fleeSeconds) || fleeSeconds < 3 || fleeSeconds > 30)
                throw new IllegalArgumentException("Invalid passive wildlife settings");
        }
    }
    public WildlifeExpansionSettings {
        var complete = new EnumMap<Species, Animal>(Species.class);
        for (Species species : Species.values()) if (species != Species.JAGUAR) complete.put(species, defaults(species, false));
        complete.putAll(animals); animals = Map.copyOf(complete); Objects.requireNonNull(caiman);
        if (jaguarMaxPerWorld < 1 || jaguarMaxPerWorld > 64) throw new IllegalArgumentException("Invalid Jaguar species cap");
    }
    private static Animal defaults(Species species, boolean enabled) { return switch (species) {
        case CAPYBARA -> new Animal(enabled, 12, 4, 16, .12, 4, 24, 10);
        case TAPIR -> new Animal(enabled, 8, 2, 24, .08, 6, 32, 12);
        case TOUCAN -> new Animal(enabled, 6, 2, 6, .12, 3, 24, 6);
        case MONKEY -> new Animal(enabled, 6, 3, 12, .1, 4, 24, 8);
        case FROG -> new Animal(enabled, 8, 4, 6, .18, 2, 12, 5);
        case CAIMAN -> new Animal(enabled, 4, 1, 20, .06, 6, 24, 12);
        case JAGUAR -> throw new IllegalArgumentException("Jaguar uses wildlife.yml");
    }; }
    public static WildlifeExpansionSettings disabled() {
        return new WildlifeExpansionSettings(Map.of(Species.CAPYBARA, new Animal(false, 12, 4, 16, .12, 4, 24, 10),
                Species.TAPIR, new Animal(false, 8, 2, 24, .08, 6, 32, 12)));
    }
    public Animal animal(Species species) { return Objects.requireNonNull(animals.get(species), "No settings for " + species); }
    public static WildlifeExpansionSettings parse(ConfigurationSection yaml) {
        if (!(yaml.get("schema-version") instanceof Integer version) || version != 1) throw new IllegalArgumentException("wildlife-expansion.yml schema-version must be 1");
        var animals = new EnumMap<Species, Animal>(Species.class);
        for (Species species : Species.values()) {
            if (species == Species.JAGUAR) continue;
            String p = species.key() + ".";
            if (!yaml.contains(species.key()) && species != Species.CAPYBARA && species != Species.TAPIR) { animals.put(species, defaults(species, false)); continue; }
            if (!(yaml.get(p + "enabled") instanceof Boolean enabled)) throw new IllegalArgumentException(p + "enabled must be boolean");
            animals.put(species, new Animal(enabled, integer(yaml, p+"max-per-world"), integer(yaml,p+"max-per-cell"),
                    number(yaml,p+"health"), number(yaml,p+"spawn-chance"), number(yaml,p+"avoidance-range"),
                    number(yaml,p+"roam-radius"), number(yaml,p+"flee-seconds")));
        }
        Predator predator = Predator.defaults();
        if (yaml.contains("caiman")) {
            var limits = new gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Limits(number(yaml,"caiman.detection-range"), number(yaml,"caiman.warning-range"),
                    number(yaml,"caiman.engagement-range"), number(yaml,"caiman.roam-radius"), number(yaml,"caiman.warning-seconds"),
                    number(yaml,"caiman.chase-seconds"), number(yaml,"caiman.flee-seconds"), integer(yaml,"caiman.deterrent-group-size"));
            predator = new Predator(limits, number(yaml,"caiman.damage"), number(yaml,"caiman.bite-range"), number(yaml,"caiman.bite-seconds"), number(yaml,"caiman.fire-radius"));
        }
        return new WildlifeExpansionSettings(animals, predator, yaml.contains("jaguar-max-per-world") ? integer(yaml,"jaguar-max-per-world") : 32);
    }
    private static int integer(ConfigurationSection yaml, String key) {
        if (!(yaml.get(key) instanceof Integer value)) throw new IllegalArgumentException(key + " must be an integer"); return value;
    }
    private static double number(ConfigurationSection yaml, String key) {
        if (!(yaml.get(key) instanceof Number value)) throw new IllegalArgumentException(key + " must be numeric"); return value.doubleValue();
    }
}
