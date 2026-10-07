package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.SeasonSettings;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import java.util.*;
import java.util.concurrent.*;
import org.bukkit.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.*;
import org.bukkit.scheduler.BukkitTask;

/** Per-world game-time seasons. All world/event access stays on the server thread. */
public final class SeasonManager implements WildlandsModule, Listener {
    private static final EnumSet<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS,
            Material.POTATOES, Material.BEETROOTS, Material.COCOA, Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP);
    private static final class Session {
        SeasonRecord record;
        boolean loading;
        long weatherTicks;
        WeatherManager.Snapshot originalWeather;
    }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final WeatherManager weather = new WeatherManager();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private boolean enabled;
    private BukkitTask clockTask, saveTask;
    private CompletableFuture<Void> lastSave = CompletableFuture.completedFuture(null);

    public SeasonManager(WildlandsPlugin plugin, StorageService storage) { this.plugin = plugin; this.storage = storage; }
    @Override public String id() { return "seasons"; }
    private SeasonSettings settings() { return plugin.configuration().seasons(); }
    private boolean managed(World world) {
        return enabled && world.getEnvironment() == World.Environment.NORMAL && settings().worlds().contains(world.getName());
    }
    @Override public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        synchronizeWorlds();
        long ticks = settings().clockSeconds() * 20L;
        clockTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
        long saveTicks = plugin.configuration().settings().saveIntervalSeconds() * 20L;
        saveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (lastSave.isDone()) save(snapshot());
        }, saveTicks, saveTicks);
    }
    @Override public void disable() {
        enabled = false;
        if (clockTask != null) { clockTask.cancel(); clockTask = null; }
        if (saveTask != null) { saveTask.cancel(); saveTask = null; }
        HandlerList.unregisterAll(this);
        save(snapshot());
        for (UUID id : new ArrayList<>(sessions.keySet())) release(id, true);
    }
    private void synchronizeWorlds() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            World world = plugin.getServer().getWorld(id);
            if (world == null || !managed(world)) release(id, true);
        }
        for (World world : plugin.getServer().getWorlds()) {
            if (managed(world) && !sessions.containsKey(world.getUID())) {
                var session = new Session();
                sessions.put(world.getUID(), session);
                load(world.getUID(), session);
            }
        }
    }
    private void load(UUID id, Session session) {
        session.loading = true;
        storage.submit(() -> storage.findSeason(id).orElse(new SeasonRecord(id,
                new SeasonClock.State(Season.DRY, 0)))).whenComplete((record, failure) -> plugin.onMain(() -> {
            if (sessions.get(id) != session) return;
            session.loading = false;
            if (failure != null) { plugin.report("Season load failed for " + id + "; will retry", failure); return; }
            session.record = new SeasonRecord(id, SeasonClock.advance(record.state(), 0, settings().durations()));
            World world = plugin.getServer().getWorld(id);
            if (world != null && managed(world)) evaluateWeather(world, session);
        }));
    }
    private void tick() {
        synchronizeWorlds();
        long ticks = settings().clockSeconds() * 20L;
        for (var entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.record == null) { if (!session.loading) load(entry.getKey(), session); continue; }
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world == null) continue;
            Season before = session.record.state().season();
            session.record = new SeasonRecord(entry.getKey(), SeasonClock.advance(session.record.state(), ticks, settings().durations()));
            session.weatherTicks += ticks;
            boolean changed = before != session.record.state().season();
            if (changed || session.weatherTicks >= settings().weatherSeconds() * 20L) evaluateWeather(world, session);
            if (changed) save(List.of(session.record));
        }
    }
    private void evaluateWeather(World world, Session session) {
        if (session.originalWeather == null) session.originalWeather = WeatherManager.Snapshot.capture(world);
        weather.evaluate(world, settings().profiles().get(session.record.state().season()), settings().weatherSeconds());
        session.weatherTicks = 0;
    }
    private void release(UUID id, boolean restore) {
        Session session = sessions.remove(id);
        if (session == null) return;
        if (session.record != null) save(List.of(session.record));
        World world = plugin.getServer().getWorld(id);
        if (restore && world != null && session.originalWeather != null) session.originalWeather.restore(world);
    }
    @EventHandler public void onWorldLoad(WorldLoadEvent event) { synchronizeWorlds(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) { release(event.getWorld().getUID(), true); }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        var profile = profile(event.getBlock().getWorld());
        if (profile.isEmpty() || !CROPS.contains(event.getBlock().getType())
                || !(event.getNewState().getBlockData() instanceof Ageable ageable)) return;
        int age = SeasonRules.cropAge(ageable.getAge(), ageable.getMaximumAge(), profile.get().cropMultiplier(),
                ThreadLocalRandom.current().nextDouble());
        if (age < 0) event.setCancelled(true);
        else if (age != ageable.getAge()) {
            ageable.setAge(age); event.getNewState().setBlockData(ageable);
        }
    }
    public Optional<SeasonClock.State> state(World world) {
        if (!managed(world)) return Optional.empty();
        Session session = sessions.get(world.getUID());
        return session == null || session.record == null ? Optional.empty() : Optional.of(session.record.state());
    }
    public Optional<SeasonSettings.Profile> profile(World world) {
        return state(world).map(state -> settings().profiles().get(state.season()));
    }
    public double temperatureDelta(World world) { return profile(world).map(SeasonSettings.Profile::temperatureDelta).orElse(0.0); }
    public boolean set(World world, Season season) {
        if (state(world).isEmpty()) return false;
        Session session = sessions.get(world.getUID());
        session.record = new SeasonRecord(world.getUID(), new SeasonClock.State(season, 0));
        evaluateWeather(world, session); save(List.of(session.record)); return true;
    }
    public String status(World world) {
        if (!managed(world)) return "Seasons: disabled or unmanaged for " + world.getName();
        return state(world).map(state -> "Season: " + state.season() + " | Day "
                + (state.elapsedTicks() / 24000 + 1) + "/" + settings().profiles().get(state.season()).days())
                .orElse("Seasons: loading for " + world.getName());
    }
    private List<SeasonRecord> snapshot() {
        return sessions.values().stream().map(session -> session.record).filter(Objects::nonNull).toList();
    }
    private void save(List<SeasonRecord> records) {
        lastSave = storage.submit(() -> { storage.saveSeasons(records); return null; });
        lastSave.exceptionally(failure -> { plugin.report("Season save failed; retained for retry", failure); return null; });
    }
}
