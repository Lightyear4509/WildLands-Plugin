package gg.ggwp.wildlands.commands;

import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class WildlifeCommandTest {
    @Test void denialPrecedesEntityLookupOrMutation() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class);
        new WildlifeCommand(plugin).admin(sender, new String[]{"admin", "wildlife", "spawn", "wildlands", "0", "65", "0"});
        verify(sender).sendMessage("[Wildlands] Permission denied."); verifyNoInteractions(plugin);
    }
}
