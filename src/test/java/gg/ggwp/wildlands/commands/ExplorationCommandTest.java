package gg.ggwp.wildlands.commands;

import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.world.LandmarkManager;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class ExplorationCommandTest {
    @Test void deniedAdminNeverReadsTheRegistryAndConsolePlayerCommandIsSafe() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class); var command = new ExplorationCommand(plugin);
        command.admin(sender, new String[]{"admin", "landmarks", "list"}); verifyNoInteractions(plugin);
        when(sender.hasPermission("ggwpwildlands.use")).thenReturn(true); command.player(sender, new String[]{"camp", "set"});
        verify(plugin, never()).landmarks(); verify(plugin, never()).getServer();
    }
    @Test void consoleCanInspectLandmarkDiagnostics() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class); when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        var manager = mock(LandmarkManager.class); when(plugin.landmarks()).thenReturn(manager);
        when(manager.records()).thenReturn(List.of()); when(manager.diagnostics()).thenReturn("Landmarks: enabled");
        new ExplorationCommand(plugin).admin(sender, new String[]{"admin", "landmarks", "list"});
        verify(sender).sendMessage("[Wildlands] Landmarks: enabled"); verify(plugin, never()).getServer();
    }
}
