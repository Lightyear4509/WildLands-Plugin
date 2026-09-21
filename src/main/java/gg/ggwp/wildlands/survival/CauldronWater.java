package gg.ggwp.wildlands.survival;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

/** Chunk metadata follows vanilla chunk saves; no scans or forced chunk loads. */
public final class CauldronWater implements Listener {
    private final WaterItems items = new WaterItems();
    private NamespacedKey key(Block block) {
        return new NamespacedKey("ggwpwildlands", "cauldron_" + (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15));
    }
    public WaterQuality quality(Block block) {
        String stored = block.getChunk().getPersistentDataContainer().get(key(block), PersistentDataType.STRING);
        if (stored == null) return WaterQuality.QUESTIONABLE;
        try { return WaterQuality.valueOf(stored); }
        catch (IllegalArgumentException unknown) { return WaterQuality.QUESTIONABLE; }
    }
    private void set(Block block, WaterQuality quality) {
        block.getChunk().getPersistentDataContainer().set(key(block), PersistentDataType.STRING, quality.name());
    }
    private void clear(Block block) {
        block.getChunk().getPersistentDataContainer().remove(key(block));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevel(CauldronLevelChangeEvent event) {
        Block block = event.getBlock();
        if (event.getNewState().getType() != Material.WATER_CAULDRON) {
            clear(block); return;
        }
        WaterQuality before = block.getType() == Material.WATER_CAULDRON ? quality(block) : null;
        WaterQuality incoming = switch (event.getReason()) {
            case NATURAL_FILL -> block.getWorld().hasStorm()
                    && block.getWorld().getHighestBlockYAt(block.getX(), block.getZ()) <= block.getY()
                    ? WaterQuality.CLEAN : WaterQuality.QUESTIONABLE;
            case BOTTLE_EMPTY -> poured(event);
            case BUCKET_EMPTY, UNKNOWN -> WaterQuality.QUESTIONABLE;
            case BANNER_WASH, ARMOR_WASH, SHULKER_WASH, EXTINGUISH -> WaterQuality.CONTAMINATED;
            default -> null;
        };
        if (incoming != null) set(block, before == null ? incoming : before.mix(incoming));
        else if (before == null) set(block, WaterQuality.QUESTIONABLE);
    }
    private WaterQuality poured(CauldronLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return WaterQuality.QUESTIONABLE;
        var main = items.quality(player.getInventory().getItemInMainHand());
        var off = items.quality(player.getInventory().getItemInOffHand());
        // This event has no hand field. Never select a cleaner bottle when either hand is ambiguous.
        if (main.isPresent() && off.isPresent()) return main.get().mix(off.get());
        return main.or(() -> off).orElse(WaterQuality.QUESTIONABLE);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { clear(event.getBlock()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { clear(event.getBlock()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) { event.blockList().forEach(this::clear); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) { event.blockList().forEach(this::clear); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(block -> { clear(block); clear(block.getRelative(event.getDirection())); });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(block -> { clear(block); clear(block.getRelative(event.getDirection())); });
    }
}
