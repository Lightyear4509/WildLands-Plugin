package gg.ggwp.wildlands.wildlife.presentation;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Phase;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Optional Jaguar prototype. Native mobs remain the only AI, damage and persistence authority. */
public final class WildlifePresentation implements Listener {
    public static final UUID PACK_ID = UUID.fromString("e8a78004-5d0b-4c64-bd7a-4f173b691301");
    private static final NamespacedKey TEMP = new NamespacedKey("ggwpwildlands", "wildlife_visual");
    private static final String[] BONES = {"body", "head", "front_left", "front_right", "rear_left", "rear_right", "tail"};
    private static final float[][] OFFSETS = {{0,.55f,0},{0,.65f,-.65f},{.22f,.2f,-.4f},{-.22f,.2f,-.4f},{.22f,.2f,.4f},{-.22f,.2f,.4f},{0,.55f,.875f}};
    private static final class View {
        final Ocelot animal;
        Phase phase = Phase.IDLE;
        Location previous;
        final List<ItemDisplay> bones = new ArrayList<>();
        Interaction hitbox;
        final Set<UUID> hiddenFrom = new HashSet<>();
        long lastSound = Long.MIN_VALUE / 2;
        View(Ocelot animal) { this.animal = animal; previous = animal.getLocation(); }
    }
    private final WildlandsPlugin plugin;
    private final java.util.function.ToDoubleFunction<Player> attackReach;
    private final Map<UUID, View> animals = new LinkedHashMap<>();
    private final Set<UUID> javaReady = new HashSet<>(), javaRequested = new HashSet<>(), bedrockReady = new HashSet<>();
    private final Map<UUID, UUID> proxies = new HashMap<>();
    private BukkitTask task;
    private long ticks;
    private long soundWindow;
    private int soundsInWindow;
    public WildlifePresentation(WildlandsPlugin plugin) { this(plugin, WildlifePresentation::nativeReach); }
    WildlifePresentation(WildlandsPlugin plugin, java.util.function.ToDoubleFunction<Player> attackReach) { this.plugin = plugin; this.attackReach = attackReach; }
    private static double nativeReach(Player player) {
        var attribute = player.getAttribute(org.bukkit.attribute.Attribute.ENTITY_INTERACTION_RANGE);
        return attribute == null ? 3 : attribute.getValue();
    }
    public void initialize() { plugin.getServer().getPluginManager().registerEvents(this, plugin); bridge(); }
    public void enable() {
        if (task != null) return;
        // Remove legacy temporary entities after a crash; they never contain gameplay inventory/state.
        for (World world : plugin.getServer().getWorlds()) world.getEntities().stream().filter(WildlifePresentation::temporary).forEach(Entity::remove);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sample, 4, 4);
    }
    public void disable() {
        if (task != null) task.cancel(); task = null;
        for (UUID id : Set.copyOf(bedrockReady)) {
            PresentationRegistry.viewer(id, false);
            Player player = plugin.getServer().getPlayer(id); if (player != null) refresh(player);
        }
        animals.values().forEach(this::clearDisplays); animals.clear(); proxies.clear();
        javaReady.clear(); javaRequested.clear(); bedrockReady.clear(); PresentationRegistry.clear();
    }
    public void attach(Ocelot animal) {
        animals.putIfAbsent(animal.getUniqueId(), new View(animal));
        if (plugin.configuration().visuals().enabled()) PresentationRegistry.publish(animal.getUniqueId(), 0);
    }
    public void configurationChanged() {
        if (!plugin.configuration().visuals().enabled()) {
            for (UUID id : Set.copyOf(bedrockReady)) {
                PresentationRegistry.viewer(id, false);
                Player player = plugin.getServer().getPlayer(id); if (player != null) refresh(player);
            }
            javaReady.clear(); javaRequested.clear(); bedrockReady.clear(); PresentationRegistry.clear(); animals.values().forEach(this::clearDisplays);
        } else animals.forEach((id, view) -> PresentationRegistry.publish(id, view.phase.ordinal()));
    }
    public void detach(UUID id) {
        View view = animals.remove(id); if (view != null) clearDisplays(view);
        PresentationRegistry.remove(id);
    }
    public void phase(UUID id, Phase phase) {
        View view = animals.get(id); if (view == null) return;
        view.phase = phase;
        if (plugin.configuration().visuals().enabled()) PresentationRegistry.publish(id, phase.ordinal());
        else PresentationRegistry.remove(id);
        updateBedrock(id, phase.ordinal());
    }
    public String choose(Player player, boolean on) {
        if (!on) {
            javaReady.remove(player.getUniqueId()); javaRequested.remove(player.getUniqueId());
            PresentationRegistry.viewer(player.getUniqueId(), false);
            refresh(player); bedrockReady.remove(player.getUniqueId()); return "Named vanilla wildlife restored.";
        }
        if (!plugin.configuration().visuals().enabled() || task == null) return "Wildlife visuals are disabled; named vanilla animals remain available.";
        if (plugin.crossplay().platform(player.getUniqueId()) == gg.ggwp.wildlands.crossplay.PlatformAdapter.Platform.BEDROCK) {
            Object bridge = bridge();
            if (bridge == null) return "Bedrock model bridge unavailable; using named vanilla wildlife.";
            try {
                if (!(boolean) bridge.getClass().getMethod("hasPack", UUID.class).invoke(bridge, player.getUniqueId()))
                    return "Wildlands Bedrock pack is missing, declined or not yet loaded; named vanilla wildlife remains visible.";
            } catch (ReflectiveOperationException | LinkageError unavailable) { return "Bedrock pack detection unavailable; using named vanilla wildlife."; }
            bedrockReady.add(player.getUniqueId()); PresentationRegistry.viewer(player.getUniqueId(), true);
            refresh(player); return "Bedrock Jaguar models enabled. Use /wildlands visuals off for vanilla presentation.";
        }
        var settings = plugin.configuration().visuals();
        if (settings.javaUrl().isEmpty()) return "Administrator must configure the Java pack HTTPS URL and SHA-1 first.";
        javaRequested.add(player.getUniqueId());
        player.addResourcePack(PACK_ID, settings.javaUrl(), HexFormat.of().parseHex(settings.javaSha1()), "Optional Wildlands Jaguar models", false);
        return "Optional Jaguar pack offered. Models activate only after this pack loads successfully.";
    }
    private Object bridge() {
        var geyser = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled()) return null;
        try {
            var api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, geyser.getClass().getClassLoader());
            Object manager = api.getMethod("extensionManager").invoke(api.getMethod("api").invoke(null));
            Object extension = manager.getClass().getMethod("extension", String.class).invoke(manager, "wildlandsmodels");
            if (extension == null || !(boolean) extension.getClass().getMethod("isEnabled").invoke(extension)) return null;
            extension.getClass().getMethod("bind", Class.class).invoke(extension, PresentationRegistry.class);
            return extension;
        } catch (ReflectiveOperationException | LinkageError absent) { return null; }
    }
    private void updateBedrock(UUID id, int phase) {
        if (bedrockReady.isEmpty()) return;
        Object bridge = bridge(); if (bridge == null) { restoreBedrock(); return; }
        try { bridge.getClass().getMethod("update", UUID.class, int.class).invoke(bridge, id, phase); }
        catch (ReflectiveOperationException | LinkageError failure) {
            // Fail closed to the visible native equivalent; no provider failure can stop gameplay.
            restoreBedrock(); plugin.getLogger().warning("Wildlife Bedrock presentation failed; restored vanilla equivalents.");
        }
    }
    private void restoreBedrock() {
        for (UUID viewer : Set.copyOf(bedrockReady)) {
            PresentationRegistry.viewer(viewer, false);
            Player player = plugin.getServer().getPlayer(viewer); if (player != null) refresh(player);
        }
        bedrockReady.clear();
    }
    private void refresh(Player player) {
        for (View view : animals.values()) {
            if (view.hiddenFrom.remove(player.getUniqueId())) player.showEntity(plugin, view.animal);
            for (ItemDisplay bone : view.bones) player.hideEntity(plugin, bone);
            if (view.hitbox != null) player.hideEntity(plugin, view.hitbox);
            if (bedrockReady.contains(player.getUniqueId()) && view.animal.getWorld().equals(player.getWorld())) {
                player.hideEntity(plugin, view.animal); player.showEntity(plugin, view.animal);
            }
        }
    }
    private void sample() {
        ticks += 4;
        int modeled = 0;
        for (View view : new ArrayList<>(animals.values())) {
            if (!view.animal.isValid() || view.animal.isDead()) { detach(view.animal.getUniqueId()); continue; }
            var settings = plugin.configuration().visuals();
            Location here = view.animal.getLocation();
            here.setPitch(0);
            if (Math.floorMod(ticks + view.animal.getUniqueId().hashCode(), 600) < 4) sound(view.animal.getUniqueId(), "idle", Sound.ENTITY_CAT_AMBIENT);
            List<Player> viewers = settings.enabled() ? view.animal.getWorld().getPlayers().stream()
                    .filter(player -> javaReady.contains(player.getUniqueId()) && here.distanceSquared(player.getLocation()) < 64 * 64).toList() : List.of();
            if (viewers.isEmpty() || modeled++ >= settings.maxAnimals()) { clearDisplays(view); continue; }
            try {
                if (view.bones.isEmpty()) createDisplays(view);
                Set<UUID> visible = new HashSet<>(); viewers.forEach(player -> visible.add(player.getUniqueId()));
                for (UUID id : Set.copyOf(view.hiddenFrom)) if (!visible.contains(id)) {
                    Player player = plugin.getServer().getPlayer(id); if (player != null) {
                        player.showEntity(plugin, view.animal); view.bones.forEach(bone -> player.hideEntity(plugin, bone)); player.hideEntity(plugin, view.hitbox);
                    }
                    view.hiddenFrom.remove(id);
                }
                for (Player player : viewers) if (view.hiddenFrom.add(player.getUniqueId())) {
                    view.bones.forEach(bone -> player.showEntity(plugin, bone)); player.showEntity(plugin, view.hitbox); player.hideEntity(plugin, view.animal);
                }
                boolean moving = here.distanceSquared(view.previous) > .0004;
                for (int index = 0; index < view.bones.size(); index++) {
                    ItemDisplay bone = view.bones.get(index); bone.teleport(here);
                    float[] offset = OFFSETS[index];
                    float angle = index >= 2 && index <= 5 ? JaguarAnimation.leg(view.phase, ticks / 20.0, moving, index - 2)
                            : index == 1 && view.phase == Phase.WARNING ? -.2f : 0;
                    Quaternionf rotation = new Quaternionf().rotateX(angle);
                    if (index == 6) rotation.rotateY((float) Math.sin(ticks / 20.0 * 1.6) * .1f);
                    bone.setTransformation(new Transformation(new Vector3f(offset[0], offset[1] + JaguarAnimation.crouch(view.phase), offset[2]),
                            rotation, new Vector3f(1), new Quaternionf()));
                }
                view.hitbox.teleport(here); view.previous = here;
            } catch (RuntimeException failure) { clearDisplays(view); plugin.report("Jaguar presentation failed; native animal restored", failure); }
        }
    }
    private void createDisplays(View view) {
        try {
            for (String name : BONES) {
                ItemDisplay display = view.animal.getWorld().spawn(view.animal.getLocation(), ItemDisplay.class, entity -> {
                    markTemporary(entity); entity.setVisibleByDefault(false); entity.setInterpolationDuration(4); entity.setTeleportDuration(4);
                    entity.setViewRange(1); entity.setShadowRadius(.4f); entity.setDisplayWidth(2); entity.setDisplayHeight(2);
                    var item = new ItemStack(Material.PAPER); var meta = item.getItemMeta();
                    meta.setItemModel(new NamespacedKey("ggwpwildlands", "jaguar/" + name)); item.setItemMeta(meta);
                    entity.setItemStack(item); entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                }); view.bones.add(display);
            }
            view.hitbox = view.animal.getWorld().spawn(view.animal.getLocation(), Interaction.class, entity -> {
                markTemporary(entity); entity.setVisibleByDefault(false); entity.setInteractionWidth((float) view.animal.getWidth());
                entity.setInteractionHeight((float) view.animal.getHeight()); entity.setResponsive(true);
            });
            proxies.put(view.hitbox.getUniqueId(), view.animal.getUniqueId());
        } catch (RuntimeException failure) { clearDisplays(view); throw failure; }
    }
    private static void markTemporary(Entity entity) { entity.setPersistent(false); entity.setInvulnerable(true); entity.getPersistentDataContainer().set(TEMP, PersistentDataType.BYTE, (byte) 1); }
    private static boolean temporary(Entity entity) { return entity.getPersistentDataContainer().has(TEMP, PersistentDataType.BYTE); }
    private void clearDisplays(View view) {
        for (UUID id : view.hiddenFrom) { Player player = plugin.getServer().getPlayer(id); if (player != null) player.showEntity(plugin, view.animal); }
        view.hiddenFrom.clear(); view.bones.forEach(Entity::remove); view.bones.clear();
        if (view.hitbox != null) { proxies.remove(view.hitbox.getUniqueId()); view.hitbox.remove(); view.hitbox = null; }
    }
    public void sound(UUID animal, String cue, Sound fallback) {
        View view = animals.get(animal); if (view == null || ticks - view.lastSound < 40) return;
        if (ticks - soundWindow >= 20) { soundWindow = ticks; soundsInWindow = 0; }
        if (soundsInWindow >= 4) return;
        soundsInWindow++;
        view.lastSound = ticks;
        for (Player player : view.animal.getWorld().getPlayers()) if (view.animal.getLocation().distanceSquared(player.getLocation()) < 24 * 24) {
            if (javaReady.contains(player.getUniqueId()) || bedrockReady.contains(player.getUniqueId()))
                player.playSound(view.animal.getLocation(), "ggwpwildlands:jaguar." + cue, SoundCategory.NEUTRAL, .7f, 1);
            else player.playSound(view.animal.getLocation(), fallback, SoundCategory.NEUTRAL, .7f, .7f);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void attack(PrePlayerAttackEntityEvent event) {
        UUID animal = proxies.get(event.getAttacked().getUniqueId()); if (animal == null) return;
        if (event.isCancelled() && event.willAttack()) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        View view = animals.get(animal);
        if (view != null && view.hiddenFrom.contains(player.getUniqueId()) && view.animal.isValid()
                && !view.animal.isDead() && player.getWorld().equals(view.animal.getWorld())
                && withinReach(player, view.animal)
                && player.hasLineOfSight(view.animal)) player.attack(view.animal);
    }
    private boolean withinReach(Player player, Entity entity) {
        var box = entity.getBoundingBox(); var eye = player.getEyeLocation();
        double x = Math.max(Math.max(box.getMinX() - eye.getX(), 0), eye.getX() - box.getMaxX());
        double y = Math.max(Math.max(box.getMinY() - eye.getY(), 0), eye.getY() - box.getMaxY());
        double z = Math.max(Math.max(box.getMinZ() - eye.getZ(), 0), eye.getZ() - box.getMaxZ());
        double reach = attackReach.applyAsDouble(player);
        return reach >= 0 && x * x + y * y + z * z <= reach * reach;
    }
    @EventHandler public void pack(PlayerResourcePackStatusEvent event) {
        if (!PACK_ID.equals(event.getID())) return;
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED && javaRequested.contains(event.getPlayer().getUniqueId()) && plugin.configuration().visuals().enabled()) javaReady.add(event.getPlayer().getUniqueId());
        else if (event.getStatus() != PlayerResourcePackStatusEvent.Status.ACCEPTED && event.getStatus() != PlayerResourcePackStatusEvent.Status.DOWNLOADED) {
            javaReady.remove(event.getPlayer().getUniqueId()); refresh(event.getPlayer());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        javaReady.remove(event.getPlayer().getUniqueId()); javaRequested.remove(event.getPlayer().getUniqueId()); bedrockReady.remove(event.getPlayer().getUniqueId());
        PresentationRegistry.viewer(event.getPlayer().getUniqueId(), false); animals.values().forEach(view -> view.hiddenFrom.remove(event.getPlayer().getUniqueId()));
    }
    @EventHandler public void loaded(EntitiesLoadEvent event) { event.getEntities().stream().filter(WildlifePresentation::temporary).forEach(Entity::remove); }
    public String diagnostics() { return "Jaguar visuals: animals=" + animals.size() + "; displays=" + animals.values().stream().mapToInt(v -> v.bones.size()).sum() + "; Java viewers=" + javaReady.size() + "; Bedrock viewers=" + bedrockReady.size() + "; bridge=" + (bridge() != null); }
}
