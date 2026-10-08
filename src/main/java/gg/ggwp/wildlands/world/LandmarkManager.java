package gg.ggwp.wildlands.world;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/** Loaded-chunk registration and spatially indexed discovery; no coordinate scans or teleportation. */
public final class LandmarkManager implements WildlandsModule, Listener {
    private record Cell(UUID world, int x, int z) {}
    private record Loaded(ExpeditionRecord record, Map<UUID, Long> discoveries) {}
    private static final class Session {
        ExpeditionRecord record; final Map<UUID, Long> discoveries = new HashMap<>(); boolean loading;
    }
    private static final NamespacedKey NAVIGATION = new NamespacedKey("ggwpwildlands", "navigation_target");
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final Map<UUID, LandmarkRecord> records = new LinkedHashMap<>();
    private final Map<UUID, LandmarkRecord> camps = new HashMap<>();
    private final Map<Cell, List<LandmarkRecord>> cells = new HashMap<>();
    private final Map<UUID, Integer> worldCounts = new HashMap<>();
    private final Map<UUID, LandmarkRecord> dirtyLandmarks = new LinkedHashMap<>();
    private final Map<DiscoveryRecord.Key, DiscoveryRecord> dirtyDiscoveries = new LinkedHashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Deque<UUID> queue = new ArrayDeque<>();
    private BukkitTask sampling, saving;
    private CompletableFuture<Void> lastSave = CompletableFuture.completedFuture(null);
    private boolean enabled;
    public LandmarkManager(WildlandsPlugin plugin, StorageService storage, List<LandmarkRecord> saved) {
        this.plugin = plugin; this.storage = storage; saved.forEach(this::index);
    }
    @Override public String id() { return "landmarks"; }
    private void index(LandmarkRecord record) {
        records.put(record.id(), record);
        if (record.owner() != null) { camps.put(record.owner(), record); return; }
        cells.computeIfAbsent(new Cell(record.worldUuid(), Math.floorDiv(record.x(), 128), Math.floorDiv(record.z(), 128)), ignored -> new ArrayList<>()).add(record);
        worldCounts.merge(record.worldUuid(), 1, Integer::sum);
    }
    @Override public void enable() {
        enabled = true; plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (World world : plugin.getServer().getWorlds()) for (Chunk chunk : world.getLoadedChunks()) register(chunk);
        for (Player player : plugin.getServer().getOnlinePlayers()) join(player);
        long ticks = plugin.configuration().landmarks().sampleSeconds() * 20L;
        sampling = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, ticks, ticks);
        long saveTicks = plugin.configuration().settings().saveIntervalSeconds() * 20L;
        saving = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> { if (lastSave.isDone()) save(snapshot()); }, saveTicks, saveTicks);
    }
    @Override public void disable() {
        enabled = false; HandlerList.unregisterAll(this);
        if (sampling != null) { sampling.cancel(); sampling = null; }
        if (saving != null) { saving.cancel(); saving = null; }
        save(snapshot()); sessions.clear(); queue.clear();
    }
    public void register(Chunk chunk) {
        if (!enabled || !chunk.isLoaded() || !(chunk.getWorld().getGenerator() instanceof WorldGenerator generator)) return;
        World world = chunk.getWorld();
        if (worldCounts.getOrDefault(world.getUID(), 0) >= plugin.configuration().landmarks().maxPerWorld() || records.size() >= 131072) return;
        LandmarkPlanner.inChunk(world.getSeed(), generator.settings(), world.getMinHeight(), world.getMaxHeight(), chunk.getX(), chunk.getZ())
                .map(candidate -> candidate.record(world.getUID())).ifPresent(record -> {
                    if (records.containsKey(record.id())) return;
                    index(record); dirtyLandmarks.put(record.id(), record);
                });
    }
    private void join(Player player) {
        UUID id = player.getUniqueId(); var session = new Session(); sessions.put(id, session); queue.remove(id); queue.add(id); load(id, session);
    }
    private void load(UUID id, Session session) {
        session.loading = true;
        storage.submit(() -> new Loaded(storage.findExpedition(id).orElseGet(() -> ExpeditionRecord.initial(id)), storage.findDiscoveries(id)))
                .whenComplete((loaded, failure) -> plugin.onMain(() -> {
                    if (sessions.get(id) != session) return; session.loading = false;
                    if (failure != null) { plugin.report("Exploration load failed for " + id + "; will retry", failure); return; }
                    session.record = loaded.record(); session.discoveries.putAll(loaded.discoveries());
                }));
    }
    private void sample() {
        int count = Math.min(queue.size(), plugin.configuration().landmarks().batchSize());
        for (int i = 0; i < count; i++) {
            UUID id = queue.remove(); Session session = sessions.get(id); if (session == null) continue;
            queue.add(id);
            if (session.record == null) { if (!session.loading) load(id, session); continue; }
            Player player = plugin.getServer().getPlayer(id); if (player == null || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) continue;
            Location location = player.getLocation(); UUID world = player.getWorld().getUID();
            int cellX = Math.floorDiv(location.getBlockX(), 128), cellZ = Math.floorDiv(location.getBlockZ(), 128);
            var settings = plugin.configuration().landmarks();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                for (LandmarkRecord record : cells.getOrDefault(new Cell(world, cellX + dx, cellZ + dz), List.of())) {
                    if (session.discoveries.containsKey(record.id()) || session.discoveries.size() >= 65536) continue;
                    double x = record.x() + .5 - location.getX(), z = record.z() + .5 - location.getZ();
                    if (x * x + z * z > settings.discoveryRadius() * settings.discoveryRadius() || Math.abs(record.y() - location.getY()) > settings.verticalRange()) continue;
                    long firstSeen = System.currentTimeMillis(); session.discoveries.put(record.id(), firstSeen);
                    var discovery = new DiscoveryRecord(id, record.id(), firstSeen); dirtyDiscoveries.put(discovery.key(), discovery);
                    player.sendMessage("[Wildlands] Discovered " + record.kind().label() + ": " + record.name() + ". Review it with /landmark " + record.name());
                }
            var camp = camps.get(id); ExpeditionRecord before = session.record;
            if (camp != null && camp.worldUuid().equals(world)) {
                double distance = Math.hypot(camp.x() + .5 - location.getX(), camp.z() + .5 - location.getZ());
                session.record = ExplorationRules.advance(before, distance, session.discoveries.size(), settings.departAt(), settings.returnAt());
            } else session.record = ExplorationRules.progress(before, session.discoveries.size());
            if (session.record.completedTrips() > before.completedTrips()) player.sendMessage("[Wildlands] Expedition complete. Welcome back to camp.");
            if (session.record.rank() != before.rank()) player.sendMessage("[Wildlands] Trail knowledge: " + session.record.rank().name().toLowerCase(Locale.ROOT) + ".");
        }
    }
    public Optional<ExpeditionRecord> player(UUID id) { var session = sessions.get(id); return session == null ? Optional.empty() : Optional.ofNullable(session.record); }
    public String state(UUID id) { var session = sessions.get(id); return !enabled ? "DISABLED" : session == null ? "OFFLINE" : session.record != null ? "LOADED" : session.loading ? "LOADING" : "LOAD_FAILED"; }
    public List<LandmarkRecord> journal(UUID id) {
        Session session = sessions.get(id); if (session == null || session.record == null) return List.of();
        var found = session.discoveries.entrySet().stream().sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .map(entry -> records.get(entry.getKey())).filter(Objects::nonNull).filter(record -> record.owner() == null || record.owner().equals(id)).toList();
        var result = new ArrayList<LandmarkRecord>(found); if (camps.containsKey(id)) result.addFirst(camps.get(id)); return List.copyOf(result);
    }
    public Optional<LandmarkRecord> find(UUID player, String name) {
        if (name.equalsIgnoreCase("camp")) return Optional.ofNullable(camps.get(player));
        return journal(player).stream().filter(record -> record.name().equalsIgnoreCase(name) || record.id().toString().equalsIgnoreCase(name)).findFirst();
    }
    public List<LandmarkRecord> records() { return List.copyOf(records.values()); }
    public String diagnostics() { return "Landmarks: " + (enabled ? "enabled" : "disabled") + "; records: " + records.size() + "; sessions: " + sessions.size(); }
    public String camp(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (!enabled || session == null || session.record == null) return "Exploration data is not loaded.";
        var shelter = plugin.shelter().assess(player);
        if (player.isDead() || !shelter.protectedFromRain() || !shelter.dryGround() || !shelter.nearbyCampfire())
            return "Mark camp while standing on dry ground under a roof beside a lit campfire.";
        UUID owner = player.getUniqueId(); UUID id = camps.containsKey(owner) ? camps.get(owner).id()
                : UUID.nameUUIDFromBytes(("wildlands-camp:" + owner).getBytes(StandardCharsets.UTF_8));
        if (!records.containsKey(id) && records.size() >= 131072) return "The landmark registry is full.";
        Location location = player.getLocation(); var record = new LandmarkRecord(id, player.getWorld().getUID(),
                "camp-" + owner.toString().replace("-", "").substring(0, 12), LandmarkKind.CAMP, location.getBlockX(), location.getBlockY(), location.getBlockZ(), owner);
        index(record); dirtyLandmarks.put(id, record);
        var before = session.record;
        session.record = new ExpeditionRecord(owner, id, before.longestDistance(), before.completedTrips(), false, before.rank());
        save(List.of(session.record)); return "Camp waypoint saved. /landmark camp shows the route back; setting a new camp replaces this waypoint.";
    }
    public boolean track(Player player, LandmarkRecord record) {
        Session session = sessions.get(player.getUniqueId());
        if (!enabled || session == null || session.record == null || find(player.getUniqueId(), record.id().toString()).isEmpty()) return false;
        session.record = session.record.tracking(record.id()); save(List.of(session.record));
        var held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.COMPASS && held.getItemMeta() instanceof CompassMeta meta) {
            World world = plugin.getServer().getWorld(record.worldUuid());
            if (world != null) {
                meta.setLodestone(new Location(world, record.x(), record.y(), record.z())); meta.setLodestoneTracked(false);
                meta.getPersistentDataContainer().set(NAVIGATION, PersistentDataType.STRING, record.id().toString());
                held.setItemMeta(meta);
            }
        }
        return true;
    }
    public void stopTracking(Player player) {
        Session session = sessions.get(player.getUniqueId()); if (session == null || session.record == null) return;
        session.record = session.record.tracking(null); save(List.of(session.record));
        var held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.COMPASS && held.getItemMeta() instanceof CompassMeta meta && meta.getPersistentDataContainer().has(NAVIGATION)) {
            meta.clearLodestone(); meta.getPersistentDataContainer().remove(NAVIGATION); held.setItemMeta(meta);
        }
    }
    public String route(Player player, LandmarkRecord target) {
        if (!player.getWorld().getUID().equals(target.worldUuid())) {
            var world = plugin.getServer().getWorld(target.worldUuid()); return "In " + (world == null ? target.worldUuid() : world.getName()) + "; travel to that world first.";
        }
        double dx = target.x() + .5 - player.getLocation().getX(), dz = target.z() + .5 - player.getLocation().getZ();
        return ExplorationRules.bearing(dx, dz) + " " + Math.round(Math.hypot(dx, dz)) + "m";
    }
    public Optional<String> navigation(Player player) {
        if (!enabled) return Optional.empty();
        return player(player.getUniqueId()).map(ExpeditionRecord::target).map(records::get).map(record -> route(player, record));
    }
    private List<ExpeditionRecord> snapshot() { return sessions.values().stream().map(session -> session.record).filter(Objects::nonNull).toList(); }
    private void save(List<ExpeditionRecord> players) {
        var landmarks = List.copyOf(dirtyLandmarks.values()); var discoveries = List.copyOf(dirtyDiscoveries.values());
        dirtyLandmarks.clear(); dirtyDiscoveries.clear();
        lastSave = storage.submit(() -> { storage.saveExploration(landmarks, discoveries, players); return null; });
        lastSave.exceptionally(failure -> { plugin.report("Exploration save failed; retained for retry", failure); return null; });
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { join(event.getPlayer()); }
    @EventHandler public void onChunkLoad(ChunkLoadEvent event) { register(event.getChunk()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId(); Session session = sessions.remove(id); queue.remove(id);
        if (session != null && session.record != null) save(List.of(session.record));
    }
}
