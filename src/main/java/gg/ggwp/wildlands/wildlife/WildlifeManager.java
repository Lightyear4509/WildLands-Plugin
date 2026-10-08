package gg.ggwp.wildlands.wildlife;

import com.destroystokyo.paper.entity.ai.GoalType;
import gg.ggwp.wildlands.config.WildlifeSettings;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior;
import gg.ggwp.wildlands.world.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/** One species, distributed server-thread decisions, chunk-persistent entities and SQLite homes. */
public final class WildlifeManager implements WildlandsModule, Listener {
    private static final NamespacedKey TAG = new NamespacedKey("ggwpwildlands", "jaguar");
    private static final NamespacedKey HOME_X = new NamespacedKey("ggwpwildlands", "jaguar_home_x");
    private static final NamespacedKey HOME_Y = new NamespacedKey("ggwpwildlands", "jaguar_home_y");
    private static final NamespacedKey HOME_Z = new NamespacedKey("ggwpwildlands", "jaguar_home_z");
    private static final class Session {
        final Ocelot cat;
        final WildlifeRecord record;
        JaguarBehavior.Memory memory = JaguarBehavior.Memory.idle();
        UUID target, provoker;
        long lastSample, lastBite = Long.MIN_VALUE / 2;
        long lastNavigation = Long.MIN_VALUE / 2;
        Location navigationGoal;
        Location retreatGoal;
        Session(Ocelot cat, WildlifeRecord record, long tick) { this.cat = cat; this.record = record; lastSample = tick; }
    }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final Map<UUID, WildlifeRecord> known = new LinkedHashMap<>();
    private final Map<UUID, Session> loaded = new LinkedHashMap<>();
    private final ArrayDeque<UUID> rotation = new ArrayDeque<>();
    private boolean enabled;
    private long ticks;
    private int playerCursor;
    private BukkitTask sampleTask, spawnTask, retryTask;
    public WildlifeManager(WildlandsPlugin plugin, StorageService storage, List<WildlifeRecord> records) {
        this.plugin = plugin; this.storage = storage;
        records.forEach(record -> known.put(record.uuid(), record));
    }
    private WildlifeSettings settings() { return plugin.configuration().wildlife(); }
    @Override public String id() { return "wildlife"; }
    public void initialize() { plugin.getServer().getPluginManager().registerEvents(this, plugin); }
    public void configurationChanged() {
        if (!enabled) return;
        for (var entry : new ArrayList<>(loaded.entrySet())) if (!managed(entry.getValue().cat.getWorld()) || loaded.size() > settings().maxLoaded()) {
            entry.getValue().cat.getPathfinder().stopPathfinding(); entry.getValue().cat.setTarget(null);
            loaded.remove(entry.getKey()); rotation.remove(entry.getKey());
        }
        for (World world : plugin.getServer().getWorlds()) for (Entity entity : world.getEntities()) {
            try { adopt(entity); }
            catch (Exception failure) { plugin.report("Wildlife reconfiguration could not adopt " + entity.getUniqueId(), failure); }
        }
    }
    @Override public void enable() {
        enabled = true;
        // One traversal of already loaded entities at enable; thereafter entity lifecycle events maintain the cache.
        for (World world : plugin.getServer().getWorlds()) for (Entity entity : world.getEntities()) adopt(entity);
        sampleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, settings().sampleTicks(), settings().sampleTicks());
        long spawnTicks = settings().spawnSeconds() * 20L;
        spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::naturalSpawn, spawnTicks, spawnTicks);
        retryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> save(List.of()), 600, 600);
    }
    @Override public void disable() {
        enabled = false;
        for (BukkitTask task : new BukkitTask[]{sampleTask, spawnTask, retryTask}) if (task != null) task.cancel();
        sampleTask = spawnTask = retryTask = null;
        for (Session session : loaded.values()) { session.cat.getPathfinder().stopPathfinding(); session.cat.setTarget(null); }
        loaded.clear(); rotation.clear(); save(List.of());
        // Bookkeeping and attack cancellation remain registered while disabled, like cauldron provenance tracking.
    }
    public static boolean tagged(Entity entity) {
        return entity instanceof Ocelot && entity.getPersistentDataContainer().has(TAG, PersistentDataType.BYTE);
    }
    private boolean managed(World world) { return settings().worlds().contains(world.getName()) && world.getEnvironment() == World.Environment.NORMAL; }
    private void configure(Ocelot cat) {
        cat.setAdult(); cat.setRemoveWhenFarAway(false); cat.setPersistent(true);
        cat.customName(Component.text("Jaguar")); cat.setCustomNameVisible(true);
        var health = cat.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) { health.setBaseValue(settings().health()); cat.setHealth(Math.min(cat.getHealth(), settings().health())); }
        // Retain look/swim behavior but prevent vanilla avoidance and chicken targeting competing with our decisions.
        plugin.getServer().getMobGoals().removeAllGoals(cat, GoalType.MOVE);
        plugin.getServer().getMobGoals().removeAllGoals(cat, GoalType.TARGET);
        cat.setTarget(null);
    }
    private void adopt(Entity entity) {
        if (!enabled || !tagged(entity) || loaded.containsKey(entity.getUniqueId()) || loaded.size() >= settings().maxLoaded()
                || !managed(entity.getWorld())) return;
        var cat = (Ocelot) entity;
        WildlifeRecord record = known.get(cat.getUniqueId());
        if (record == null) {
            var data = cat.getPersistentDataContainer();
            Double x = data.get(HOME_X, PersistentDataType.DOUBLE), y = data.get(HOME_Y, PersistentDataType.DOUBLE), z = data.get(HOME_Z, PersistentDataType.DOUBLE);
            if (x == null || y == null || z == null) { plugin.getLogger().warning("Jaguar has missing home metadata: " + cat.getUniqueId()); return; }
            try { record = new WildlifeRecord(cat.getUniqueId(), cat.getWorld().getUID(), x, y, z, true); }
            catch (IllegalArgumentException invalid) { plugin.report("Invalid jaguar home metadata", invalid); return; }
            known.put(record.uuid(), record); save(List.of(record));
        }
        if (!record.worldUuid().equals(cat.getWorld().getUID())) { plugin.getLogger().warning("Jaguar world identity mismatch: " + record.uuid()); return; }
        configure(cat);
        loaded.put(record.uuid(), new Session(cat, record, ticks)); rotation.addLast(record.uuid());
    }
    private void sample() {
        ticks += settings().sampleTicks();
        int budget = Math.min(settings().batchSize(), rotation.size());
        for (int i = 0; i < budget; i++) {
            UUID id = rotation.removeFirst(); Session session = loaded.get(id);
            if (session == null) continue;
            if (!session.cat.isValid() || session.cat.isDead()) { loaded.remove(id); continue; }
            if (!managed(session.cat.getWorld())) { session.cat.getPathfinder().stopPathfinding(); session.cat.setTarget(null); loaded.remove(id); continue; }
            rotation.addLast(id);
            try { decide(session); }
            catch (Exception error) { session.cat.getPathfinder().stopPathfinding(); plugin.report("Jaguar decision failed for " + id, error); }
        }
    }
    private static boolean eligible(Player player) {
        return player.isValid() && !player.isDead() && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }
    private LivingEntity target(Session session) {
        Ocelot cat = session.cat;
        double range = settings().limits().detectionRange(), closest = range * range;
        LivingEntity result = null;
        if (session.provoker != null) {
            Entity attacker = plugin.getServer().getEntity(session.provoker);
            if (attacker instanceof Player player && validTarget(cat, player, closest)) return player;
        }
        if (session.target != null) {
            Entity current = plugin.getServer().getEntity(session.target);
            if (current instanceof LivingEntity living && validTarget(cat, living, closest)) return living;
        }
        for (Player player : cat.getWorld().getPlayers()) if (eligible(player)) {
            double distance = cat.getLocation().distanceSquared(player.getLocation());
            if (distance <= closest && cat.hasLineOfSight(player)) { closest = distance; result = player; }
        }
        if (result == null && settings().huntPrey() && night(cat.getWorld())) {
            for (Entity entity : cat.getNearbyEntities(range, 8, range)) if (entity instanceof Chicken || entity instanceof Rabbit) {
                LivingEntity prey = (LivingEntity) entity;
                double distance = cat.getLocation().distanceSquared(prey.getLocation());
                if (prey.isValid() && !prey.isDead() && distance <= closest && cat.hasLineOfSight(prey)) { closest = distance; result = prey; }
            }
        }
        return result;
    }
    private boolean validTarget(Ocelot cat, LivingEntity target, double rangeSquared) {
        return target.isValid() && !target.isDead() && target.getWorld().equals(cat.getWorld())
                && (!(target instanceof Player player) || eligible(player))
                && (target instanceof Player || settings().huntPrey() && (target instanceof Chicken || target instanceof Rabbit))
                && cat.getLocation().distanceSquared(target.getLocation()) <= rangeSquared && cat.hasLineOfSight(target);
    }
    private static boolean night(World world) { long time = world.getTime(); return time >= 13000 && time < 23000; }
    private void decide(Session session) {
        Ocelot cat = session.cat; Location here = cat.getLocation();
        Location home = new Location(cat.getWorld(), session.record.homeX(), session.record.homeY(), session.record.homeZ());
        LivingEntity target = target(session);
        UUID nextTarget = target == null ? null : target.getUniqueId();
        if (nextTarget != null && !Objects.equals(session.target, nextTarget) && session.memory.phase() != JaguarBehavior.Phase.RETREATING)
            session.memory = JaguarBehavior.Memory.idle(); // A new player must receive their own warning, even after prey hunting.
        session.target = nextTarget;
        int players = 0;
        double rangeSquared = settings().limits().detectionRange() * settings().limits().detectionRange();
        for (Player player : cat.getWorld().getPlayers()) if (eligible(player) && here.distanceSquared(player.getLocation()) <= rangeSquared) players++;
        var observation = new JaguarBehavior.Observation(target == null ? Double.POSITIVE_INFINITY : here.distance(target.getLocation()),
                here.distance(home), players, fire(here, (int) Math.ceil(settings().fireRadius())), night(cat.getWorld()),
                target != null && target.getUniqueId().equals(session.provoker), target != null,
                target != null && !(target instanceof Player));
        var before = session.memory.phase();
        session.memory = JaguarBehavior.step(session.memory, observation, settings().limits(), Math.clamp((ticks - session.lastSample) / 20.0, .05, 10));
        session.lastSample = ticks;
        if (session.memory.phase() != JaguarBehavior.Phase.RETREATING) session.retreatGoal = null;
        if (session.memory.phase() == JaguarBehavior.Phase.WARNING && before != JaguarBehavior.Phase.WARNING) {
            cat.getWorld().playSound(here, Sound.ENTITY_CAT_HISS, 1, .7f);
            if (target instanceof Player player) player.sendMessage(Component.text("[Wildlands] A jaguar warns you away. Back off, gather allies or seek a lit campfire."));
        }
        switch (session.memory.phase()) {
            case IDLE -> { cat.setTarget(null); if (here.distanceSquared(home) > 16) navigate(session, home, .8); else cat.getPathfinder().stopPathfinding(); }
            case STALKING -> { cat.setTarget(null); if (target != null) { cat.lookAt(target); navigate(session, target.getLocation(), .9); } }
            case WARNING -> { cat.setTarget(null); cat.getPathfinder().stopPathfinding(); if (target != null) cat.lookAt(target); }
            case ATTACKING -> {
                if (target != null) {
                    cat.lookAt(target); navigate(session, target.getLocation(), 1.25);
                    if (cat.getWorld().getDifficulty() != Difficulty.PEACEFUL && here.distanceSquared(target.getLocation()) <= settings().biteRange() * settings().biteRange()
                            && cat.hasLineOfSight(target) && ticks - session.lastBite >= settings().biteSeconds() * 20) {
                        session.lastBite = ticks; target.damage(settings().damage(), cat);
                        cat.getWorld().playSound(here, Sound.ENTITY_CAT_HISS, .7f, .6f);
                    }
                }
            }
            case RETREATING -> {
                cat.setTarget(null); session.provoker = null;
                if (session.retreatGoal == null) {
                    session.retreatGoal = home;
                    if (here.distanceSquared(home) < 9 && (target != null || observation.fire())) {
                        var direction = target != null ? here.toVector().subtract(target.getLocation().toVector())
                                : new org.bukkit.util.Vector(Math.cos(session.record.uuid().hashCode()), 0, Math.sin(session.record.uuid().hashCode()));
                        direction.setY(0);
                        if (direction.lengthSquared() > .01) session.retreatGoal = here.clone().add(direction.normalize().multiply(8));
                    }
                }
                if (here.distanceSquared(session.retreatGoal) > 4) navigate(session, session.retreatGoal, 1.2);
                else cat.getPathfinder().stopPathfinding();
            }
        }
    }
    private void navigate(Session session, Location destination, double speed) {
        Ocelot cat = session.cat;
        long age = ticks - session.lastNavigation;
        if (age < 20 || age < 40 && session.navigationGoal != null && session.navigationGoal.distanceSquared(destination) < 16) return;
        Location here = cat.getLocation();
        // Include a one-chunk path-search margin; never ask the navigator to search unloaded terrain.
        for (int x = (Math.min(here.getBlockX(), destination.getBlockX()) >> 4) - 1; x <= (Math.max(here.getBlockX(), destination.getBlockX()) >> 4) + 1; x++)
            for (int z = (Math.min(here.getBlockZ(), destination.getBlockZ()) >> 4) - 1; z <= (Math.max(here.getBlockZ(), destination.getBlockZ()) >> 4) + 1; z++)
                if (!cat.getWorld().isChunkLoaded(x, z)) { cat.getPathfinder().stopPathfinding(); return; }
        session.navigationGoal = destination.clone(); session.lastNavigation = ticks;
        cat.getPathfinder().moveTo(destination, speed);
    }
    private static boolean fire(Location center, int radius) {
        World world = center.getWorld();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (x * x + z * z > radius * radius) continue;
            int wx = center.getBlockX() + x, wz = center.getBlockZ() + z;
            if (!world.isChunkLoaded(wx >> 4, wz >> 4)) continue;
            for (int y = -2; y <= 2; y++) {
                int wy = center.getBlockY() + y;
                if (wy < world.getMinHeight() || wy >= world.getMaxHeight()) continue;
                var block = world.getBlockAt(wx, wy, wz);
                if ((block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE)
                        && block.getBlockData() instanceof Campfire campfire && campfire.isLit()) return true;
            }
        }
        return false;
    }
    public Optional<UUID> spawn(Location location) {
        World world = location.getWorld();
        if (!enabled || world == null || !managed(world) || loaded.size() >= settings().maxLoaded()
                || !world.getWorldBorder().isInside(location)
                || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                || location.getY() <= world.getMinHeight() || location.getY() >= world.getMaxHeight() - 2) return Optional.empty();
        long count = known.values().stream().filter(record -> record.worldUuid().equals(world.getUID())).count();
        long cell = HabitatManager.cell(location.getBlockX(), location.getBlockZ());
        if (count >= settings().maxPerWorld() || known.values().stream().anyMatch(record -> record.worldUuid().equals(world.getUID())
                && HabitatManager.cell((int) Math.floor(record.homeX()), (int) Math.floor(record.homeZ())) == cell)) return Optional.empty();
        if (!world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()).getType().isSolid()
                || !location.getBlock().isPassable() || location.getBlock().isLiquid()
                || !world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ()).isPassable()) return Optional.empty();
        if (fire(location, (int) Math.ceil(settings().fireRadius()))) return Optional.empty();
        Ocelot cat = world.spawn(location, Ocelot.class, CreatureSpawnEvent.SpawnReason.CUSTOM, animal -> {
            var data = animal.getPersistentDataContainer(); data.set(TAG, PersistentDataType.BYTE, (byte) 1);
            data.set(HOME_X, PersistentDataType.DOUBLE, location.getX()); data.set(HOME_Y, PersistentDataType.DOUBLE, location.getY()); data.set(HOME_Z, PersistentDataType.DOUBLE, location.getZ());
            configure(animal); animal.setHealth(settings().health());
        });
        if (!cat.isValid() || cat.isDead()) return Optional.empty(); // Respect other plugins cancelling CreatureSpawnEvent.
        adopt(cat); return Optional.of(cat.getUniqueId());
    }
    private void naturalSpawn() {
        if (!enabled || loaded.size() >= settings().maxLoaded()) return;
        var players = plugin.getServer().getOnlinePlayers().stream().filter(WildlifeManager::eligible).filter(player -> managed(player.getWorld())).toList();
        if (players.isEmpty()) return;
        Player player = players.get(Math.floorMod(playerCursor++, players.size()));
        World world = player.getWorld();
        if (!(world.getGenerator() instanceof WorldGenerator generator)) return; // Initial species uses generated rainforest habitats.
        var terrain = new TerrainModel(world.getSeed(), generator.settings().seaLevel(), world.getMinHeight(), world.getMaxHeight());
        var random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 4; attempt++) {
            if (random.nextDouble() >= settings().spawnChance()) continue;
            double angle = random.nextDouble(Math.PI * 2), distance = random.nextDouble(32, 49);
            int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance), z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            var column = terrain.column(x, z);
            if (!column.submerged() && HabitatManager.jaguar(column.region())
                    && spawn(new Location(world, x + .5, column.groundY() + 1, z + .5)).isPresent()) return;
        }
    }
    private void save(Collection<WildlifeRecord> records) {
        storage.submit(() -> { storage.saveWildlife(records); return null; })
                .exceptionally(failure -> { plugin.report("Wildlife save failed; retained for retry", failure); return null; });
    }
    private void removed(Entity entity, boolean permanent) {
        if (!tagged(entity)) return;
        loaded.remove(entity.getUniqueId()); rotation.remove(entity.getUniqueId());
        if (permanent) { WildlifeRecord record = known.remove(entity.getUniqueId()); if (record != null) save(List.of(record.dead())); }
    }
    @EventHandler public void entitiesLoad(EntitiesLoadEvent event) { event.getEntities().forEach(this::adopt); }
    @EventHandler public void entitiesUnload(EntitiesUnloadEvent event) { event.getEntities().forEach(entity -> removed(entity, false)); }
    @EventHandler(priority = EventPriority.MONITOR) public void death(EntityDeathEvent event) { removed(event.getEntity(), true); }
    @EventHandler(priority = EventPriority.MONITOR) public void remove(EntityRemoveEvent event) { removed(event.getEntity(), event.getCause() != EntityRemoveEvent.Cause.UNLOAD); }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (tagged(event.getDamager())) {
            var session = loaded.get(event.getDamager().getUniqueId());
            if (!enabled || session == null || session.memory.phase() != JaguarBehavior.Phase.ATTACKING
                    || !event.getEntity().getUniqueId().equals(session.target)) event.setCancelled(true);
        }
        if (enabled && tagged(event.getEntity()) && event.getDamager() instanceof Player player && eligible(player)) {
            Session session = loaded.get(event.getEntity().getUniqueId()); if (session != null) session.provoker = player.getUniqueId();
        }
    }
    @EventHandler(ignoreCancelled = true) public void portal(EntityPortalEvent event) { if (tagged(event.getEntity())) event.setCancelled(true); }
    public List<String> diagnostics() {
        var result = new ArrayList<String>(); result.add("Wildlife: " + (enabled ? "enabled" : "disabled") + "; registered=" + known.size() + "; loaded=" + loaded.size());
        loaded.values().stream().limit(16).forEach(session -> result.add(session.record.uuid() + " " + session.cat.getWorld().getName()
                + " " + session.memory.phase() + " home=" + Math.round(session.record.homeX()) + "," + Math.round(session.record.homeY()) + "," + Math.round(session.record.homeZ())));
        return List.copyOf(result);
    }
}
