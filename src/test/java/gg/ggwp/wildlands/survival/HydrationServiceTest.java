package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HydrationServiceTest {
    @TempDir Path directory;

    @Test void reconnectRejectsStaleLoadAndPersistsChangesAndHudPreference() throws Exception {
        var plugin = mock(WildlandsPlugin.class);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory.resolve("config")).load());
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; }).when(plugin).onMain(any());
        var storage = new StorageService(Logger.getAnonymousLogger());
        var hydration = new HydrationService(plugin, storage);
        var server = mock(org.bukkit.Server.class);
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(call -> mock(org.bukkit.scheduler.BukkitTask.class));
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        try (var treatment = mockConstruction(WaterTreatment.class)) {
            storage.submit(() -> {
                storage.open(directory.resolve("data.db"));
                storage.saveHydration(List.of(new HydrationRecord(id, 17, true, 0)));
                return null;
            }).get(5, TimeUnit.SECONDS);
            hydration.enable();
            hydration.onJoin(new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            assertFalse(hydration.set(id, 100), "Cannot overwrite unloaded saved state");
            quit(hydration, player);
            hydration.onJoin(new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            callbacks.remove().run();
            assertEquals("LOADING", hydration.state(id));
            callbacks.remove().run();
            assertEquals(17, hydration.record(id).orElseThrow().hydration());
            when(player.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
            var recovery = new org.bukkit.event.entity.EntityRegainHealthEvent(player, 2,
                    org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED);
            hydration.onRecovery(recovery);
            assertEquals(1, recovery.getAmount(), "Moderate dehydration halves natural recovery");
            var magicalRecovery = new org.bukkit.event.entity.EntityRegainHealthEvent(player, 2,
                    org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.MAGIC);
            hydration.onRecovery(magicalRecovery);
            assertEquals(2, magicalRecovery.getAmount(), "Potion healing remains unchanged");
            when(player.getGameMode()).thenReturn(org.bukkit.GameMode.CREATIVE);
            var creativeRecovery = new org.bukkit.event.entity.EntityRegainHealthEvent(player, 2,
                    org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED);
            hydration.onRecovery(creativeRecovery);
            assertEquals(2, creativeRecovery.getAmount(), "Creative players are exempt");
            assertTrue(hydration.set(id, 65));
            assertTrue(hydration.hud(id, false));
            quit(hydration, player);
            hydration.onJoin(new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            callbacks.remove().run();
            assertEquals(new HydrationRecord(id, 65, false, 0), hydration.record(id).orElseThrow());
            hydration.trackHud(true);
            hydration.disable();
            assertTrue(hydration.hud(id, true), "HUD preference remains writable with hydration disabled");
            assertFalse(hydration.set(id, 0));
            assertFalse(hydration.drink(player, WaterQuality.CLEAN));
            var respawn = mock(PlayerRespawnEvent.class);
            when(respawn.getPlayer()).thenReturn(player);
            hydration.onRespawn(respawn);
            assertEquals(65, hydration.record(id).orElseThrow().hydration());
            hydration.trackHud(false);
            assertTrue(hydration.record(id).isEmpty(), "Last consumer releases the session");
        } finally { storage.close(); }
    }
    private static void quit(HydrationService hydration, Player player) {
        hydration.onQuit(new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null, PlayerQuitEvent.QuitReason.DISCONNECTED));
    }
}
