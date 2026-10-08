package gg.ggwp.wildlands.config;

import org.bukkit.configuration.ConfigurationSection;

public record FoodSettings(double preservedSaturation) {
    public static FoodSettings parse(ConfigurationSection yaml) {
        if (!(yaml.get("schema-version") instanceof Integer v) || v != 1)
            throw new IllegalArgumentException("food.schema-version must be 1");
        if (!(yaml.get("preserved-saturation") instanceof Number n) || !Double.isFinite(n.doubleValue())
                || n.doubleValue() < 0 || n.doubleValue() > 4)
            throw new IllegalArgumentException("food.preserved-saturation must be a finite number in 0..4");
        return new FoodSettings(n.doubleValue());
    }
}
