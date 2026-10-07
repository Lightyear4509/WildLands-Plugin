package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.EnvironmentSettings;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitTask;

/** Shared UUID sessions for the independent temperature and wetness modules. */
public final class EnvironmentService implements Listener {
    private static final class Session { EnvironmentRecord record; boolean loading; }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final ShelterService shelter;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private boolean temperatureEnabled, wetnessEnabled;
    private BukkitTask sampling, saving;
    private CompletableFuture<Void> lastSave = CompletableFuture.completedFuture(null);
    public EnvironmentService(WildlandsPlugin plugin, StorageService storage, ShelterService shelter) {
        this.plugin = plugin; this.storage = storage; this.shelter = shelter;
    }
    public WildlandsModule temperatureModule() { return module("temperature", true); }
    public WildlandsModule wetnessModule() { return module("wetness", false); }
    private WildlandsModule module(String id, boolean temperature) {
        return new WildlandsModule() {
            @Override public String id() { return id; }
            @Override public void enable() { setEnabled(temperature, true); }
            @Override public void disable() { setEnabled(temperature, false); }
        };
    }
    private void setEnabled(boolean temperature, boolean enabled) {
        if (temperature) temperatureEnabled = enabled; else wetnessEnabled = enabled;
        if (temperatureEnabled || wetnessEnabled) start(); else stop();
    }
    private void start() {
        if (sampling != null) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) join(player);
        long ticks = plugin.configuration().environment().sampleSeconds() * 20L;
        sampling = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, ticks, ticks);
        long saveTicks = plugin.configuration().settings().saveIntervalSeconds() * 20L;
        saving = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (lastSave.isDone()) save(snapshot());
        }, saveTicks, saveTicks);
    }
    private void stop() {
        if (sampling != null) { sampling.cancel(); sampling = null; }
        if (saving != null) { saving.cancel(); saving = null; }
        HandlerList.unregisterAll(this);
        save(snapshot()); sessions.clear();
    }
    private void join(Player player) {
        Session session = new Session(); sessions.put(player.getUniqueId(), session); load(player.getUniqueId(), session);
    }
    private void load(UUID id, Session session) {
        session.loading = true;
        double defaultTemperature = plugin.configuration().environment().defaultTemperature();
        storage.submit(() -> storage.findEnvironment(id).orElse(new EnvironmentRecord(id,
                defaultTemperature, 0))).whenComplete((record, failure) -> plugin.onMain(() -> {
            if (sessions.get(id) != session) return;
            session.loading = false;
            if (failure == null) session.record = record;
            else plugin.report("Environment load failed for " + id + "; will retry", failure);
        }));
    }
    private void sample() {
        EnvironmentSettings settings = plugin.configuration().environment();
        for (Map.Entry<UUID, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.record == null) { if (!session.loading) load(entry.getKey(), session); continue; }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || player.isDead() || !survival(player)) continue;
            ShelterStatus status = shelter.assess(player);
            EnvironmentRecord next = session.record;
            WaterExposure exposure = WaterExposure.sample(player);
            if (temperatureEnabled) next = next.withTemperature(EnvironmentRules.temperature(settings,
                    biomeTemperature(player) + EnvironmentRules.dailyAdjustment(settings, player.getWorld().getTime(),
                            player.getWorld().getEnvironment() == World.Environment.NORMAL)
                            + EnvironmentRules.activityAdjustment(settings, player.isSprinting()),
                    player.getLocation().getY(), exposure.rain(), exposure.immersed(), status));
            if (wetnessEnabled) next = EnvironmentRules.wetness(next, settings, settings.sampleSeconds(),
                    exposure.rain(), exposure.immersed(), status);
            session.record = next;
        }
    }
    private static boolean survival(Player player) { return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE; }
    private static double biomeTemperature(Player player) {
        // Paper exposes a stable scalar derived from the active biome without scanning terrain.
        return player.getLocation().getBlock().getTemperature() * 20.0 + 10.0;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { join(event.getPlayer()); }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRecovery(EntityRegainHealthEvent event) {
        if (!temperatureEnabled || !wetnessEnabled || !(event.getEntity() instanceof Player player)
                || !survival(player) || event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) return;
        record(player.getUniqueId()).filter(record -> EnvironmentRules.coldWet(record, plugin.configuration().environment()))
                .ifPresent(record -> event.setAmount(event.getAmount() * plugin.configuration().environment().coldWetRecoveryMultiplier()));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null && session.record != null) save(List.of(session.record));
    }
    public Optional<EnvironmentRecord> record(UUID id) {
        Session session = sessions.get(id); return session == null ? Optional.empty() : Optional.ofNullable(session.record);
    }
    public double hydrationMultiplier(UUID id) {
        return temperatureEnabled ? record(id).map(record -> EnvironmentRules.hydrationMultiplier(record, plugin.configuration().environment())).orElse(1.0) : 1.0;
    }
    public String status(Player player) {
        EnvironmentRecord record = record(player.getUniqueId()).orElse(null);
        String temperature = !temperatureEnabled ? "disabled" : record == null ? "loading" : Math.round(record.temperature()) + "°C";
        String wetness = !wetnessEnabled ? "disabled" : record == null ? "loading" : Math.round(record.wetness()) + "%";
        String shelterText = "disabled";
        if (shelter.enabled()) {
            ShelterStatus shelterStatus = shelter.assess(player);
            shelterText = shelterStatus.label() + (shelterStatus.nearbyCampfire() ? "; campfire warmth" : "");
        }
        return "Temperature: " + temperature + "; wetness: " + wetness + "; shelter: " + shelterText;
    }
    private List<EnvironmentRecord> snapshot() { return sessions.values().stream().map(session -> session.record).filter(Objects::nonNull).toList(); }
    private void save(List<EnvironmentRecord> records) {
        lastSave = storage.submit(() -> { storage.saveEnvironment(records); return null; });
        lastSave.exceptionally(failure -> { plugin.report("Environment save failed; retained for retry", failure); return null; });
    }
}
