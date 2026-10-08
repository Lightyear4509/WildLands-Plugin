package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.*;

/** Owns explicitly created worlds and restores their frozen generator after restart. */
public final class WorldManager implements WildlandsModule {
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final Map<String, WorldRecord> records = new LinkedHashMap<>();
    private final Set<String> creating = new HashSet<>();
    private boolean enabled;
    private org.bukkit.scheduler.BukkitTask retryTask;
    public WorldManager(WildlandsPlugin plugin, StorageService storage, List<WorldRecord> saved) {
        this.plugin = plugin; this.storage = storage;
        saved.forEach(record -> records.put(record.name(), record));
    }
    @Override public String id() { return "worldgen"; }
    @Override public void enable() {
        enabled = true;
        retryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> storage.submit(() -> {
            storage.flushWorldIdentities(); return null;
        }).exceptionally(failure -> { plugin.report("World identity retry failed", failure); return null; }), 600, 600);
    }
    @Override public void disable() {
        enabled = false;
        if (retryTask != null) { retryTask.cancel(); retryTask = null; }
    }
    public void restore() {
        for (WorldRecord record : List.copyOf(records.values())) {
            try { load(record); }
            catch (Exception failure) { plugin.report("Rainforest world could not be restored: " + record.name(), failure); }
        }
    }
    private World load(WorldRecord record) {
        World existing = plugin.getServer().getWorld(record.name());
        if (existing != null) {
            if (!(existing.getGenerator() instanceof WorldGenerator generator) || !generator.settings().equals(record.settings())
                    || existing.getSeed() != record.seed() || record.uuid() != null && !record.uuid().equals(existing.getUID()))
                throw new IllegalStateException("Loaded world does not match the registered rainforest profile");
            return existing;
        }
        var directory = WorldPaths.dimension(plugin.getServer().getLevelDirectory(), record.name());
        if (record.uuid() != null && !Files.isDirectory(directory))
            throw new IllegalStateException("Registered world directory is missing; restore its backup before loading");
        World world = WorldCreator.ofKey(NamespacedKey.minecraft(record.name())).environment(World.Environment.NORMAL).seed(record.seed())
                .generator(new WorldGenerator(record.settings())).createWorld();
        if (world == null) throw new IllegalStateException("Paper refused world creation");
        if (world.getSeed() != record.seed() || record.uuid() != null && !record.uuid().equals(world.getUID()))
            throw new IllegalStateException("World seed/UUID does not match the registered profile; restore its backup");
        if (record.uuid() == null) {
            chooseSpawn(world, record);
            var identified = record.withUuid(world.getUID());
            records.put(record.name(), identified);
            storage.submit(() -> { storage.identifyWorld(identified); return null; })
                    .exceptionally(error -> { plugin.report("World UUID save failed for " + record.name(), error); return null; });
        }
        plugin.getLogger().info("Rainforest world loaded: " + record.name() + "; UUID " + world.getUID());
        return world;
    }
    private void chooseSpawn(World world, WorldRecord record) {
        var spawn = SpawnPlanner.find(record.seed(), record.settings(), world.getMinHeight(), world.getMaxHeight());
        world.setSpawnLocation(spawn.x(), world.getHighestBlockYAt(spawn.x(), spawn.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1, spawn.z());
    }
    public void create(String name, long seed, Consumer<String> reply) {
        if (!enabled) { reply.accept("World generation is disabled in config.yml."); return; }
        WorldRecord record;
        try { record = new WorldRecord(name, null, seed, plugin.configuration().worldgen()); }
        catch (IllegalArgumentException invalid) { reply.accept(invalid.getMessage()); return; }
        if (records.containsKey(name) || creating.contains(name) || records.size() + creating.size() >= 16) {
            reply.accept("World already registered, being created, or the 16-world limit has been reached."); return;
        }
        if (plugin.getServer().getWorld(name) != null
                || Files.exists(plugin.getServer().getWorldContainer().toPath().resolve(name))
                || Files.exists(WorldPaths.dimension(plugin.getServer().getLevelDirectory(), name))) {
            reply.accept("That world already exists. Only new world directories may be created."); return;
        }
        creating.add(name);
        reply.accept("Registering new rainforest world " + name + ". Initial generation can take a moment.");
        storage.submit(() -> { storage.reserveWorld(record); return null; }).whenComplete((ignored, failure) -> plugin.onMain(() -> {
            creating.remove(name);
            if (failure != null) { plugin.report("World registration failed", failure); reply.accept("Registration failed; see server log."); return; }
            records.put(name, record);
            // Once reserved, always finish assigning the generator, even if creation was disabled meanwhile.
            try { World world = load(record); reply.accept("Rainforest world ready: " + name + "; UUID " + world.getUID()); }
            catch (Exception error) { plugin.report("Rainforest creation failed for " + name, error); reply.accept("Creation failed; profile retained for restart retry. See server log."); }
        }));
    }
    public Collection<WorldRecord> records() { return List.copyOf(records.values()); }
    public Optional<Region> region(Location location) {
        World world = location.getWorld();
        if (world == null || !(world.getGenerator() instanceof WorldGenerator generator)) return Optional.empty();
        return Optional.of(new TerrainModel(world.getSeed(), generator.settings().seaLevel(), world.getMinHeight(), world.getMaxHeight())
                .region(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
}
