package gg.ggwp.wildlands.core;

import gg.ggwp.wildlands.storage.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

// Sessions and Bukkit objects are server-thread-only; workers receive immutable records.
public final class PlayerManager implements WildlandsModule, Listener {
    private static final class Session {
        PlayerRecord record;
        boolean loaded;
        boolean loading;
        Session(PlayerRecord record) { this.record = record; }
    }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final int intervalSeconds;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private BukkitTask task;
    private CompletableFuture<Void> saving = CompletableFuture.completedFuture(null);

    public PlayerManager(WildlandsPlugin plugin, StorageService storage, int intervalSeconds) {
        this.plugin = plugin;
        this.storage = storage;
        this.intervalSeconds = intervalSeconds;
    }
    @Override public String id() { return "player-records"; }
    @Override public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) join(player);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick,
                intervalSeconds * 20L, intervalSeconds * 20L);
    }
    private void tick() {
        if (!saving.isDone()) return;
        save(snapshot());
        // A temporary write/read failure must not strand an online session until reconnect.
        for (var entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (!session.loaded && !session.loading) load(entry.getKey(), session);
        }
    }
    @Override public void disable() {
        if (task != null) { task.cancel(); task = null; }
        HandlerList.unregisterAll(this);
        save(snapshot());
        sessions.clear();
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { join(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) save(List.of(touch(session.record)));
    }
    private void join(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Session session = new Session(new PlayerRecord(uuid, player.getName(), now, now));
        sessions.put(uuid, session);
        load(uuid, session);
    }
    private void load(UUID uuid, Session session) {
        session.loading = true;
        PlayerRecord initial = session.record;
        storage.submit(() -> {
            storage.save(List.of(initial));
            return storage.find(uuid).orElseThrow();
        }).whenComplete((record, error) -> {
            if (error != null) plugin.report("Could not load/save player record " + uuid + "; will retry", error);
            plugin.onMain(() -> {
                if (sessions.get(uuid) != session) return;
                session.loading = false;
                if (error == null) {
                    session.record = record;
                    session.loaded = true;
                }
            });
        });
    }
    private PlayerRecord touch(PlayerRecord record) {
        return new PlayerRecord(record.uuid(), record.lastKnownName(), record.firstSeen(),
                Math.max(record.lastSeen(), System.currentTimeMillis()));
    }
    private List<PlayerRecord> snapshot() {
        return sessions.values().stream().map(session -> touch(session.record)).toList();
    }
    private void save(List<PlayerRecord> records) {
        saving = storage.submit(() -> { storage.save(records); return null; });
        saving.exceptionally(error -> { plugin.report("Player batch save failed; retained for retry", error); return null; });
    }
    public Optional<PlayerRecord> record(UUID uuid) {
        Session session = sessions.get(uuid);
        return session == null ? Optional.empty() : Optional.of(session.record);
    }
    public String state(UUID uuid) {
        Session session = sessions.get(uuid);
        return session == null ? "NOT_TRACKED" : session.loaded ? "LOADED" : session.loading ? "LOADING" : "LOAD_FAILED";
    }
    public int size() { return sessions.size(); }
}
