package gg.ggwp.wildlands.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigurationManager {
    public record Snapshot(Settings settings, Map<String, String> messages, HydrationSettings hydration,
                           EnvironmentSettings environment, SeasonSettings seasons, WorldgenSettings worldgen) {
        public Snapshot { messages = Map.copyOf(messages); java.util.Objects.requireNonNull(hydration); java.util.Objects.requireNonNull(environment); java.util.Objects.requireNonNull(seasons); java.util.Objects.requireNonNull(worldgen); }
        public String message(String key) { return messages.get("prefix") + messages.get(key); }
    }
    private final Path directory;
    public ConfigurationManager(Path directory) { this.directory = directory; }

    // Only called on the storage worker, including resource copying.
    public Snapshot load() throws IOException, InvalidConfigurationException {
        Files.createDirectories(directory);
        YamlConfiguration config = read("config.yml");
        YamlConfiguration messages = read("messages.yml");
        if (!(messages.get("schema-version") instanceof Integer) || messages.getInt("schema-version") != 1)
            throw new IllegalArgumentException("messages.yml schema-version must be 1");
        var values = new java.util.HashMap<String, String>();
        for (String key : new String[]{"prefix", "no-permission", "players-only", "unknown-command"}) {
            if (!(messages.get(key) instanceof String value) || value.length() > 512)
                throw new IllegalArgumentException("messages.yml: " + key + " must be text of at most 512 characters");
            values.put(key, value);
        }
        return new Snapshot(Settings.parse(config), values, HydrationSettings.parse(read("hydration.yml")),
                EnvironmentSettings.parse(read("environment.yml")), SeasonSettings.parse(read("seasons.yml")), WorldgenSettings.parse(read("worldgen.yml")));
    }

    private YamlConfiguration read(String name) throws IOException, InvalidConfigurationException {
        Path path = directory.resolve(name);
        if (!Files.exists(path)) {
            try (var input = ConfigurationManager.class.getClassLoader().getResourceAsStream(name)) {
                if (input == null) throw new IOException("Missing bundled resource " + name);
                Files.copy(input, path);
            }
        }
        var yaml = new YamlConfiguration();
        yaml.load(path.toFile()); // Unlike loadConfiguration, malformed YAML must fail explicitly.
        return yaml;
    }
}
