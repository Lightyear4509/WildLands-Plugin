package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.survival.HydrationService;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class HydrationCommandTest {
    @Test void deniedAdminNeverTouchesPlayerState() {
        var plugin = mock(WildlandsPlugin.class);
        var sender = mock(CommandSender.class);
        new HydrationCommand(plugin).admin(sender, new String[]{"admin", "hydration", "player", "100"});
        verifyNoInteractions(plugin);
        verify(sender).sendMessage("[Wildlands] Permission denied.");
    }
    @Test void consoleCanSetLoadedOnlinePlayerButNonfiniteValuesAreRejected() {
        var plugin = mock(WildlandsPlugin.class);
        var server = mock(Server.class);
        var hydration = mock(HydrationService.class);
        var sender = mock(CommandSender.class);
        var target = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(plugin.state()).thenReturn(WildlandsPlugin.State.READY);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.hydration()).thenReturn(hydration);
        when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        when(server.getPlayerExact("player")).thenReturn(target);
        when(target.getUniqueId()).thenReturn(id);
        when(target.getName()).thenReturn("player");
        when(hydration.set(id, 42)).thenReturn(true);
        var command = new HydrationCommand(plugin);
        for (String invalid : new String[]{"NaN", "Infinity", "-1", "101", "abc"})
            command.admin(sender, new String[]{"admin", "hydration", "player", invalid});
        verifyNoInteractions(hydration);
        command.admin(sender, new String[]{"admin", "hydration", "player", "42"});
        verify(hydration).set(id, 42);
        verify(sender).sendMessage("[Wildlands] Hydration for player set to 42.0%.");
    }
}
