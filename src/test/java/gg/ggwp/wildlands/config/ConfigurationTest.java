package gg.ggwp.wildlands.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationTest {
    @TempDir Path directory;
    @Test void createsAndLoadsDefaults() throws Exception {
        var snapshot = new ConfigurationManager(directory).load();
        assertEquals("wildlands.db", snapshot.settings().databaseFile());
        assertEquals(30, snapshot.settings().saveIntervalSeconds());
        assertTrue(snapshot.settings().modules().get("player-records"));
        assertTrue(Files.exists(directory.resolve("messages.yml")));
        assertTrue(Files.exists(directory.resolve("hydration.yml")));
        assertTrue(Files.exists(directory.resolve("environment.yml")));
        assertEquals(0.025, snapshot.hydration().lossPerSecond());
        assertEquals(5, snapshot.environment().sampleSeconds());
    }
    @Test void rejectsMalformedYamlRatherThanOverwritingIt() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "modules: [broken");
        assertThrows(Exception.class, () -> new ConfigurationManager(directory).load());
        assertEquals("modules: [broken", Files.readString(directory.resolve("config.yml")));
    }
    @Test void rejectsInvalidTypesPathsIntervalsAndFutureModules() throws Exception {
        new ConfigurationManager(directory).load();
        for (Object[] change : new Object[][]{
                {"storage.file", "../outside.db"}, {"storage.save-interval-seconds", 0},
                {"storage.save-interval-seconds", "30"}, {"debug.enabled", "yes"},
                {"modules.player-records", "true"}, {"modules.seasons", true}, {"schema-version", 2}}) {
            var yaml = new YamlConfiguration();
            yaml.load(directory.resolve("config.yml").toFile());
            yaml.set((String) change[0], change[1]);
            assertThrows(IllegalArgumentException.class, () -> Settings.parse(yaml), change[0].toString());
        }
    }
    @Test void rejectsInvalidHydrationWithoutOverwritingTheFile() throws Exception {
        var manager = new ConfigurationManager(directory);
        manager.load();
        Path file = directory.resolve("hydration.yml");
        String invalid = Files.readString(file).replace("per-second: 0.025", "per-second: -1");
        Files.writeString(file, invalid);
        assertThrows(IllegalArgumentException.class, manager::load);
        assertEquals(invalid, Files.readString(file));
    }
    @Test void messagesMustHaveSupportedSchemaAndStrings() throws Exception {
        var manager = new ConfigurationManager(directory);
        manager.load();
        Files.writeString(directory.resolve("messages.yml"), "schema-version: 2\nprefix: hi");
        assertThrows(IllegalArgumentException.class, manager::load);
    }
}
