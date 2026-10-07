package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeasonManagerTest {
    @TempDir Path directory;
    private final WildlandsPlugin plugin = mock(WildlandsPlugin.class);
    private final World world = mock(World.class);
    private final UUID id = UUID.randomUUID();
    private final Queue<Runnable> callbacks = new ConcurrentLinkedQueue<>();
    private final List<Runnable> tasks = new ArrayList<>();

    private SeasonManager setup(StorageService storage) throws Exception {
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory.resolve("config")).load());
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getWorlds()).thenReturn(List.of(world));
        when(server.getWorld(id)).thenReturn(world);
        when(world.getUID()).thenReturn(id);
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getWeatherDuration()).thenReturn(1000);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); return mock(BukkitTask.class);
        });
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; }).when(plugin).onMain(any());
        storage.submit(() -> { storage.open(directory.resolve("seasons.db")); return null; }).get(5, TimeUnit.SECONDS);
        return new SeasonManager(plugin, storage);
    }
    private void drain(StorageService storage) throws Exception {
        storage.submit(() -> null).get(5, TimeUnit.SECONDS);
        Runnable callback;
        while ((callback = callbacks.poll()) != null) callback.run();
    }

    @Test void savedClockLoadsBeforeEffectsAndAdvancesThenPersistsOnShutdown() throws Exception {
        var storage = new StorageService(Logger.getAnonymousLogger());
        try {
            var manager = setup(storage);
            storage.submit(() -> { storage.saveSeasons(List.of(new SeasonRecord(id,
                    new SeasonClock.State(Season.MONSOON, 12345)))); return null; }).get(5, TimeUnit.SECONDS);
            manager.enable();
            assertTrue(manager.state(world).isEmpty());
            assertFalse(manager.set(world, Season.DRY), "Unloaded data cannot be overwritten");
            verify(world, never()).setStorm(anyBoolean());
            drain(storage);
            assertEquals(new SeasonClock.State(Season.MONSOON, 12345), manager.state(world).orElseThrow());
            assertEquals(-2, manager.temperatureDelta(world));
            tasks.getFirst().run();
            assertEquals(12545, manager.state(world).orElseThrow().elapsedTicks());
            assertTrue(manager.set(world, Season.WET));
            tasks.getFirst().run();
            manager.disable();
            drain(storage);
            assertEquals(new SeasonClock.State(Season.WET, 200),
                    storage.submit(() -> storage.findSeason(id).orElseThrow().state()).get(5, TimeUnit.SECONDS));
            assertEquals(0, manager.temperatureDelta(world));
            verify(world).setWeatherDuration(1000); // Restore the weather captured before seasonal control.
        } finally { storage.close(); }
    }

    @Test void delayedLoadCannotReenableEffectsAfterDisableAndOtherDimensionsAreUnmanaged() throws Exception {
        var storage = new StorageService(Logger.getAnonymousLogger());
        try {
            var manager = setup(storage);
            manager.enable();
            manager.disable();
            drain(storage);
            assertTrue(manager.state(world).isEmpty());
            verify(world, never()).setStorm(anyBoolean());
            when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
            manager.enable();
            drain(storage);
            assertTrue(manager.state(world).isEmpty());
            verify(world, never()).setStorm(anyBoolean());
            manager.disable();
        } finally { storage.close(); }
    }
}
