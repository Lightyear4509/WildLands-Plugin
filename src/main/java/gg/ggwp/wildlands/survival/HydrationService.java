package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.config.HydrationSettings;
import gg.ggwp.wildlands.storage.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;

/** Bukkit state is confined to the server thread; only immutable snapshots reach SQLite. */
public final class HydrationService implements WildlandsModule, Listener {
    private static final class Session {
        HydrationRecord record;
        boolean loading;
        boolean respawnPending;
        long combatUntil;
    }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private BukkitTask sampling, saving;
    private WaterTreatment treatment;
    private WaterCollection collection;
    private boolean gameplayEnabled, hudTracking;
    private CompletableFuture<Void> lastSave = CompletableFuture.completedFuture(null);

    public HydrationService(WildlandsPlugin plugin, StorageService storage) {
        this.plugin = plugin;
        this.storage = storage;
    }
    private HydrationSettings settings() { return plugin.configuration().hydration(); }
    @Override public String id() { return "hydration"; }
    @Override public void enable() {
        gameplayEnabled = true;
        treatment = new WaterTreatment(plugin, this);
        treatment.enable();
        collection = new WaterCollection(plugin, plugin.cauldronWater());
        plugin.getServer().getPluginManager().registerEvents(collection, plugin);
        startTracking();
    }
    /** HUD preferences share the existing record without enabling hydration mechanics. */
    public void trackHud(boolean enabled) {
        hudTracking = enabled;
        if (enabled) startTracking();
        else if (!gameplayEnabled) stopTracking();
    }
    private void startTracking() {
        if (sampling != null) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) join(player);
        sampling = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 100, 100);
        long interval = plugin.configuration().settings().saveIntervalSeconds() * 20L;
        saving = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (lastSave.isDone()) save(snapshot());
        }, interval, interval);
    }
    @Override public void disable() {
        gameplayEnabled = false;
        if (collection != null) { HandlerList.unregisterAll(collection); collection = null; }
        if (treatment != null) { treatment.disable(); treatment = null; }
        if (!hudTracking) stopTracking();
    }
    private void stopTracking() {
        if (sampling != null) { sampling.cancel(); sampling = null; }
        if (saving != null) { saving.cancel(); saving = null; }
        HandlerList.unregisterAll(this);
        save(snapshot());
        sessions.clear();
    }
    private void join(Player player) {
        Session session = new Session();
        sessions.put(player.getUniqueId(), session);
        load(player.getUniqueId(), session);
    }
    private void load(UUID id, Session session) {
        session.loading = true;
        boolean defaultHud = settings().defaultHud();
        storage.submit(() -> storage.findHydration(id).orElseGet(() -> new HydrationRecord(id, 100, defaultHud, 0)))
                .whenComplete((record, error) -> {
                    if (error != null) plugin.report("Hydration load failed for " + id + "; will retry", error);
                    plugin.onMain(() -> {
                        if (sessions.get(id) != session) return;
                        session.loading = false;
                        if (error == null) {
                            session.record = session.respawnPending
                                    ? new HydrationRecord(id, settings().respawnHydration(), record.hudEnabled(), 0) : record;
                            session.respawnPending = false;
                        }
                    });
                });
    }
    private void tick() {
        for (var entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.record == null) {
                if (!session.loading) load(entry.getKey(), session);
                continue;
            }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (!gameplayEnabled || player == null || player.isDead() || !survival(player)) continue;
            // Game time, not wall time: server stalls and offline time never become catch-up damage.
            var step = HydrationRules.advance(session.record, settings(), 5, player.isSprinting(),
                    player.isSwimming(), System.nanoTime() < session.combatUntil, plugin.environment() == null ? 1
                            : plugin.environment().hydrationMultiplier(entry.getKey()));
            session.record = step.state();
            if (step.damageDue() && settings().damage() > 0) player.damage(settings().damage());
        }
    }
    private static boolean survival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { join(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        Session removed = sessions.remove(event.getPlayer().getUniqueId());
        if (removed != null && removed.record != null) save(List.of(removed.record));
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        if (!gameplayEnabled) return;
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        session.combatUntil = 0;
        if (session.record == null) session.respawnPending = true;
        else session.record = new HydrationRecord(session.record.uuid(), settings().respawnHydration(), session.record.hudEnabled(), 0);
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRecovery(EntityRegainHealthEvent event) {
        if (!gameplayEnabled) return;
        if (!(event.getEntity() instanceof Player player) || !survival(player)
                || event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) return;
        record(player.getUniqueId()).filter(record -> record.hydration() < settings().recoveryBelow())
                .ifPresent(record -> event.setAmount(event.getAmount() * settings().recoveryMultiplier()));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!gameplayEnabled) return;
        if (event.getFinalDamage() <= 0) return;
        if (event.getEntity() instanceof Player player) combat(player);
        Entity source = event.getDamager();
        if (source instanceof Player player) combat(player);
        else if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player player) combat(player);
    }
    private void combat(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session != null) session.combatUntil = System.nanoTime() + settings().combatMemorySeconds() * 1_000_000_000L;
    }
    public boolean drink(Player player, WaterQuality quality) {
        if (!gameplayEnabled) return false;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.record == null) return false;
        session.record = HydrationRules.drink(session.record, quality, settings());
        var drink = settings().water().get(quality);
        if (survival(player) && HydrationRules.illness(quality, settings(), ThreadLocalRandom.current().nextDouble())
                && drink.illnessSeconds() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, drink.illnessSeconds() * 20, 0));
        }
        return true;
    }
    public boolean set(UUID id, double value) {
        if (!gameplayEnabled) return false;
        Session session = sessions.get(id);
        if (session == null || session.record == null) return false;
        session.record = new HydrationRecord(id, value, session.record.hudEnabled(), 0);
        save(List.of(session.record));
        return true;
    }
    public boolean hud(UUID id, boolean enabled) {
        Session session = sessions.get(id);
        if (session == null || session.record == null) return false;
        session.record = session.record.withHud(enabled);
        save(List.of(session.record));
        return true;
    }
    public Optional<HydrationRecord> record(UUID id) {
        Session session = sessions.get(id);
        return session == null ? Optional.empty() : Optional.ofNullable(session.record);
    }
    public String state(UUID id) {
        if (!gameplayEnabled) return "DISABLED";
        Session session = sessions.get(id);
        return session == null ? "DISABLED_OR_OFFLINE" : session.record != null ? "LOADED" : session.loading ? "LOADING" : "LOAD_FAILED";
    }
    private List<HydrationRecord> snapshot() {
        return sessions.values().stream().map(session -> session.record).filter(Objects::nonNull).toList();
    }
    private void save(List<HydrationRecord> records) {
        lastSave = storage.submit(() -> { storage.saveHydration(records); return null; });
        lastSave.exceptionally(error -> { plugin.report("Hydration save failed; retained for retry", error); return null; });
    }
}
