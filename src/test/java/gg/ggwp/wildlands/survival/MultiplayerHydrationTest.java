package gg.ggwp.wildlands.survival;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiplayerHydrationTest {
    @TempDir Path directory;
    @Test void concurrentJavaAndFloodgateShapedSessionsKeepDrinksSettingsAndRestartRecordsIsolated() throws Exception {
        var plugin = mock(WildlandsPlugin.class); var server = mock(Server.class); var scheduler = mock(BukkitScheduler.class);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory.resolve("config")).load());
        when(plugin.getServer()).thenReturn(server); when(server.getScheduler()).thenReturn(scheduler);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(BukkitTask.class));
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; }).when(plugin).onMain(any());
        var players = new ArrayList<Player>(); var records = new ArrayList<HydrationRecord>();
        for (int i = 0; i < 32; i++) {
            UUID id = i < 16 ? new UUID(0x123456789abcdefL, i + 1) : new UUID(0, 0x10000000L + i);
            var player = mock(Player.class); when(player.getUniqueId()).thenReturn(id); when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            players.add(player); records.add(new HydrationRecord(id, 10 + i, true, 0));
        }
        doReturn(players).when(server).getOnlinePlayers(); var storage = new StorageService(Logger.getAnonymousLogger());
        try (var treatment = mockConstruction(WaterTreatment.class)) {
            storage.submit(() -> { storage.open(directory.resolve("data.db")); storage.saveHydration(records); return null; }).get(5, TimeUnit.SECONDS);
            var hydration = new HydrationService(plugin, storage); hydration.enable();
            storage.submit(() -> null).get(5, TimeUnit.SECONDS); assertEquals(32, callbacks.size());
            while (!callbacks.isEmpty()) callbacks.remove().run();
            assertTrue(hydration.drink(players.getFirst(), WaterQuality.CLEAN));
            assertTrue(hydration.hud(players.getLast().getUniqueId(), false));
            assertEquals(35, hydration.record(players.getFirst().getUniqueId()).orElseThrow().hydration());
            for (int i = 1; i < 32; i++) assertEquals(10 + i, hydration.record(players.get(i).getUniqueId()).orElseThrow().hydration());
            hydration.disable(); storage.submit(() -> null).get(5, TimeUnit.SECONDS);
        } finally { storage.close(); }
        var reopened = new StorageService(Logger.getAnonymousLogger());
        try {
            reopened.submit(() -> {
                reopened.open(directory.resolve("data.db"));
                for (int i = 0; i < 32; i++) {
                    var record = reopened.findHydration(players.get(i).getUniqueId()).orElseThrow();
                    assertEquals(i == 0 ? 35 : 10 + i, record.hydration()); assertEquals(i != 31, record.hudEnabled());
                }
                return null;
            }).get(5, TimeUnit.SECONDS);
        } finally { reopened.close(); }
    }
}
