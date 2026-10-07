package gg.ggwp.wildlands.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.*;
import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.crossplay.CrossplayService;
import gg.ggwp.wildlands.storage.StorageService;
import java.util.*;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WildlandsCommandTest {
    WildlandsPlugin plugin;
    CommandSender sender;
    WildlandsCommand handler;
    Command command;
    @BeforeEach void setUp() {
        plugin = mock(WildlandsPlugin.class);
        sender = mock(CommandSender.class);
        command = mock(Command.class);
        when(plugin.state()).thenReturn(WildlandsPlugin.State.READY);
        configure(true);
        when(plugin.getPluginMeta()).thenReturn(new PluginDescriptionFile("Wildlands", "0.1.0", "test.Main"));
        handler = new WildlandsCommand(plugin);
    }
    void configure(boolean debug) {
        when(plugin.configuration()).thenReturn(new ConfigurationManager.Snapshot(
                new Settings("wildlands.db", 30, Map.of("player-records", true), debug),
                Map.of("prefix", "[Custom] ", "no-permission", "Denied", "unknown-command", "Unknown"), mock(HydrationSettings.class),
                mock(EnvironmentSettings.class)));
    }
    void run(String... args) { assertTrue(handler.onCommand(sender, command, "wildlands", args)); }
    @Test void deniesReloadWithoutInvokingMutation() {
        run("reload");
        verify(sender).sendMessage("[Custom] Denied");
        verify(plugin, never()).reload(any());
    }
    @Test void allowsConsoleReloadWithAdminPermission() {
        when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        run("reload");
        verify(plugin).reload(any());
    }
    @Test void debugHasItsOwnPermissionAndDoesNotFallBackToAdmin() {
        when(sender.hasPermission("ggwpwildlands.admin")).thenReturn(true);
        run("admin", "debug");
        verify(sender).sendMessage("[Custom] Denied");
        verify(plugin, never()).storage();
    }
    @Test void disablingDebugBlocksEvenPermittedSenders() {
        configure(false);
        when(sender.hasPermission("ggwpwildlands.admin.debug")).thenReturn(true);
        run("admin", "debug");
        verify(sender).sendMessage("[Custom] Debug is disabled in config.yml.");
        verify(plugin, never()).storage();
    }
    @Test void consoleStatusDoesNotRequirePlayer() {
        when(sender.hasPermission("ggwpwildlands.use")).thenReturn(true);
        run("status");
        verify(sender).sendMessage("[Custom] GGWP Wildlands 0.1.0 — foundation ready.");
        verify(plugin, never()).players();
    }
    @Test void consoleCanUseDebugWithOnlyDebugPermission() {
        when(sender.hasPermission("ggwpwildlands.admin.debug")).thenReturn(true);
        var server = mock(Server.class);
        when(server.getVersion()).thenReturn("Paper 26.2");
        when(plugin.getServer()).thenReturn(server);
        when(plugin.players()).thenReturn(mock(PlayerManager.class));
        var storage = mock(StorageService.class);
        when(plugin.storage()).thenReturn(storage);
        when(storage.health()).thenReturn("OK");
        var crossplay = mock(CrossplayService.class);
        when(plugin.crossplay()).thenReturn(crossplay);
        when(crossplay.geyserStatus()).thenReturn("ABSENT");
        when(crossplay.floodgateStatus()).thenReturn("ABSENT");
        when(plugin.modules()).thenReturn(new ModuleManager());
        run("admin", "debug");
        var messages = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(messages.capture());
        assertTrue(messages.getAllValues().stream().anyMatch(message -> message.contains("Geyser: ABSENT")));
    }
    @Test void completionHidesUnauthorizedCommandsAndInvisiblePlayers() {
        assertTrue(handler.onTabComplete(sender, command, "wildlands", new String[]{""}).isEmpty());
        when(sender.hasPermission("ggwpwildlands.use")).thenReturn(true);
        assertEquals(List.of("help", "hud", "status"), handler.onTabComplete(sender, command, "wildlands", new String[]{""}));
        var viewer = mock(Player.class);
        var visible = mock(Player.class);
        var hidden = mock(Player.class);
        when(viewer.hasPermission("ggwpwildlands.admin.debug")).thenReturn(true);
        when(viewer.canSee(visible)).thenReturn(true);
        when(visible.getName()).thenReturn("Visible");
        var server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        doReturn(List.of(visible, hidden)).when(server).getOnlinePlayers();
        assertEquals(List.of("Visible"),
                handler.onTabComplete(viewer, command, "wildlands", new String[]{"admin", "debug", ""}));
    }
    @Test void startingStateDoesNotDereferenceUninitializedServices() {
        when(plugin.state()).thenReturn(WildlandsPlugin.State.STARTING);
        when(plugin.configuration()).thenReturn(null);
        run("admin", "debug");
        verify(sender).sendMessage("[Wildlands] Foundation is starting.");
        verify(plugin, never()).storage();
    }
}
