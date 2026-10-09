package gg.ggwp.wildlands.core;

import gg.ggwp.wildlands.commands.WildlandsCommand;
import gg.ggwp.wildlands.config.*;
import gg.ggwp.wildlands.crossplay.CrossplayService;
import gg.ggwp.wildlands.storage.StorageService;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

public final class WildlandsPlugin extends JavaPlugin {
    public enum State { STARTING, READY, FAILED, STOPPING }
    private volatile State state = State.STARTING;
    private StorageService storage;
    private ConfigurationManager configuration;
    private ConfigurationManager.Snapshot config;
    private final ModuleManager modules = new ModuleManager();
    private PlayerManager players;
    private gg.ggwp.wildlands.survival.HydrationService hydration;
    private gg.ggwp.wildlands.survival.ShelterService shelter;
    private gg.ggwp.wildlands.survival.EnvironmentService environment;
    private gg.ggwp.wildlands.seasons.SeasonManager seasons;
    private gg.ggwp.wildlands.world.WorldManager worlds;
    private java.util.List<gg.ggwp.wildlands.storage.WorldRecord> savedWorlds;
    private java.util.List<gg.ggwp.wildlands.storage.WildlifeRecord> savedWildlife;
    private gg.ggwp.wildlands.wildlife.WildlifeManager wildlife;
    private gg.ggwp.wildlands.wildlife.presentation.WildlifePresentation presentation;
    private gg.ggwp.wildlands.items.CustomItemManager crafting;
    private gg.ggwp.wildlands.world.LandmarkManager landmarks;
    private java.util.List<gg.ggwp.wildlands.storage.LandmarkRecord> savedLandmarks;
    private CrossplayService crossplay;
    private boolean reloading;
    private final gg.ggwp.wildlands.survival.CauldronWater cauldronWater = new gg.ggwp.wildlands.survival.CauldronWater();

    @Override public void onEnable() {
        state = State.STARTING;
        storage = new StorageService(getLogger());
        configuration = new ConfigurationManager(getDataFolder().toPath());
        crossplay = new CrossplayService(this);
        crossplay.refresh();
        var levelDirectory = getServer().getLevelDirectory();
        getServer().getPluginManager().registerEvents(crossplay, this);
        var handler = new WildlandsCommand(this);
        var command = Objects.requireNonNull(getCommand("wildlands"), "Missing wildlands command");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        var hydrationHandler = new gg.ggwp.wildlands.commands.HydrationCommand(this);
        var hydrationCommand = Objects.requireNonNull(getCommand("hydration"));
        hydrationCommand.setExecutor(hydrationHandler);
        hydrationCommand.setTabCompleter(hydrationHandler);
        var seasonHandler = new gg.ggwp.wildlands.commands.SeasonCommand(this);
        var seasonCommand = Objects.requireNonNull(getCommand("season"));
        seasonCommand.setExecutor(seasonHandler); seasonCommand.setTabCompleter(seasonHandler);
        var explorationHandler = new gg.ggwp.wildlands.commands.ExplorationCommand(this);
        for (String name : new String[]{"landmark", "landmarks"}) {
            var explorationCommand = Objects.requireNonNull(getCommand(name));
            explorationCommand.setExecutor(explorationHandler); explorationCommand.setTabCompleter(explorationHandler);
        }
        storage.submit(() -> {
            var loaded = configuration.load();
            storage.open(getDataFolder().toPath().resolve(loaded.settings().databaseFile()));
            savedWorlds = storage.worlds();
            savedWildlife = storage.wildlife();
            savedLandmarks = storage.landmarks();
            for (var world : savedWorlds) if (world.uuid() != null) {
                var metadata = gg.ggwp.wildlands.world.WorldPaths.dimension(levelDirectory, world.name()).resolve("data/paper/metadata.dat");
                if (!java.nio.file.Files.isRegularFile(metadata))
                    throw new java.io.IOException("Registered world metadata is missing: " + world.name() + "; restore its backup");
            }
            return loaded;
        }).whenComplete((loaded, failure) -> onMain(() -> {
            if (failure != null) { failStartup(failure); return; }
            try {
                config = loaded;
                worlds = new gg.ggwp.wildlands.world.WorldManager(this, storage, savedWorlds);
                modules.register(worlds);
                presentation = new gg.ggwp.wildlands.wildlife.presentation.WildlifePresentation(this);
                presentation.initialize();
                wildlife = new gg.ggwp.wildlands.wildlife.WildlifeManager(this, storage, savedWildlife);
                wildlife.initialize(); modules.register(wildlife);
                getServer().getPluginManager().registerEvents(cauldronWater, this);
                players = new PlayerManager(this, storage, loaded.settings().saveIntervalSeconds());
                modules.register(players);
                hydration = new gg.ggwp.wildlands.survival.HydrationService(this, storage);
                modules.register(hydration);
                modules.register(new gg.ggwp.wildlands.ui.HudManager(this, hydration));
                seasons = new gg.ggwp.wildlands.seasons.SeasonManager(this, storage);
                modules.register(seasons);
                shelter = new gg.ggwp.wildlands.survival.ShelterService(this);
                environment = new gg.ggwp.wildlands.survival.EnvironmentService(this, storage, shelter);
                modules.register(environment.temperatureModule());
                modules.register(environment.wetnessModule());
                modules.register(shelter);
                crafting = new gg.ggwp.wildlands.items.CustomItemManager(this);
                crafting.initialize(); modules.register(crafting);
                modules.register(new gg.ggwp.wildlands.survival.NutritionService(this, crafting.items()));
                landmarks = new gg.ggwp.wildlands.world.LandmarkManager(this, storage, savedLandmarks);
                modules.register(landmarks);
                modules.apply(loaded.settings().modules());
                worlds.restore();
                state = State.READY;
                getLogger().info("Foundation ready. Geyser: " + crossplay.geyserStatus()
                        + "; Floodgate: " + crossplay.floodgateStatus());
            } catch (Exception error) { failStartup(error); }
        }));
    }
    private void failStartup(Throwable failure) {
        state = State.FAILED;
        report("Foundation startup failed; disabling plugin without modifying gameplay", failure);
        getServer().getPluginManager().disablePlugin(this);
    }
    public void reload(Consumer<String> reply) {
        if (reloading) { reply.accept("A reload is already running."); return; }
        reloading = true;
        storage.submit(configuration::load).whenComplete((candidate, failure) -> onMain(() -> {
            try {
                if (failure != null) throw new CompletionException(failure);
                if (!candidate.settings().databaseFile().equals(config.settings().databaseFile())
                        || candidate.settings().saveIntervalSeconds() != config.settings().saveIntervalSeconds())
                    throw new IllegalArgumentException("Storage settings require a server restart; no settings changed");
                if (candidate.hydration().boilingSeconds() != config.hydration().boilingSeconds())
                    throw new IllegalArgumentException("Boiling duration changes require a server restart; no settings changed");
                if (candidate.environment().sampleSeconds() != config.environment().sampleSeconds())
                    throw new IllegalArgumentException("Environment sampling interval changes require a server restart; no settings changed");
                if (candidate.seasons().clockSeconds() != config.seasons().clockSeconds())
                    throw new IllegalArgumentException("Season clock interval changes require a server restart; no settings changed");
                if (candidate.wildlife().sampleTicks() != config.wildlife().sampleTicks()
                        || candidate.wildlife().spawnSeconds() != config.wildlife().spawnSeconds())
                    throw new IllegalArgumentException("Wildlife scheduling changes require a server restart; no settings changed");
                if (candidate.crafting().sampleSeconds() != config.crafting().sampleSeconds())
                    throw new IllegalArgumentException("Crafting interval changes require a server restart; no settings changed");
                if (candidate.landmarks().sampleSeconds() != config.landmarks().sampleSeconds())
                    throw new IllegalArgumentException("Discovery sampling changes require a server restart; no settings changed");
                if (!candidate.visuals().javaUrl().equals(config.visuals().javaUrl()) || !candidate.visuals().javaSha1().equals(config.visuals().javaSha1()))
                    throw new IllegalArgumentException("Wildlife pack delivery changes require a server restart; no settings changed");
                var previous = config;
                config = candidate;
                try {
                    modules.apply(candidate.settings().modules(), () -> config = previous);
                } catch (Exception error) {
                    config = previous;
                    throw error;
                }
                crossplay.refresh();
                wildlife.configurationChanged();
                presentation.configurationChanged();
                crafting.configurationChanged();
                reply.accept("Configuration reloaded.");
            } catch (Exception error) {
                report("Configuration reload rejected; previous configuration retained", error);
                reply.accept("Reload rejected. Previous configuration retained; see the server log.");
            } finally { reloading = false; }
        }));
    }
    public void onMain(Runnable task) {
        if (state == State.STOPPING || !isEnabled()) return;
        try {
            getServer().getScheduler().runTask(this, () -> {
                if (state != State.STOPPING && isEnabled()) task.run();
            });
        } catch (IllegalPluginAccessException ignored) { /* Server disabled plugin between checks. */ }
    }
    public void report(String message, Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        getLogger().log(Level.SEVERE, message, failure);
    }
    @Override public void onDisable() {
        state = State.STOPPING;
        try { modules.close(); }
        catch (Exception error) { report("Module shutdown failed", error); }
        if (storage != null) storage.close();
    }
    public gg.ggwp.wildlands.survival.CauldronWater cauldronWater() { return cauldronWater; }
    public State state() { return state; }
    public ConfigurationManager.Snapshot configuration() { return config; }
    public ModuleManager modules() { return modules; }
    public gg.ggwp.wildlands.survival.HydrationService hydration() { return hydration; }
    public gg.ggwp.wildlands.survival.EnvironmentService environment() { return environment; }
    public gg.ggwp.wildlands.seasons.SeasonManager seasons() { return seasons; }
    public gg.ggwp.wildlands.world.WorldManager worlds() { return worlds; }
    public gg.ggwp.wildlands.wildlife.WildlifeManager wildlife() { return wildlife; }
    public gg.ggwp.wildlands.wildlife.presentation.WildlifePresentation presentation() { return presentation; }
    public gg.ggwp.wildlands.items.CustomItemManager crafting() { return crafting; }
    public gg.ggwp.wildlands.world.LandmarkManager landmarks() { return landmarks; }
    public gg.ggwp.wildlands.survival.ShelterService shelter() { return shelter; }
    public PlayerManager players() { return players; }
    public StorageService storage() { return storage; }
    public CrossplayService crossplay() { return crossplay; }
}
