package gg.ggwp.wildlands.survival;

import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.items.SurvivalItems;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class NutritionServiceTest {
    @TempDir Path directory;
    @Test void bonusRunsAfterNativeFoodAndCapsAtHungerButDisabledModuleNeverAppliesIt() throws Exception {
        var plugin = mock(WildlandsPlugin.class); var server = mock(Server.class); when(plugin.getServer()).thenReturn(server);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory).load());
        var modules = mock(ModuleManager.class); when(plugin.modules()).thenReturn(modules);
        when(modules.states()).thenReturn(Map.of("nutrition", ModuleManager.State.ENABLED));
        var items = mock(SurvivalItems.class); var food = mock(ItemStack.class); when(items.preserved(food)).thenReturn(true);
        var player = mock(Player.class); UUID id = UUID.randomUUID(); when(player.getUniqueId()).thenReturn(id); when(server.getPlayer(id)).thenReturn(player);
        var event = mock(PlayerItemConsumeEvent.class); when(event.getPlayer()).thenReturn(player); when(event.getItem()).thenReturn(food);
        new NutritionService(plugin, items).onConsume(event);
        var callback = ArgumentCaptor.forClass(Runnable.class); verify(plugin).onMain(callback.capture());
        verify(player, never()).setSaturation(anyFloat());
        when(player.getFoodLevel()).thenReturn(20); when(player.getSaturation()).thenReturn(19.5f);
        callback.getValue().run(); verify(player).setSaturation(20f);
        when(modules.states()).thenReturn(Map.of("nutrition", ModuleManager.State.DISABLED)); clearInvocations(player);
        callback.getValue().run(); verify(player, never()).setSaturation(anyFloat());
    }
}
