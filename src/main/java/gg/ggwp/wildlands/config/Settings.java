package gg.ggwp.wildlands.config;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record Settings(String databaseFile, int saveIntervalSeconds, Map<String, Boolean> modules,
                       boolean debugEnabled) {
    public Settings { modules = Map.copyOf(modules); }

    public static Settings parse(ConfigurationSection yaml) {
        require(yaml.get("schema-version") instanceof Integer && yaml.getInt("schema-version") == 1,
                "schema-version must be 1");
        Object file = yaml.get("storage.file");
        require(file instanceof String && ((String) file).matches("[a-zA-Z0-9_-]+\\.db"),
                "storage.file must be a simple .db filename");
        Object interval = yaml.get("storage.save-interval-seconds");
        require(interval instanceof Integer && (int) interval >= 5 && (int) interval <= 3600,
                "storage.save-interval-seconds must be an integer between 5 and 3600");
        require(yaml.get("debug.enabled") instanceof Boolean, "debug.enabled must be true or false");
        ConfigurationSection section = yaml.getConfigurationSection("modules");
        require(section != null, "modules must be a mapping");
        var modules = new java.util.LinkedHashMap<String, Boolean>();
        for (String key : section.getKeys(false)) {
            require(java.util.Set.of("player-records", "hydration", "hud", "temperature", "wetness", "shelter", "seasons", "worldgen", "wildlife").contains(key), "Unknown module: " + key + " (later milestones are not installed)");
            require(section.get(key) instanceof Boolean, "modules." + key + " must be true or false");
            modules.put(key, section.getBoolean(key));
        }
        require(modules.containsKey("player-records"), "modules.player-records is required");
        // Existing foundation installs explicitly opt into new gameplay modules.
        modules.putIfAbsent("hydration", false);
        modules.putIfAbsent("hud", false);
        modules.putIfAbsent("temperature", false);
        modules.putIfAbsent("wetness", false);
        modules.putIfAbsent("shelter", false);
        modules.putIfAbsent("seasons", false);
        modules.putIfAbsent("worldgen", false);
        modules.putIfAbsent("wildlife", false);
        return new Settings((String) file, (int) interval, modules, yaml.getBoolean("debug.enabled"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
