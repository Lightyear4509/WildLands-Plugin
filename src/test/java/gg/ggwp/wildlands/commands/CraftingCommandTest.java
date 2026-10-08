package gg.ggwp.wildlands.commands;

import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.items.CustomItemManager;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class CraftingCommandTest {
    @Test void permissionDenialNeverLooksUpAPlayerOrMutatesInventory() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class);
        new CraftingCommand(plugin).admin(sender, new String[]{"admin", "crafting", "give", "Hidden", "waterskin"});
        verify(sender).sendMessage("[Wildlands] Permission denied."); verifyNoInteractions(plugin);
    }
    @Test void consoleCanInspectWithoutPlayerState() {
        var plugin = mock(WildlandsPlugin.class); var sender = mock(CommandSender.class);
        when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        var crafting = mock(CustomItemManager.class); when(plugin.crafting()).thenReturn(crafting);
        when(crafting.diagnostics()).thenReturn("Crafting: enabled");
        new CraftingCommand(plugin).admin(sender, new String[]{"admin", "crafting", "info"});
        verify(sender).sendMessage("[Wildlands] Crafting: enabled"); verify(plugin, never()).getServer();
    }
}
