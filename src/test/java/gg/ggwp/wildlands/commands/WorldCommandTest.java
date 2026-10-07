package gg.ggwp.wildlands.commands;

import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class WorldCommandTest {
    @Test void permissionDenialPrecedesWorldLookupAndCreation() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class);
        new WorldCommand(plugin).admin(sender, new String[]{"admin", "world", "create", "rainforest", "4509"});
        verify(sender).sendMessage("[Wildlands] Permission denied.");
        verifyNoInteractions(plugin);
    }
}
