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
    private CrossplayService crossplay;
    private boolean reloading;
    private final gg.ggwp.wildlands.survival.CauldronWater cauldronWater = new gg.ggwp.wildlands.survival.CauldronWater();

    @Override public void onEnable() {
        state = State.STARTING;
        storage = new StorageService(getLogger());
        configuration = new ConfigurationManager(getDataFolder().toPath());
        crossplay = new CrossplayService(this);
        crossplay.refresh();
        getServer().getPluginManager().registerEvents(crossplay, this);
        var handler = new WildlandsCommand(this);
        var command = Objects.requireNonNull(getCommand("wildlands"), "Missing wildlands command");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        var hydrationHandler = new gg.ggwp.wildlands.commands.HydrationCommand(this);
        var hydrationCommand = Objects.requireNonNull(getCommand("hydration"));
        hydrationCommand.setExecutor(hydrationHandler);
        hydrationCommand.setTabCompleter(hydrationHandler);
        storage.submit(() -> {
            var loaded = configuration.load();
            storage.open(getDataFolder().toPath().resolve(loaded.settings().databaseFile()));
            return loaded;
        }).whenComplete((loaded, failure) -> onMain(() -> {
            if (failure != null) { failStartup(failure); return; }
            try {
                config = loaded;
                getServer().getPluginManager().registerEvents(cauldronWater, this);
                players = new PlayerManager(this, storage, loaded.settings().saveIntervalSeconds());
                modules.register(players);
                hydration = new gg.ggwp.wildlands.survival.HydrationService(this, storage);
                modules.register(hydration);
                modules.register(new gg.ggwp.wildlands.ui.HudManager(this, hydration));
                modules.apply(loaded.settings().modules());
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
                modules.apply(candidate.settings().modules());
                config = candidate;
                crossplay.refresh();
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
    public PlayerManager players() { return players; }
    public StorageService storage() { return storage; }
    public CrossplayService crossplay() { return crossplay; }
}
