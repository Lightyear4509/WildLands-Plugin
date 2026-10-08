package gg.ggwp.wildlands.config;

import org.bukkit.configuration.ConfigurationSection;

/** Generation settings are frozen for each world; changing them requires a new world. */
public record WorldgenSettings(int version, int seaLevel, double treeDensity, boolean caves, boolean ores) {
    public WorldgenSettings {
        if (version < 1 || version > 2 || seaLevel < 32 || seaLevel > 128 || !Double.isFinite(treeDensity)
                || treeDensity < 0 || treeDensity > 1)
            throw new IllegalArgumentException("Invalid world generator settings");
    }
    public static WorldgenSettings parse(ConfigurationSection yaml) {
        if (!(yaml.get("schema-version") instanceof Integer schema) || schema != 1
                || !(yaml.get("generator-version") instanceof Integer version)
                || !(yaml.get("sea-level") instanceof Integer sea)
                || !(yaml.get("tree-density") instanceof Number density)
                || !(yaml.get("caves") instanceof Boolean caves)
                || !(yaml.get("ores") instanceof Boolean ores))
            throw new IllegalArgumentException("worldgen.yml has invalid field types or schema");
        return new WorldgenSettings(version, sea, density.doubleValue(), caves, ores);
    }
    public void write(ConfigurationSection yaml) {
        yaml.set("schema-version", 1); yaml.set("generator-version", version); yaml.set("sea-level", seaLevel);
        yaml.set("tree-density", treeDensity); yaml.set("caves", caves); yaml.set("ores", ores);
    }
}
