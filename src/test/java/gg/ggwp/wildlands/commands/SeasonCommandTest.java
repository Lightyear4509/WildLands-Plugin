package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.seasons.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class SeasonCommandTest {
    @Test void permissionDenialNeverTouchesTheService() {
        var plugin = mock(WildlandsPlugin.class);
        var sender = mock(CommandSender.class);
        new SeasonCommand(plugin).admin(sender, new String[]{"admin", "season", "monsoon", "world"});
        verifyNoInteractions(plugin);
        verify(sender).sendMessage("[Wildlands] Permission denied.");
    }
    @Test void consoleCanSetExplicitWorldAndInvalidSeasonDoesNotMutate() {
        var plugin = mock(WildlandsPlugin.class);
        var sender = mock(CommandSender.class);
        var server = mock(Server.class);
        var world = mock(World.class);
        var manager = mock(SeasonManager.class);
        when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        when(plugin.state()).thenReturn(WildlandsPlugin.State.READY);
        when(plugin.seasons()).thenReturn(manager);
        when(plugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(manager.set(world, Season.MONSOON)).thenReturn(true);
        var command = new SeasonCommand(plugin);
        command.admin(sender, new String[]{"admin", "season", "winter", "world"});
        verify(manager, never()).set(any(), any());
        command.admin(sender, new String[]{"admin", "season", "monsoon", "world"});
        verify(manager).set(world, Season.MONSOON);
        verify(sender).sendMessage("[Wildlands] Season for world set to MONSOON; its day counter was reset.");
    }
}
