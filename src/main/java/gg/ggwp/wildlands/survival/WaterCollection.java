package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.HydrationSettings.Spring;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;

public final class WaterCollection implements Listener {
    private final WildlandsPlugin plugin;
    private final CauldronWater cauldrons;
    private final WaterItems items;
    public WaterCollection(WildlandsPlugin plugin, CauldronWater cauldrons) {
        this(plugin, cauldrons, new WaterItems());
    }
    WaterCollection(WildlandsPlugin plugin, CauldronWater cauldrons, WaterItems items) {
        this.items = items;
        this.plugin = plugin;
        this.cauldrons = cauldrons;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getItem() == null || event.getItem().getType() != Material.GLASS_BOTTLE
                || event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        PlayerInventory inventory = player.getInventory();
        ItemStack original = inventory.getItem(event.getHand());
        if (original == null || original.getType() != Material.GLASS_BOTTLE || original.getAmount() < 1) return;
        ItemStack held = original.clone();
        Block source = event.getClickedBlock();
        boolean cauldron = source != null && source.getType() == Material.WATER_CAULDRON;
        if (!cauldron) {
            var hit = player.rayTraceBlocks(5, FluidCollisionMode.SOURCE_ONLY);
            source = hit == null ? null : hit.getHitBlock();
            if (source == null || source.getType() != Material.WATER) return;
        }
        WaterQuality quality;
        if (cauldron) quality = cauldrons.quality(source);
        else {
            var location = new Spring(source.getWorld().getName(), source.getX(), source.getY(), source.getZ());
            quality = WaterSources.classify(source.getBiome().getKey().toString(), plugin.configuration().hydration().springs().contains(location));
        }
        // Deny the vanilla fill only after identifying the precise interaction we handle.
        event.setCancelled(true);
        if (cauldron) {
            var originalData = source.getBlockData().clone();
            BlockState next = source.getState();
            Levelled level = (Levelled) next.getBlockData();
            if (level.getLevel() == 1) next.setType(Material.CAULDRON);
            else { level.setLevel(level.getLevel() - 1); next.setBlockData(level); }
            var change = new CauldronLevelChangeEvent(source, player,
                    CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL, next);
            plugin.getServer().getPluginManager().callEvent(change);
            // Other listeners can change the hand during this nested event.
            ItemStack current = inventory.getItem(event.getHand());
            if (change.isCancelled() || current == null || !current.equals(held)) return;
            if (!source.getBlockData().equals(originalData)) return;
            // The last bottle changes WATER_CAULDRON to CAULDRON; update(false) rejects that type change.
            if (!change.getNewState().update(true, true)) return;
        }
        ItemStack filled = items.bottle(quality);
        if (player.getGameMode() != GameMode.CREATIVE && held.getAmount() == 1) inventory.setItem(event.getHand(), filled);
        else {
            if (player.getGameMode() != GameMode.CREATIVE) { held.setAmount(held.getAmount() - 1); inventory.setItem(event.getHand(), held); }
            inventory.addItem(filled).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.playSound(player.getLocation(), "minecraft:item.bottle.fill", 1, 1);
    }
}
