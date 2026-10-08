package gg.ggwp.wildlands.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.survival.HydrationService;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CustomItemManagerTest {
    @TempDir Path directory;
    WildlandsPlugin plugin; SurvivalItems items; RecipeManager recipes; CustomItemManager manager; BukkitTask task;
    @BeforeEach void setup() throws Exception {
        plugin = mock(WildlandsPlugin.class); items = mock(SurvivalItems.class); recipes = mock(RecipeManager.class);
        var server = mock(Server.class); when(plugin.getServer()).thenReturn(server);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory).load());
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getWorlds()).thenReturn(List.of()); doReturn(List.of()).when(server).getOnlinePlayers();
        var scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        task = mock(BukkitTask.class); when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        manager = new CustomItemManager(plugin, items, recipes);
    }
    @Test void lifecycleRemovesRecipesAndScheduledWork() {
        manager.initialize(); manager.enable(); assertTrue(manager.enabled()); verify(recipes).enable();
        manager.disable(); assertFalse(manager.enabled()); verify(task).cancel(); verify(recipes).disable();
        assertTrue(manager.diagnostics().contains("loaded stations: 0"));
    }
    @Test void waterskinUsesNativeReplacementAndNeverConsumesWhileDisabledOrLoading() {
        var item = mock(ItemStack.class); var empty = mock(ItemStack.class); var player = mock(Player.class);
        UUID id = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(id);
        var event = mock(PlayerItemConsumeEvent.class); when(event.getPlayer()).thenReturn(player); when(event.getItem()).thenReturn(item);
        when(items.kind(item)).thenReturn(Optional.of(SurvivalItems.Kind.WATERSKIN)); when(items.charges(item)).thenReturn(1);
        manager.onConsume(event); verify(event).setCancelled(true); verify(event, never()).setReplacement(any());
        manager.enable(); var hydration = mock(HydrationService.class); when(plugin.hydration()).thenReturn(hydration);
        when(hydration.state(id)).thenReturn("LOADING"); clearInvocations(event);
        manager.onConsume(event); verify(event).setCancelled(true); verify(event, never()).setReplacement(any());
        when(hydration.state(id)).thenReturn("LOADED"); when(items.waterskin(0)).thenReturn(empty); clearInvocations(event);
        manager.onConsume(event); verify(event).setReplacement(empty); verify(event, never()).setCancelled(true);
        verify(hydration, never()).drink(any(), any()); // Shared native water listener applies exactly one drink.
    }
}
