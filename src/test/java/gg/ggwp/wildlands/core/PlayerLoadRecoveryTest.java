package gg.ggwp.wildlands.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerLoadRecoveryTest {
    @TempDir Path directory;

    @Test void transientJoinFailureRecoversOnlineWithoutReconnectAndPreservesFirstSeen() throws Exception {
        Path file = directory.resolve("recovery.db");
        var storage = new StorageService(Logger.getAnonymousLogger());
        var plugin = mock(WildlandsPlugin.class);
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        var timer = new AtomicReference<Runnable>();
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getScheduler()).thenReturn(scheduler);
        doReturn(List.of()).when(server).getOnlinePlayers();
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; }).when(plugin).onMain(any());
        doAnswer(call -> {
            timer.set(call.getArgument(1));
            return mock(BukkitTask.class);
        }).when(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(600L), eq(600L));
        UUID uuid = UUID.randomUUID();
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("returning-player");
        var manager = new PlayerManager(plugin, storage, 30);
        try {
            storage.submit(() -> {
                storage.open(file);
                storage.save(List.of(new PlayerRecord(uuid, "previous-name", 1, 2)));
                return null;
            }).get(5, TimeUnit.SECONDS);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER temporarily_fail BEFORE INSERT ON players BEGIN SELECT RAISE(ABORT, 'temporary failure'); END");
            }
            manager.enable();
            manager.onJoin(new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            drain(callbacks);
            assertNotEquals("LOADED", manager.state(uuid));

            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement()) { statement.execute("DROP TRIGGER temporarily_fail"); }
            timer.get().run();
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            drain(callbacks);
            assertEquals("LOADED", manager.state(uuid));
            assertEquals(1, manager.record(uuid).orElseThrow().firstSeen());
            assertEquals("returning-player", manager.record(uuid).orElseThrow().lastKnownName());
        } finally {
            manager.disable();
            storage.close();
        }
    }
    private static void drain(Queue<Runnable> callbacks) {
        Runnable callback;
        while ((callback = callbacks.poll()) != null) callback.run();
    }
}
