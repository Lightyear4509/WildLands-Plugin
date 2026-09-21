package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.*;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaterItemsTest {
    private ItemStack bottle(String quality, PotionType type) {
        var item = mock(ItemStack.class);
        var meta = mock(PotionMeta.class);
        var data = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.POTION);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getBasePotionType()).thenReturn(type);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenReturn(quality);
        return item;
    }
    @Test void unknownDataIsConservativeAndEffectPotionsAreNotWater() {
        var items = new WaterItems();
        assertEquals(Optional.of(WaterQuality.QUESTIONABLE), items.quality(bottle(null, PotionType.WATER)));
        assertEquals(Optional.of(WaterQuality.QUESTIONABLE), items.quality(bottle("FUTURE", PotionType.WATER)));
        assertEquals(Optional.of(WaterQuality.SALT), items.quality(bottle("SALT", PotionType.WATER)));
        assertTrue(items.quality(bottle("CLEAN", PotionType.HEALING)).isEmpty());
        var custom = bottle("CLEAN", PotionType.WATER);
        when(((PotionMeta) custom.getItemMeta()).hasCustomEffects()).thenReturn(true);
        assertTrue(items.quality(custom).isEmpty());
    }
    @Test void loadingPlayerCannotConsumeWaterAndLoseBottleWithoutHydration() {
        var plugin = mock(WildlandsPlugin.class);
        var hydration = mock(HydrationService.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(hydration.record(id)).thenReturn(Optional.empty());
        var event = mock(PlayerItemConsumeEvent.class);
        when(event.getPlayer()).thenReturn(player);
        var water = bottle("CLEAN", PotionType.WATER);
        when(event.getItem()).thenReturn(water);
        new WaterTreatment(plugin, hydration).onConsume(event);
        verify(event).setCancelled(true);
        verify(hydration, never()).drink(any(), any());
    }
}
