package gg.ggwp.wildlands.wildlife;

import com.destroystokyo.paper.entity.ai.GoalType;
import gg.ggwp.wildlands.config.WildlifeSettings;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior;
import gg.ggwp.wildlands.wildlife.behaviors.PassiveBehavior;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/** Shared distributed work and population budgets, chunk-persistent entities and species-aware SQLite homes. */
public final class WildlifeManager implements WildlandsModule, Listener {
    private static final NamespacedKey TAG = new NamespacedKey("ggwpwildlands", "jaguar");
    private static final NamespacedKey SPECIES = new NamespacedKey("ggwpwildlands", "species");
    private static final NamespacedKey HOME_X = new NamespacedKey("ggwpwildlands", "jaguar_home_x");
    private static final NamespacedKey HOME_Y = new NamespacedKey("ggwpwildlands", "jaguar_home_y");
    private static final NamespacedKey HOME_Z = new NamespacedKey("ggwpwildlands", "jaguar_home_z");
    private static final class Session {
        final Mob cat;
        final WildlifeRecord record;
        JaguarBehavior.Memory memory = JaguarBehavior.Memory.idle();
        PassiveBehavior.Memory passive = PassiveBehavior.Memory.idle();
        UUID target, provoker;
        long lastSample, lastBite = Long.MIN_VALUE / 2;
        long lastNavigation = Long.MIN_VALUE / 2;
        Location navigationGoal;
        Location retreatGoal;
        Location perchGoal;
        long lastHop = Long.MIN_VALUE / 2;
        boolean startled;
        Session(Mob cat, WildlifeRecord record, long tick) { this.cat = cat; this.record = record; lastSample = tick; }
    }
    private final WildlandsPlugin plugin;
    private final StorageService storage;
    private final Map<UUID, WildlifeRecord> known = new LinkedHashMap<>();
    private final Map<UUID, Session> loaded = new LinkedHashMap<>();
    private final ArrayDeque<UUID> rotation = new ArrayDeque<>();
    private boolean enabled;
    private long ticks;
    private int playerCursor;
    private int speciesCursor;
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
        for (var entry : new ArrayList<>(loaded.entrySet())) if (!managed(entry.getValue().cat.getWorld()) || !active(entry.getValue().record.species()) || loaded.size() > settings().maxLoaded()) {
            stop(entry.getValue());
            loaded.remove(entry.getKey()); rotation.remove(entry.getKey());
            if (plugin.presentation() != null) plugin.presentation().detach(entry.getKey());
        }
        for (Session session : loaded.values()) configure(session.cat, session.record.species());
        for (World world : plugin.getServer().getWorlds()) for (Entity entity : world.getEntities()) {
            try { adopt(entity); }
            catch (Exception failure) { plugin.report("Wildlife reconfiguration could not adopt " + entity.getUniqueId(), failure); }
        }
    }
    @Override public void enable() {
        enabled = true;
        if (plugin.presentation() != null) plugin.presentation().enable();
        // One traversal of already loaded entities at enable; thereafter entity lifecycle events maintain the cache.
        for (World world : plugin.getServer().getWorlds()) for (Entity entity : world.getEntities()) adopt(entity);
        sampleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, settings().sampleTicks(), settings().sampleTicks());
        long spawnTicks = settings().spawnSeconds() * 20L;
        spawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::naturalSpawn, spawnTicks, spawnTicks);
        retryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> save(List.of()), 600, 600);
    }
    @Override public void disable() {
        enabled = false;
        if (plugin.presentation() != null) plugin.presentation().disable();
        for (BukkitTask task : new BukkitTask[]{sampleTask, spawnTask, retryTask}) if (task != null) task.cancel();
        sampleTask = spawnTask = retryTask = null;
        for (Session session : loaded.values()) stop(session);
        loaded.clear(); rotation.clear(); save(List.of());
        // Bookkeeping and attack cancellation remain registered while disabled, like cauldron provenance tracking.
    }
    public static boolean tagged(Entity entity) {
        return species(entity).isPresent();
    }
    public static Optional<Species> species(Entity entity) {
        if (entity instanceof Ocelot && entity.getPersistentDataContainer().has(TAG, PersistentDataType.BYTE)) return Optional.of(Species.JAGUAR);
        if (!(entity instanceof Mob)) return Optional.empty();
        String value = entity.getPersistentDataContainer().get(SPECIES, PersistentDataType.STRING);
        if (value == null) return Optional.empty();
        try { Species species = Species.valueOf(value); return species != Species.JAGUAR && species.nativeClass().isInstance(entity) ? Optional.of(species) : Optional.empty(); }
        catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    private boolean active(Species species) { return species == Species.JAGUAR || plugin.configuration().expansion().animal(species).enabled(); }
    private double health(Species species) { return species == Species.JAGUAR ? settings().health() : plugin.configuration().expansion().animal(species).health(); }
    private boolean managed(World world) { return settings().worlds().contains(world.getName()) && world.getEnvironment() == World.Environment.NORMAL; }
    private void configure(Mob cat, Species species) {
        if (cat instanceof Animals animal) { animal.setAdult(); animal.setBreed(false); animal.setAgeLock(true); }
        if (cat instanceof Pig pig) pig.setSaddle(false);
        if (cat instanceof Tameable animal) { animal.setTamed(false); animal.setOwner(null); }
        if (cat instanceof Sittable animal) animal.setSitting(false);
        // Frog movement uses bounded native-physics hops; its vanilla Brain must not compete with our targets.
        cat.setAware(species != Species.FROG && species != Species.TOUCAN && species != Species.CAIMAN);
        if (cat instanceof Frog frog) frog.setTongueTarget(null);
        cat.setRemoveWhenFarAway(false); cat.setPersistent(true);
        cat.customName(Component.text(species.displayName())); cat.setCustomNameVisible(true);
        var health = cat.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) { health.setBaseValue(health(species)); cat.setHealth(Math.min(cat.getHealth(), health(species))); }
        // Retain look/swim behavior but prevent vanilla avoidance and chicken targeting competing with our decisions.
        plugin.getServer().getMobGoals().removeAllGoals(cat, GoalType.MOVE);
        plugin.getServer().getMobGoals().removeAllGoals(cat, GoalType.TARGET);
        cat.setTarget(null);
        cat.getPathfinder().setCanFloat(true);
    }
    private void adopt(Entity entity) {
        if (!enabled || !tagged(entity) || loaded.containsKey(entity.getUniqueId()) || loaded.size() >= settings().maxLoaded()
                || !managed(entity.getWorld())) return;
        var cat = (Mob) entity;
        Species species = species(cat).orElseThrow();
        if (!active(species)) return;
        if (cat.isDead() || cat.getHealth() <= 0) {
            // Some saved chunks can contain a corpse awaiting native removal. Never recreate its home record.
            removed(cat, true); cat.remove(); return;
        }
        WildlifeRecord record = known.get(cat.getUniqueId());
        if (record == null) {
            var data = cat.getPersistentDataContainer();
            Double x = data.get(HOME_X, PersistentDataType.DOUBLE), y = data.get(HOME_Y, PersistentDataType.DOUBLE), z = data.get(HOME_Z, PersistentDataType.DOUBLE);
            if (x == null || y == null || z == null) { plugin.getLogger().warning("Wildlife has missing home metadata: " + cat.getUniqueId()); return; }
            try { record = new WildlifeRecord(cat.getUniqueId(), cat.getWorld().getUID(), x, y, z, true, species); }
            catch (IllegalArgumentException invalid) { plugin.report("Invalid wildlife home metadata", invalid); return; }
            known.put(record.uuid(), record); save(List.of(record));
        }
        if (!record.worldUuid().equals(cat.getWorld().getUID())) { plugin.getLogger().warning("Wildlife world identity mismatch: " + record.uuid()); return; }
        if (record.species() != species) { plugin.getLogger().warning("Wildlife species identity mismatch: " + record.uuid()); return; }
        configure(cat, species);
        loaded.put(record.uuid(), new Session(cat, record, ticks)); rotation.addLast(record.uuid());
        if (plugin.presentation() != null) plugin.presentation().attach(cat, species);
    }
    private void sample() {
        ticks += settings().sampleTicks();
        // Cheap motion over cached intents. Flight does not run new AI, height-map scans or SQL here.
        for (Session session : loaded.values()) if (session.cat.isValid() && !session.cat.isDead()
                && managed(session.cat.getWorld()) && session.record.worldUuid().equals(session.cat.getWorld().getUID())) {
            if (session.record.species() == Species.TOUCAN) fly(session);
            else if (session.record.species() == Species.CAIMAN) swim(session);
        }
        int budget = Math.min(settings().batchSize(), rotation.size());
        for (int i = 0; i < budget; i++) {
            UUID id = rotation.removeFirst(); Session session = loaded.get(id);
            if (session == null) continue;
            if (!session.cat.isValid() || session.cat.isDead()) { loaded.remove(id); if (plugin.presentation() != null) plugin.presentation().detach(id); continue; }
            if (!managed(session.cat.getWorld()) || !session.record.worldUuid().equals(session.cat.getWorld().getUID())) { stop(session); loaded.remove(id); if (plugin.presentation() != null) plugin.presentation().detach(id); continue; }
            rotation.addLast(id);
            try { if (session.record.species().passive()) decidePassive(session); else decide(session); }
            catch (Exception error) { stop(session); plugin.report("Wildlife decision failed for " + id, error); }
        }
    }
    private static boolean eligible(Player player) {
        return player.isValid() && !player.isDead() && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE);
    }
    private LivingEntity target(Session session) {
        Mob cat = session.cat;
        double range = predator(session).limits().detectionRange(), closest = range * range;
        LivingEntity result = null;
        if (session.provoker != null) {
            Entity attacker = plugin.getServer().getEntity(session.provoker);
            if (attacker instanceof Player player && validTarget(session, player, closest)) return player;
        }
        if (session.target != null) {
            Entity current = plugin.getServer().getEntity(session.target);
            if (current instanceof LivingEntity living && validTarget(session, living, closest)) return living;
        }
        for (Player player : cat.getWorld().getPlayers()) if (eligible(player)) {
            double distance = cat.getLocation().distanceSquared(player.getLocation());
            if (distance <= closest && cat.hasLineOfSight(player)) { closest = distance; result = player; }
        }
        if (result == null && session.record.species() == Species.JAGUAR && settings().huntPrey() && night(cat.getWorld())) {
            for (Entity entity : cat.getNearbyEntities(range, 8, range)) if (entity instanceof Chicken || entity instanceof Rabbit) {
                LivingEntity prey = (LivingEntity) entity;
                double distance = cat.getLocation().distanceSquared(prey.getLocation());
                if (prey.isValid() && !prey.isDead() && distance <= closest && cat.hasLineOfSight(prey)) { closest = distance; result = prey; }
            }
        }
        return result;
    }
    private boolean validTarget(Session session, LivingEntity target, double rangeSquared) {
        Mob cat = session.cat;
        return target.isValid() && !target.isDead() && target.getWorld().equals(cat.getWorld())
                && (!(target instanceof Player player) || eligible(player))
                && (target instanceof Player || session.record.species() == Species.JAGUAR && settings().huntPrey() && (target instanceof Chicken || target instanceof Rabbit))
                && cat.getLocation().distanceSquared(target.getLocation()) <= rangeSquared && cat.hasLineOfSight(target);
    }
    private static boolean night(World world) { long time = world.getTime(); return time >= 13000 && time < 23000; }
    private record PredatorRules(JaguarBehavior.Limits limits, double damage, double biteRange, double biteSeconds, double fireRadius) { }
    private PredatorRules predator(Session session) {
        if (session.record.species() == Species.CAIMAN) {
            var rules = plugin.configuration().expansion().caiman();
            return new PredatorRules(rules.limits(), rules.damage(), rules.biteRange(), rules.biteSeconds(), rules.fireRadius());
        }
        return new PredatorRules(settings().limits(), settings().damage(), settings().biteRange(), settings().biteSeconds(), settings().fireRadius());
    }
    private static boolean standingPlace(Location location) {
        World world = location.getWorld(); int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        return world != null && y > world.getMinHeight() && y + 1 < world.getMaxHeight() && world.getWorldBorder().isInside(location) && world.isChunkLoaded(x >> 4, z >> 4)
                && world.getBlockAt(x, y - 1, z).getType().isSolid() && world.getBlockAt(x, y, z).isPassable()
                && !world.getBlockAt(x, y, z).isLiquid() && world.getBlockAt(x, y + 1, z).isPassable();
    }
    private Location perch(Session session, Location home) {
        World world = home.getWorld(); Location best = null; double score = -1;
        double phase = Math.floorDiv(ticks + Math.floorMod(session.record.uuid().hashCode(), 160), 160) * 2.399;
        // Twelve cached height-map columns, never a canopy/block-volume scan.
        for (int sample = 0; sample < 12; sample++) {
            double angle = phase + sample * 2.399;
            int x = home.getBlockX() + (int) (Math.cos(angle) * (3 + sample % 4)), z = home.getBlockZ() + (int) (Math.sin(angle) * (3 + sample % 4));
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getHighestBlockYAt(x, z) + 1;
            if (Math.abs(y - home.getY()) > 12) continue;
            Location candidate = new Location(world, x + .5, y, z + .5);
            if (!world.getWorldBorder().isInside(candidate) || !standingPlace(candidate)) continue;
            double distance = candidate.distanceSquared(session.cat.getLocation());
            if (distance > score) { score = distance; best = candidate; }
        }
        return best == null && standingPlace(home) ? home : best;
    }
    private void hop(Session session, Location destination) {
        Mob frog = session.cat; Location here = frog.getLocation();
        if (ticks - session.lastHop < 40 || !frog.isOnGround() && !frog.isInWater()) return;
        var direction = destination.toVector().subtract(here.toVector()); direction.setY(0);
        if (direction.lengthSquared() < .1) return;
        direction.normalize(); Location landing = here.clone().add(direction.clone().multiply(1.5));
        boolean wet = waterLanding(landing);
        if (!wet && !standingPlace(landing)) return;
        session.lastHop = ticks;
        frog.setVelocity(direction.multiply(frog.isInWater() ? .16 : .24).setY(frog.isInWater() ? .12 : .32));
        cue(frog, "movement", Sound.ENTITY_FROG_LONG_JUMP);
    }
    private static boolean waterLanding(Location location) {
        World world = location.getWorld(); int y = location.getBlockY();
        if (world == null || y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()
                || !world.getWorldBorder().isInside(location) || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        var feet = location.getBlock();
        return (feet.getType() == Material.WATER || feet.isPassable() && !feet.isLiquid() && feet.getRelative(0,-1,0).getType() == Material.WATER)
                && feet.getRelative(0,1,0).isPassable();
    }
    private static void stop(Session session) {
        session.cat.getPathfinder().stopPathfinding(); session.cat.setTarget(null); session.navigationGoal = null;
        if (session.record.species() == Species.TOUCAN) { session.cat.setGravity(true); session.cat.setVelocity(new org.bukkit.util.Vector()); }
    }
    public static Sound fallback(Species species, String cue) {
        boolean hurt = cue.equals("hurt"), dead = cue.equals("death");
        return switch (species) {
            case JAGUAR, MONKEY -> dead ? Sound.ENTITY_CAT_DEATH : hurt ? Sound.ENTITY_CAT_HURT : cue.equals("warning") ? Sound.ENTITY_CAT_HISS : Sound.ENTITY_CAT_AMBIENT;
            case CAPYBARA, TAPIR -> dead ? Sound.ENTITY_PIG_DEATH : hurt ? Sound.ENTITY_PIG_HURT : Sound.ENTITY_PIG_AMBIENT;
            case TOUCAN -> dead ? Sound.ENTITY_PARROT_DEATH : hurt ? Sound.ENTITY_PARROT_HURT : Sound.ENTITY_PARROT_AMBIENT;
            case FROG -> dead ? Sound.ENTITY_FROG_DEATH : hurt ? Sound.ENTITY_FROG_HURT : cue.equals("movement") ? Sound.ENTITY_FROG_LONG_JUMP : Sound.ENTITY_FROG_AMBIENT;
            case CAIMAN -> dead ? Sound.ENTITY_TURTLE_DEATH : hurt ? Sound.ENTITY_TURTLE_HURT : Sound.ENTITY_TURTLE_SHAMBLE;
        };
    }
    private void decidePassive(Session session) {
        Mob animal = session.cat; Location here = animal.getLocation();
        Location home = new Location(animal.getWorld(), session.record.homeX(), session.record.homeY(), session.record.homeZ());
        var rules = plugin.configuration().expansion().animal(session.record.species());
        Player threat = null; double closest = 12 * 12;
        for (Player player : animal.getWorld().getPlayers()) if (eligible(player)) {
            double distance = here.distanceSquared(player.getLocation());
            if (distance < closest && animal.hasLineOfSight(player)) { closest = distance; threat = player; }
        }
        Location group = null; double groupDistance = 12 * 12;
        if (session.record.species().grouped()) for (Session other : loaded.values()) {
            if (other == session || other.record.species() != session.record.species() || !other.cat.isValid() || !other.cat.getWorld().equals(animal.getWorld())) continue;
            double distance = here.distanceSquared(other.cat.getLocation());
            if (distance > 9 && distance < groupDistance) { groupDistance = distance; group = other.cat.getLocation(); }
        }
        Location water = session.record.species().aquatic() ? water(here, animal.isInWater()) : null;
        var observation = new PassiveBehavior.Observation(threat == null ? Double.POSITIVE_INFINITY : Math.sqrt(closest),
                here.distance(home), session.startled, animal.isInWater(), group != null, water != null,
                night(animal.getWorld()), fire(here, 3));
        var before = session.passive.phase();
        session.passive = PassiveBehavior.step(session.record.species(), session.passive, observation, rules,
                Math.clamp((ticks - session.lastSample) / 20.0, .05, 10)); session.lastSample = ticks;
        session.startled = false;
        animal.setTarget(null);
        if (before != session.passive.phase()) session.retreatGoal = null;
        if (plugin.presentation() != null) {
            var pose = switch (session.passive.phase()) {
                case RESTING, PERCHING -> JaguarBehavior.Phase.IDLE;
                case FORAGING -> JaguarBehavior.Phase.WARNING;
                case GROUPING, SWIMMING, RETURNING, FLITTING, HOPPING -> JaguarBehavior.Phase.STALKING;
                case RETREATING -> JaguarBehavior.Phase.RETREATING;
            };
            plugin.presentation().phase(session.record.uuid(), pose);
        }
        switch (session.passive.phase()) {
            case RESTING -> animal.getPathfinder().stopPathfinding();
            case FLITTING, PERCHING -> {
                if (session.perchGoal == null || !standingPlace(session.perchGoal) || session.passive.phase() == PassiveBehavior.Phase.FLITTING && ticks - session.lastNavigation > 160) {
                    session.perchGoal = perch(session, home);
                }
                if (session.perchGoal != null && here.distanceSquared(session.perchGoal) > .5) navigate(session, session.perchGoal, .8);
                else { animal.getPathfinder().stopPathfinding(); session.navigationGoal = null; animal.setGravity(true); }
            }
            case HOPPING -> {
                double angle = Math.floorDiv(ticks + Math.floorMod(session.record.uuid().hashCode(), 200), 80) * 2.399;
                Location goal = water != null ? water : home.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
                navigate(session, goal, .55);
            }
            case RETURNING -> navigate(session, home, .9);
            case GROUPING -> { if (group != null) navigate(session, group, .75); }
            case SWIMMING -> { if (water != null) navigate(session, water, .8); else animal.getPathfinder().stopPathfinding(); }
            case FORAGING -> {
                double angle = Math.floorDiv(ticks + Math.floorMod(session.record.uuid().hashCode(), 200), 160) * 2.399;
                navigate(session, home.clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6), .6);
            }
            case RETREATING -> {
                if (session.retreatGoal == null) {
                    var direction = threat == null ? here.toVector().subtract(home.toVector()) : here.toVector().subtract(threat.getLocation().toVector());
                    direction.setY(0);
                    if (direction.lengthSquared() < .01) direction = new org.bukkit.util.Vector(1, 0, 0);
                    Location goal = here.clone().add(direction.normalize().multiply(8));
                    var fromHome = goal.toVector().subtract(home.toVector()); fromHome.setY(0);
                    if (fromHome.lengthSquared() > rules.roamRadius() * rules.roamRadius()) {
                        fromHome.normalize().multiply(rules.roamRadius() * .9); goal = home.clone().add(fromHome);
                    }
                    session.retreatGoal = goal;
                    cue(animal, "warning", fallback(session.record.species(), "idle"));
                }
                navigate(session, session.retreatGoal, 1.15);
            }
        }
    }
    private static Location water(Location here) { return water(here, false); }
    private static Location water(Location here, boolean swimming) {
        return water(here, swimming, false);
    }
    private static Location water(Location here, boolean swimming, boolean wideBody) {
        World world = here.getWorld(); Location result = null; double best = swimming ? -1 : Double.POSITIVE_INFINITY;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            int wx = here.getBlockX() + x, wz = here.getBlockZ() + z;
            if (!world.isChunkLoaded(wx >> 4, wz >> 4)) continue;
            for (int y = -1; y <= 0; y++) {
                int wy = here.getBlockY() + y;
                if (wy < world.getMinHeight() || wy + 1 >= world.getMaxHeight()) continue;
                if (world.getBlockAt(wx, wy, wz).getType() != Material.WATER || !world.getBlockAt(wx, wy + 1, wz).isPassable()) continue;
                if (wideBody) {
                    boolean clear = true;
                    for (int[] side : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int sx = wx + side[0], sz = wz + side[1];
                        if (!world.isChunkLoaded(sx >> 4, sz >> 4) || world.getBlockAt(sx,wy,sz).getType() != Material.WATER) { clear = false; break; }
                    }
                    if (!clear) continue;
                }
                double distance = x * x + z * z + y * y;
                if (swimming && distance <= 4) continue;
                if (swimming ? distance > best : distance < best) { best = distance; result = new Location(world, wx + .5, wy + .5, wz + .5); }
            }
        }
        return result;
    }
    private void decide(Session session) {
        Mob cat = session.cat; Location here = cat.getLocation();
        var rules = predator(session);
        Location home = new Location(cat.getWorld(), session.record.homeX(), session.record.homeY(), session.record.homeZ());
        LivingEntity target = target(session);
        UUID nextTarget = target == null ? null : target.getUniqueId();
        if (nextTarget != null && !Objects.equals(session.target, nextTarget) && session.memory.phase() != JaguarBehavior.Phase.RETREATING)
            session.memory = JaguarBehavior.Memory.idle(); // A new player must receive their own warning, even after prey hunting.
        session.target = nextTarget;
        int players = 0;
        double rangeSquared = rules.limits().detectionRange() * rules.limits().detectionRange();
        for (Player player : cat.getWorld().getPlayers()) if (eligible(player) && here.distanceSquared(player.getLocation()) <= rangeSquared) players++;
        var observation = new JaguarBehavior.Observation(target == null ? Double.POSITIVE_INFINITY : here.distance(target.getLocation()),
                here.distance(home), players, fire(here, (int) Math.ceil(rules.fireRadius())), night(cat.getWorld()) || session.record.species() == Species.CAIMAN,
                target != null && target.getUniqueId().equals(session.provoker), target != null,
                target != null && !(target instanceof Player));
        var before = session.memory.phase();
        session.memory = JaguarBehavior.step(session.memory, observation, rules.limits(), Math.clamp((ticks - session.lastSample) / 20.0, .05, 10));
        session.lastSample = ticks;
        if (plugin.presentation() != null) plugin.presentation().phase(session.record.uuid(), session.memory.phase());
        if (session.memory.phase() != JaguarBehavior.Phase.RETREATING) session.retreatGoal = null;
        if (session.memory.phase() == JaguarBehavior.Phase.WARNING && before != JaguarBehavior.Phase.WARNING) {
            cue(cat, "warning", fallback(session.record.species(), "warning"));
            if (target instanceof Player player) player.sendMessage(Component.text("[Wildlands] A " + session.record.species().key() + " warns you away. Back off, gather allies or seek a lit campfire."));
        }
        switch (session.memory.phase()) {
            case IDLE -> {
                cat.setTarget(null);
                Location water = session.record.species() == Species.CAIMAN ? water(here, cat.isInWater(), true) : null;
                if (here.distanceSquared(home) > 16) navigate(session, home, .8);
                else if (water != null) navigate(session, water, .65);
                else stop(session);
            }
            case STALKING -> { cat.setTarget(null); if (target != null) { cat.lookAt(target); navigate(session, target.getLocation(), .9); } }
            case WARNING -> { stop(session); if (target != null) cat.lookAt(target); }
            case ATTACKING -> {
                if (target != null) {
                    cat.lookAt(target); navigate(session, target.getLocation(), 1.25);
                    if (cat.getWorld().getDifficulty() != Difficulty.PEACEFUL && here.distanceSquared(target.getLocation()) <= rules.biteRange() * rules.biteRange()
                            && cat.hasLineOfSight(target) && ticks - session.lastBite >= rules.biteSeconds() * 20) {
                        session.lastBite = ticks; target.damage(rules.damage(), cat);
                        cue(cat, "attack", fallback(session.record.species(), "warning"));
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
        Mob cat = session.cat;
        long age = ticks - session.lastNavigation;
        if (age < 20 || age < 40 && session.navigationGoal != null && session.navigationGoal.distanceSquared(destination) < 16) return;
        Location here = cat.getLocation();
        // Include a one-chunk path-search margin; never ask the navigator to search unloaded terrain.
        for (int x = (Math.min(here.getBlockX(), destination.getBlockX()) >> 4) - 1; x <= (Math.max(here.getBlockX(), destination.getBlockX()) >> 4) + 1; x++)
            for (int z = (Math.min(here.getBlockZ(), destination.getBlockZ()) >> 4) - 1; z <= (Math.max(here.getBlockZ(), destination.getBlockZ()) >> 4) + 1; z++)
                if (!cat.getWorld().isChunkLoaded(x, z)) { cat.getPathfinder().stopPathfinding(); return; }
        session.navigationGoal = destination.clone(); session.lastNavigation = ticks;
        if (session.record.species() == Species.TOUCAN) cat.getPathfinder().stopPathfinding();
        else if (session.record.species() == Species.FROG) hop(session, destination);
        else if (session.record.species() == Species.CAIMAN) cat.getPathfinder().stopPathfinding();
        else cat.getPathfinder().moveTo(destination, speed);
    }
    private void swim(Session session) {
        Mob animal = session.cat; Location goal = session.navigationGoal;
        if (goal == null) return;
        Location here = animal.getLocation(); var direction = goal.toVector().subtract(here.toVector()); direction.setY(0);
        if (direction.lengthSquared() < .16) return;
        direction.normalize(); Location next = here.clone().add(direction.clone().multiply(.6));
        if (!waterLanding(next) && !standingPlace(next)) return;
        animal.setVelocity(direction.multiply(animal.isInWater() ? .16 : .12).setY(animal.isInWater() ? Math.clamp(goal.getY()-here.getY(), -.02, .04) : 0));
        animal.setRotation((float)Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())),0);
    }
    private void fly(Session session) {
        Mob bird = session.cat; Location destination = session.navigationGoal;
        if (destination == null) { bird.setGravity(true); return; }
        Location here = bird.getLocation();
        if (here.distanceSquared(destination) < .16) { bird.setVelocity(new org.bukkit.util.Vector()); bird.setGravity(true); return; }
        double horizontal = Math.hypot(destination.getX()-here.getX(), destination.getZ()-here.getZ());
        Location aim = destination.clone();
        if (horizontal > 1) aim.setY(Math.max(session.record.homeY()+3, destination.getY()+2));
        var direction = aim.toVector().subtract(here.toVector()); if(direction.lengthSquared()<.01) return;
        direction.normalize().multiply(.18); Location next=here.clone().add(direction.clone().multiply(2));
        World world=bird.getWorld();
        if (!world.getWorldBorder().isInside(next) || next.getY()<=world.getMinHeight() || next.getY()+2>=world.getMaxHeight()
                || !world.isChunkLoaded(next.getBlockX()>>4,next.getBlockZ()>>4)) { stop(session); return; }
        if (!next.getBlock().isPassable() || next.getBlock().isLiquid() || !next.clone().add(0,1,0).getBlock().isPassable()) {
            if (here.clone().add(0,2,0).getBlock().isPassable()) direction = new org.bukkit.util.Vector(0,.18,0);
            else direction = new org.bukkit.util.Vector();
        }
        bird.setGravity(false); bird.setVelocity(direction);
        if (horizontal>.2) bird.setRotation((float)Math.toDegrees(Math.atan2(-direction.getX(),direction.getZ())),0);
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
        return spawn(location, Species.JAGUAR);
    }
    public Optional<UUID> spawn(Location location, Species species) {
        World world = location.getWorld();
        if (!enabled || !active(species) || world == null || !managed(world) || loaded.size() >= settings().maxLoaded()
                || !world.getWorldBorder().isInside(location)
                || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                || location.getY() <= world.getMinHeight() || location.getY() >= world.getMaxHeight() - 2) return Optional.empty();
        int speciesCap = species == Species.JAGUAR ? plugin.configuration().expansion().jaguarMaxPerWorld() : plugin.configuration().expansion().animal(species).maxPerWorld();
        int cellCap = species == Species.JAGUAR ? 1 : plugin.configuration().expansion().animal(species).maxPerCell();
        if (!PopulationRules.permits(known.values(), world.getUID(), species, location.getBlockX(), location.getBlockZ(), settings().maxPerWorld(), speciesCap, cellCap)) return Optional.empty();
        if (!world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()).getType().isSolid()
                || !location.getBlock().isPassable() || location.getBlock().isLiquid()
                || !world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ()).isPassable()) return Optional.empty();
        if (fire(location, (int) Math.ceil(settings().fireRadius()))) return Optional.empty();
        Class<? extends Mob> base = species.nativeClass();
        Mob cat = world.spawn(location, base, CreatureSpawnEvent.SpawnReason.CUSTOM, animal -> {
            var data = animal.getPersistentDataContainer();
            if (species == Species.JAGUAR) data.set(TAG, PersistentDataType.BYTE, (byte) 1);
            else data.set(SPECIES, PersistentDataType.STRING, species.name());
            data.set(HOME_X, PersistentDataType.DOUBLE, location.getX()); data.set(HOME_Y, PersistentDataType.DOUBLE, location.getY()); data.set(HOME_Z, PersistentDataType.DOUBLE, location.getZ());
            configure(animal, species); animal.setHealth(health(species));
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
        var candidates = Arrays.stream(Species.values()).filter(this::active).toList();
        Species species = candidates.get(Math.floorMod(speciesCursor++, candidates.size()));
        double chance = species == Species.JAGUAR ? settings().spawnChance() : plugin.configuration().expansion().animal(species).spawnChance();
        for (int attempt = 0; attempt < 4; attempt++) {
            if (random.nextDouble() >= chance) continue;
            double angle = random.nextDouble(Math.PI * 2), distance = random.nextDouble(32, 49);
            int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance), z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            var column = terrain.column(x, z);
            if (!column.submerged() && HabitatManager.accepts(species, column.region())) {
                int surface = species == Species.TOUCAN || species == Species.MONKEY ? world.getHighestBlockYAt(x, z) + 1 : column.groundY() + 1;
                var at = new Location(world, x + .5, surface, z + .5);
                if (species.aquatic() && water(at) == null) continue;
                if (spawn(at, species).isPresent()) {
                    // Small, bounded herd; each member must independently pass loaded-terrain/population checks.
                    if (species.grouped()) for (int member = 1; member <= 2; member++) spawn(at.clone().add(member * 2, 0, 0), species);
                    return;
                }
            }
        }
    }
    private void save(Collection<WildlifeRecord> records) {
        storage.submit(() -> { storage.saveWildlife(records); return null; })
                .exceptionally(failure -> { plugin.report("Wildlife save failed; retained for retry", failure); return null; });
    }
    private void removed(Entity entity, boolean permanent) {
        if (!tagged(entity)) return;
        if (plugin.presentation() != null) plugin.presentation().detach(entity.getUniqueId());
        Session session = loaded.remove(entity.getUniqueId()); if (session != null) stop(session);
        rotation.remove(entity.getUniqueId());
        if (permanent) { WildlifeRecord record = known.remove(entity.getUniqueId()); if (record != null) save(List.of(record.dead())); }
    }
    @EventHandler public void entitiesLoad(EntitiesLoadEvent event) { event.getEntities().forEach(this::adopt); }
    @EventHandler public void entitiesUnload(EntitiesUnloadEvent event) { event.getEntities().forEach(entity -> removed(entity, false)); }
    @EventHandler(priority = EventPriority.MONITOR) public void death(EntityDeathEvent event) {
        if (tagged(event.getEntity())) cue((Mob) event.getEntity(), "death", fallback(species(event.getEntity()).orElseThrow(), "death"));
        removed(event.getEntity(), true);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void hurt(EntityDamageEvent event) {
        if (tagged(event.getEntity()) && event.getFinalDamage() > 0) {
            var session = loaded.get(event.getEntity().getUniqueId());
            if (session != null && session.record.species().passive()) session.startled = true;
            cue((Mob) event.getEntity(), "hurt", fallback(species(event.getEntity()).orElseThrow(), "hurt"));
        }
    }
    private void cue(Mob cat, String cue, Sound fallback) {
        if (plugin.presentation() != null) plugin.presentation().sound(cat.getUniqueId(), cue, fallback);
        else cat.getWorld().playSound(cat.getLocation(), fallback, .7f, .7f);
    }
    @EventHandler(priority = EventPriority.MONITOR) public void remove(EntityRemoveEvent event) { removed(event.getEntity(), event.getCause() != EntityRemoveEvent.Cause.UNLOAD); }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (tagged(event.getDamager())) {
            var session = loaded.get(event.getDamager().getUniqueId());
            if (!enabled || session == null || session.record.species().passive() || session.memory.phase() != JaguarBehavior.Phase.ATTACKING
                    || !event.getEntity().getUniqueId().equals(session.target)) event.setCancelled(true);
        }
        if (enabled && tagged(event.getEntity()) && event.getDamager() instanceof Player player && eligible(player)) {
            Session session = loaded.get(event.getEntity().getUniqueId()); if (session != null) session.provoker = player.getUniqueId();
        }
    }
    @EventHandler(ignoreCancelled = true) public void portal(EntityPortalEvent event) { if (tagged(event.getEntity())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void interact(PlayerInteractEntityEvent event) {
        if (tagged(event.getRightClicked())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void breed(EntityBreedEvent event) { if (tagged(event.getMother()) || tagged(event.getFather())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void transform(EntityTransformEvent event) { if (tagged(event.getEntity())) event.setCancelled(true); }
    public List<String> diagnostics() {
        var result = new ArrayList<String>(); result.add("Wildlife: " + (enabled ? "enabled" : "disabled") + "; registered=" + known.size() + "; loaded=" + loaded.size());
        loaded.values().stream().limit(16).forEach(session -> result.add(session.record.uuid() + " " + session.cat.getWorld().getName()
                + " " + session.record.species() + " " + (session.record.species().passive() ? session.passive.phase() : session.memory.phase()) + " home=" + Math.round(session.record.homeX()) + "," + Math.round(session.record.homeY()) + "," + Math.round(session.record.homeZ())));
        if (loaded.size() > 16) result.add("Showing the first 16 of " + loaded.size() + " loaded animals.");
        return List.copyOf(result);
    }
}
