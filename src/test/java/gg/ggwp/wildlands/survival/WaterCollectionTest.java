package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class WaterCollectionTest {
    @Test void lastBottleChangesCauldronTypeAndReplacesExactlyOneHeldBottle() {
        var plugin = mock(WildlandsPlugin.class);
        var server = mock(org.bukkit.Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
        var player = mock(org.bukkit.entity.Player.class);
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        var bottle = mock(ItemStack.class);
        when(bottle.getType()).thenReturn(Material.GLASS_BOTTLE);
        when(bottle.getAmount()).thenReturn(1);
        when(bottle.clone()).thenReturn(bottle);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(bottle);
        var block = mock(org.bukkit.block.Block.class);
        var state = mock(org.bukkit.block.BlockState.class);
        var level = mock(org.bukkit.block.data.Levelled.class);
        when(block.getType()).thenReturn(Material.WATER_CAULDRON);
        when(block.getState()).thenReturn(state);
        when(block.getBlockData()).thenReturn(level);
        when(state.getBlockData()).thenReturn(level);
        when(level.clone()).thenReturn(level);
        when(level.getLevel()).thenReturn(1);
        when(state.update(true, true)).thenReturn(true);
        var cauldrons = mock(CauldronWater.class);
        when(cauldrons.quality(block)).thenReturn(WaterQuality.CLEAN);
        var items = mock(WaterItems.class);
        var filled = mock(ItemStack.class);
        when(items.bottle(WaterQuality.CLEAN)).thenReturn(filled);
        var event = mock(PlayerInteractEvent.class);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getItem()).thenReturn(bottle);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        new WaterCollection(plugin, cauldrons, items).onInteract(event);
        verify(state).setType(Material.CAULDRON);
        verify(state).update(true, true);
        verify(inventory).setItem(EquipmentSlot.HAND, filled);
        verify(inventory, never()).addItem(any(ItemStack.class));
    }
    @Test void nestedListenerChangingHandDoesNotConsumeCauldronOrOverwriteItem() {
        var plugin = mock(WildlandsPlugin.class);
        var server = mock(org.bukkit.Server.class);
        var manager = mock(org.bukkit.plugin.PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        var cauldrons = mock(CauldronWater.class);
        var player = mock(org.bukkit.entity.Player.class);
        var inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        var bottle = mock(ItemStack.class);
        var snapshot = mock(ItemStack.class);
        var replacement = mock(ItemStack.class);
        when(bottle.getType()).thenReturn(Material.GLASS_BOTTLE);
        when(bottle.getAmount()).thenReturn(1);
        when(bottle.clone()).thenReturn(snapshot);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(bottle);
        var block = mock(org.bukkit.block.Block.class);
        var state = mock(org.bukkit.block.BlockState.class);
        var level = mock(org.bukkit.block.data.Levelled.class);
        when(block.getType()).thenReturn(Material.WATER_CAULDRON);
        when(block.getState()).thenReturn(state);
        when(state.getBlockData()).thenReturn(level);
        when(block.getBlockData()).thenReturn(level);
        when(level.clone()).thenReturn(level);
        when(level.getLevel()).thenReturn(2);
        when(cauldrons.quality(block)).thenReturn(WaterQuality.CLEAN);
        var event = mock(PlayerInteractEvent.class);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getItem()).thenReturn(bottle);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(block);
        doAnswer(call -> {
            when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(replacement);
            return null;
        }).when(manager).callEvent(any(org.bukkit.event.block.CauldronLevelChangeEvent.class));
        new WaterCollection(plugin, cauldrons).onInteract(event);
        verify(state, never()).update(anyBoolean(), anyBoolean());
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any(ItemStack.class));
    }
    @Test void deniedItemUseDoesNotInspectOrChangeWorld() {
        var plugin = mock(WildlandsPlugin.class);
        var cauldrons = mock(CauldronWater.class);
        var event = mock(PlayerInteractEvent.class);
        var bottle = mock(ItemStack.class);
        when(bottle.getType()).thenReturn(Material.GLASS_BOTTLE);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getItem()).thenReturn(bottle);
        when(event.useItemInHand()).thenReturn(Event.Result.DENY);
        new WaterCollection(plugin, cauldrons).onInteract(event);
        verify(event, never()).getPlayer();
        verify(event, never()).setCancelled(anyBoolean());
        verifyNoInteractions(plugin, cauldrons);
    }
    @Test void deniedBlockUsePreservesProtection() {
        var plugin = mock(WildlandsPlugin.class);
        var cauldrons = mock(CauldronWater.class);
        var event = mock(PlayerInteractEvent.class);
        var bottle = mock(ItemStack.class);
        when(bottle.getType()).thenReturn(Material.GLASS_BOTTLE);
        when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(event.getItem()).thenReturn(bottle);
        when(event.useItemInHand()).thenReturn(Event.Result.DEFAULT);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.useInteractedBlock()).thenReturn(Event.Result.DENY);
        new WaterCollection(plugin, cauldrons).onInteract(event);
        verify(event, never()).getPlayer();
        verifyNoInteractions(plugin, cauldrons);
    }
}
