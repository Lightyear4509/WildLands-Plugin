package gg.ggwp.wildlands.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerManagerTest {
    @TempDir Path directory;

    @Test void rapidReconnectDoesNotPublishStaleSessionAndRetainsOriginalUuidRecord() throws Exception {
        var storage = new StorageService(Logger.getAnonymousLogger());
        var callbacks = new ConcurrentLinkedQueue<Runnable>();
        var plugin = mock(WildlandsPlugin.class);
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; }).when(plugin).onMain(any());
        var manager = new PlayerManager(plugin, storage, 30);
        UUID uuid = UUID.randomUUID();
        var first = mock(Player.class);
        when(first.getUniqueId()).thenReturn(uuid);
        when(first.getName()).thenReturn("old-name");
        var second = mock(Player.class);
        when(second.getUniqueId()).thenReturn(uuid);
        when(second.getName()).thenReturn(".new-name");
        try {
            storage.submit(() -> { storage.open(directory.resolve("players.db")); return null; }).get(5, TimeUnit.SECONDS);
            manager.onJoin(new PlayerJoinEvent(first, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            long firstSeen = storage.submit(() -> storage.find(uuid).orElseThrow().firstSeen()).get(5, TimeUnit.SECONDS);
            manager.onQuit(new PlayerQuitEvent(first, (net.kyori.adventure.text.Component) null, PlayerQuitEvent.QuitReason.DISCONNECTED));
            manager.onJoin(new PlayerJoinEvent(second, (net.kyori.adventure.text.Component) null));
            storage.submit(() -> null).get(5, TimeUnit.SECONDS);
            assertEquals(2, callbacks.size());
            callbacks.remove().run();
            assertEquals("LOADING", manager.state(uuid));
            callbacks.remove().run();
            assertEquals("LOADED", manager.state(uuid));
            assertEquals(".new-name", manager.record(uuid).orElseThrow().lastKnownName());
            assertEquals(firstSeen, manager.record(uuid).orElseThrow().firstSeen());
            assertEquals(1, manager.size());
            manager.onQuit(new PlayerQuitEvent(second, (net.kyori.adventure.text.Component) null, PlayerQuitEvent.QuitReason.DISCONNECTED));
            assertEquals(0, manager.size());
            var saved = storage.submit(() -> storage.find(uuid).orElseThrow()).get(5, TimeUnit.SECONDS);
            assertEquals(".new-name", saved.lastKnownName());
            assertEquals(firstSeen, saved.firstSeen());
        } finally { storage.close(); }
    }
}
