package gg.ggwp.wildlands.ui;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.storage.*;
import gg.ggwp.wildlands.survival.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

class HudManagerTest {
    @Test void environmentHudWorksWithoutHydrationAndHonorsPreferenceAndShutdown() {
        var plugin = mock(WildlandsPlugin.class);
        var hydration = mock(HydrationService.class);
        var environment = mock(EnvironmentService.class);
        var modules = mock(ModuleManager.class);
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        var task = mock(BukkitTask.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(plugin.environment()).thenReturn(environment);
        when(plugin.modules()).thenReturn(modules);
        when(modules.states()).thenReturn(Map.of("hydration", ModuleManager.State.DISABLED,
                "temperature", ModuleManager.State.ENABLED, "wetness", ModuleManager.State.ENABLED));
        when(environment.record(id)).thenReturn(Optional.of(new EnvironmentRecord(id, 26, 35)));
        when(hydration.record(id)).thenReturn(Optional.of(new HydrationRecord(id, 10, true, 0)));
        var tick = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTaskTimer(eq(plugin), tick.capture(), eq(40L), eq(40L))).thenReturn(task);
        var hud = new HudManager(plugin, hydration);
        hud.enable();
        verify(hydration).trackHud(true);
        tick.getValue().run();
        verify(player).sendActionBar(Component.text("Temp 26°C | Wet 35%", NamedTextColor.AQUA));
        clearInvocations(player);
        when(hydration.record(id)).thenReturn(Optional.of(new HydrationRecord(id, 10, false, 0)));
        tick.getValue().run();
        verify(player, never()).sendActionBar(any(Component.class));
        hud.disable();
        verify(task).cancel();
        verify(hydration).trackHud(false);
    }
}
