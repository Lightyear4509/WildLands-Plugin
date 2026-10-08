package gg.ggwp.wildlands.items;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.survival.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/** Server-thread station operations; unloaded chunks and full inventories do no work. */
public final class CustomItemManager implements WildlandsModule, Listener {
    private record Position(UUID world, int x, int y, int z) {}
    private final WildlandsPlugin plugin;
    private final SurvivalItems items;
    private final RecipeManager recipes;
    private final StationProcessor processor;
    private final Set<Position> stations = new LinkedHashSet<>();
    private final Deque<Position> queue = new ArrayDeque<>();
    private BukkitTask task;
    private boolean enabled;
    public CustomItemManager(WildlandsPlugin plugin) { this(plugin, new SurvivalItems(), null); }
    CustomItemManager(WildlandsPlugin plugin, SurvivalItems items, RecipeManager recipes) {
        this.plugin = plugin; this.items = items;
        this.recipes = recipes == null ? new RecipeManager(plugin, items) : recipes;
        processor = new StationProcessor(items, new WaterItems());
    }
    @Override public String id() { return "crafting"; }
    public SurvivalItems items() { return items; }
    public void initialize() {
        // Identity, safe consumption and block drops remain protected even while gameplay is disabled.
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    @Override public void enable() {
        recipes.enable(); enabled = true;
        for (World world : plugin.getServer().getWorlds()) for (Chunk chunk : world.getLoadedChunks()) discover(chunk);
        long ticks = plugin.configuration().crafting().sampleSeconds() * 20L;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
        for (Player player : plugin.getServer().getOnlinePlayers()) player.discoverRecipes(recipes.keys());
    }
    @Override public void disable() {
        enabled = false;
        if (task != null) { task.cancel(); task = null; }
        recipes.disable(); stations.clear(); queue.clear();
    }
    public boolean enabled() { return enabled; }
    public String diagnostics() { return "Crafting: " + (enabled ? "enabled" : "disabled") + "; loaded stations: " + stations.size() + "; recipes: " + recipes.keys().size(); }
    private static Position position(Block block) { return new Position(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()); }
    private Optional<SurvivalItems.Kind> kind(BlockState state) {
        if (!(state instanceof TileState tile)) return Optional.empty();
        String stored = tile.getPersistentDataContainer().get(SurvivalItems.KIND, PersistentDataType.STRING);
        if (stored == null) return Optional.empty();
        try {
            var kind = SurvivalItems.Kind.valueOf(stored);
            return kind.station() && state.getType() == kind.material ? Optional.of(kind) : Optional.empty();
        } catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    private void track(BlockState state) {
        if (!enabled || kind(state).filter(SurvivalItems.Kind::barrel).isEmpty()) return;
        Position position = position(state.getBlock());
        if (stations.size() < plugin.configuration().crafting().maxLoadedStations() && stations.add(position)) queue.add(position);
    }
    private void discover(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities(false)) track(state);
    }
    private void tick() {
        int count = Math.min(queue.size(), plugin.configuration().crafting().batchSize());
        for (int i = 0; i < count; i++) {
            Position position = queue.remove();
            if (!stations.contains(position)) continue;
            World world = plugin.getServer().getWorld(position.world());
            if (world == null || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) { stations.remove(position); continue; }
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            BlockState state = block.getState();
            var kind = kind(state);
            if (kind.isEmpty() || !kind.get().barrel() || !(state instanceof Container container)) { stations.remove(position); continue; }
            queue.add(position);
            boolean roof = world.getHighestBlockYAt(block.getX(), block.getZ(), HeightMap.MOTION_BLOCKING) > block.getY();
            boolean rain = world.getEnvironment() == World.Environment.NORMAL && world.hasStorm() && !roof
                    && block.getTemperature() >= 0.15 && StationRules.rainBiome(block.getBiome().getKey().getKey());
            boolean drying = kind.get() == SurvivalItems.Kind.DRYING_RACK && roof && warm(block);
            // The live native inventory is the authority; hoppers and both clients use the same transaction.
            var inventory = container.getInventory();
            processor.process(kind.get(), inventory.getContents(), rain, drying).ifPresent(inventory::setContents);
        }
    }
    private static boolean warm(Block block) {
        World world = block.getWorld();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (!world.isChunkLoaded((block.getX() + x) >> 4, (block.getZ() + z) >> 4)) continue;
            for (int y = -2; y <= 2; y++) {
                int height = block.getY() + y;
                if (height < world.getMinHeight() || height >= world.getMaxHeight()) continue;
                var data = world.getBlockAt(block.getX() + x, height, block.getZ() + z).getBlockData();
                if (data instanceof org.bukkit.block.data.type.Campfire fire && fire.isLit()) return true;
            }
        }
        return false;
    }
    public double rainMultiplier(Player player) {
        return enabled && items.kind(player.getInventory().getChestplate()).orElse(null) == SurvivalItems.Kind.RAIN_CLOAK
                ? plugin.configuration().crafting().cloakRainMultiplier() : 1;
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        var kind = items.kind(event.getItemInHand()).filter(SurvivalItems.Kind::station);
        if (kind.isEmpty() || !(event.getBlockPlaced().getState() instanceof TileState tile)) return;
        tile.getPersistentDataContainer().set(SurvivalItems.KIND, PersistentDataType.STRING, kind.get().name());
        if (tile instanceof Nameable named) named.customName(net.kyori.adventure.text.Component.text(kind.get().label));
        tile.update(false, false); track(tile);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent event) {
        var kind = kind(event.getBlockState());
        if (kind.isEmpty()) return;
        Position removed = position(event.getBlock());
        stations.remove(removed); queue.removeIf(removed::equals);
        // Replace only the block item; native contents still drop exactly once.
        for (var entity : event.getItems()) if (entity.getItemStack().getType() == kind.get().material
                && entity.getItemStack().getAmount() == 1 && items.kind(entity.getItemStack()).isEmpty()) {
            entity.setItemStack(items.create(kind.get())); break;
        }
    }
    @EventHandler public void onChunkLoad(ChunkLoadEvent event) { if (enabled) discover(event.getChunk()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        UUID world = event.getWorld().getUID(); int x = event.getChunk().getX(), z = event.getChunk().getZ();
        stations.removeIf(position -> position.world().equals(world) && (position.x() >> 4) == x && (position.z() >> 4) == z);
        queue.removeIf(position -> !stations.contains(position));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Container container) track(container);
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { if (enabled) event.getPlayer().discoverRecipes(recipes.keys()); }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEmptySkin(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && items.kind(event.getItem()).orElse(null) == SurvivalItems.Kind.WATERSKIN
                && items.charges(event.getItem()) == 0) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[Wildlands] Refill this waterskin at a crafting table with three clean water bottles.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (items.kind(event.getItem()).orElse(null) != SurvivalItems.Kind.WATERSKIN) return;
        int charges = items.charges(event.getItem());
        if (!enabled || charges <= 0 || !plugin.hydration().state(event.getPlayer().getUniqueId()).equals("LOADED")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[Wildlands] Waterskins require active crafting and loaded hydration data.");
            return;
        }
        event.setReplacement(items.waterskin(charges - 1));
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(FurnaceBurnEvent event) {
        if (enabled && kind(event.getBlock().getState()).orElse(null) == SurvivalItems.Kind.IMPROVED_STOVE)
            event.setBurnTime((int) Math.min(Integer.MAX_VALUE, event.getBurnTime() * plugin.configuration().crafting().stoveFuelMultiplier()));
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (event.getRecipe() == null || !event.getRecipe().getKey().equals(new NamespacedKey("ggwpwildlands", "boiler_water"))) return;
        var quality = new WaterItems().quality(event.getSource());
        if (!enabled || items.kind(event.getSource()).isPresent() || quality.isEmpty() || !quality.get().canBoil()) {
            event.setCancelled(true); return;
        }
        event.setResult(new WaterItems().bottle(WaterQuality.CLEAN));
    }
}
