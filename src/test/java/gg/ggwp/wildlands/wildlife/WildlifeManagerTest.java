package gg.ggwp.wildlands.wildlife;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Ocelot;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class WildlifeManagerTest {
    @TempDir Path directory;
    WildlandsPlugin plugin;
    StorageService storage;
    Server server;
    @BeforeEach void setup() throws Exception {
        plugin = mock(WildlandsPlugin.class); storage = mock(StorageService.class); server = mock(Server.class);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory).load());
        when(plugin.getServer()).thenReturn(server); when(server.getWorlds()).thenReturn(List.of());
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        doAnswer(call -> CompletableFuture.completedFuture(null)).when(storage).submit(any());
    }
    @Test void disableCancelsAllScheduledWorkAndUnloadedSpawnNeverReadsBlocks() {
        var scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        var tasks = new ArrayList<BukkitTask>();
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            var task = mock(BukkitTask.class); tasks.add(task); return task;
        });
        var manager = new WildlifeManager(plugin, storage, List.of()); manager.initialize(); manager.enable();
        var world = mock(World.class); when(world.getName()).thenReturn("wildlands"); when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        var border = mock(WorldBorder.class); when(world.getWorldBorder()).thenReturn(border); when(border.isInside(any())).thenReturn(true);
        assertTrue(manager.spawn(new Location(world, 0, 65, 0)).isEmpty());
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        manager.disable(); assertEquals(3, tasks.size()); tasks.forEach(task -> verify(task).cancel());
        assertTrue(manager.diagnostics().getFirst().contains("disabled"));
    }
    @Test void disabledTaggedAttackIsDeniedAndUnloadingPreservesRegistryButRemovalDeletesIt() throws Exception {
        var cat = mock(Ocelot.class); var data = mock(PersistentDataContainer.class);
        UUID id = UUID.randomUUID(); when(cat.getUniqueId()).thenReturn(id); when(cat.getPersistentDataContainer()).thenReturn(data);
        when(data.has(any(NamespacedKey.class), eq(PersistentDataType.BYTE))).thenReturn(true);
        var record = new WildlifeRecord(id, UUID.randomUUID(), 0, 65, 0, true);
        var manager = new WildlifeManager(plugin, storage, List.of(record));
        var damage = mock(EntityDamageByEntityEvent.class); when(damage.getDamager()).thenReturn(cat);
        manager.damage(damage); verify(damage).setCancelled(true);
        manager.remove(new EntityRemoveEvent(cat, EntityRemoveEvent.Cause.UNLOAD));
        assertTrue(manager.diagnostics().getFirst().contains("registered=1")); verifyNoInteractions(storage);
        manager.remove(new EntityRemoveEvent(cat, EntityRemoveEvent.Cause.PLUGIN));
        assertTrue(manager.diagnostics().getFirst().contains("registered=0"));
        @SuppressWarnings("rawtypes") var captor = ArgumentCaptor.forClass(StorageService.Work.class);
        verify(storage).submit(captor.capture()); captor.getValue().run();
        verify(storage).saveWildlife(List.of(record.dead()));
    }
    @Test void loadingASavedCorpseRemovesItWithoutRecreatingItsRegistryOrAi() throws Exception {
        var world = mock(World.class); when(world.getName()).thenReturn("wildlands"); when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        var cat = mock(Ocelot.class); var data = mock(PersistentDataContainer.class); UUID id = UUID.randomUUID();
        when(cat.getUniqueId()).thenReturn(id); when(cat.getWorld()).thenReturn(world); when(cat.getPersistentDataContainer()).thenReturn(data);
        when(cat.isDead()).thenReturn(true); when(data.has(any(NamespacedKey.class), eq(PersistentDataType.BYTE))).thenReturn(true);
        when(world.getEntities()).thenReturn(List.of(cat)); when(server.getWorlds()).thenReturn(List.of(world));
        var scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(BukkitTask.class));
        var record = new WildlifeRecord(id, UUID.randomUUID(), 0, 65, 0, true);
        var manager = new WildlifeManager(plugin, storage, List.of(record)); manager.enable();
        verify(cat).remove(); verify(server, never()).getMobGoals();
        assertTrue(manager.diagnostics().getFirst().contains("registered=0; loaded=0"));
        @SuppressWarnings("rawtypes") var work = ArgumentCaptor.forClass(StorageService.Work.class);
        verify(storage).submit(work.capture()); work.getValue().run(); verify(storage).saveWildlife(List.of(record.dead()));
    }
}
