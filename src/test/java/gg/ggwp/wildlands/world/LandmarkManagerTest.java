package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class LandmarkManagerTest {
    @TempDir Path directory;
    WildlandsPlugin plugin; StorageService storage; Server server; World world;
    final List<Runnable> tasks = new ArrayList<>(); final List<BukkitTask> handles = new ArrayList<>();
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        plugin = mock(WildlandsPlugin.class); storage = mock(StorageService.class); server = mock(Server.class); world = mock(World.class);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory).load()); when(plugin.getServer()).thenReturn(server);
        when(world.getUID()).thenReturn(UUID.randomUUID()); when(server.getWorlds()).thenReturn(List.of());
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        var scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); var task = mock(BukkitTask.class); handles.add(task); return task;
        });
        doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(plugin).onMain(any());
        when(storage.findExpedition(any())).thenReturn(Optional.empty()); when(storage.findDiscoveries(any())).thenReturn(Map.of());
        when(storage.submit(any())).thenAnswer(call -> {
            try { return CompletableFuture.completedFuture(call.getArgument(0, StorageService.Work.class).run()); }
            catch (Exception failure) { return CompletableFuture.failedFuture(failure); }
        });
    }
    Player player(UUID id) {
        var player = mock(Player.class); when(player.getUniqueId()).thenReturn(id); when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 100.5, 65, -99.5)); when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(server.getPlayer(id)).thenReturn(player);
        var inventory = mock(PlayerInventory.class); when(player.getInventory()).thenReturn(inventory);
        var held = mock(ItemStack.class); when(held.getType()).thenReturn(Material.STONE); when(inventory.getItemInMainHand()).thenReturn(held); return player;
    }
    @Test void twoPlayersDiscoverTheSamePublicLandmarkButPrivateCampsStayIsolatedAndNavigationNeverLoadsChunks() throws Exception {
        UUID javaId = UUID.randomUUID(), bedrockId = UUID.fromString("00000000-0000-0000-0009-01fe76725ed6");
        var java = player(javaId); var bedrock = player(bedrockId); doReturn(List.of(java, bedrock)).when(server).getOnlinePlayers();
        var landmark = new LandmarkRecord(UUID.randomUUID(), world.getUID(), "ruins-123", LandmarkKind.RUINS, 100, 65, -100, null);
        var camp = new LandmarkRecord(UUID.randomUUID(), world.getUID(), "camp-123", LandmarkKind.CAMP, 100, 65, -100, bedrockId);
        var manager = new LandmarkManager(plugin, storage, List.of(landmark, camp)); manager.enable();
        assertEquals("LOADED", manager.state(javaId)); assertTrue(manager.find(javaId, "camp").isEmpty()); assertEquals(camp, manager.find(bedrockId, "camp").orElseThrow());
        assertFalse(manager.track(java, landmark)); tasks.getFirst().run();
        assertEquals(List.of(landmark), manager.journal(javaId)); assertEquals(List.of(camp, landmark), manager.journal(bedrockId));
        var compass = mock(ItemStack.class); var meta = mock(CompassMeta.class); when(compass.getType()).thenReturn(Material.COMPASS);
        when(compass.getItemMeta()).thenReturn(meta); when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(java.getInventory().getItemInMainHand()).thenReturn(compass); when(server.getWorld(world.getUID())).thenReturn(world);
        assertTrue(manager.track(java, landmark)); verify(meta).setLodestone(new Location(world, 100, 65, -100)); verify(meta).setLodestoneTracked(false);
        assertEquals(Optional.of("HERE 0m"), manager.navigation(java));
        verify(world, never()).getChunkAt(anyInt(), anyInt()); verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
        manager.disable(); handles.forEach(task -> verify(task).cancel()); assertEquals("DISABLED", manager.state(javaId)); assertTrue(manager.navigation(java).isEmpty());
    }
    @Test void aFailedLoadRetriesBeforeAnyDiscoveryOrInitialStateIsSaved() throws Exception {
        UUID id = UUID.randomUUID(); var player = player(id); doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(storage.findExpedition(id)).thenThrow(new java.sql.SQLException("temporary read failure")).thenReturn(Optional.of(new ExpeditionRecord(id, null, 777, 2, false, ExpeditionRecord.Rank.EXPLORER)));
        var manager = new LandmarkManager(plugin, storage, List.of()); manager.enable(); assertEquals("LOAD_FAILED", manager.state(id));
        verify(storage, never()).saveExploration(any(), any(), any()); tasks.getFirst().run();
        assertEquals("LOADED", manager.state(id)); assertEquals(777, manager.player(id).orElseThrow().longestDistance());
        manager.disable();
    }
    @Test void markingCampRequiresPhysicalProtectionAndRelocatingAnExistingCampPreservesItsIdentity() {
        UUID id = UUID.randomUUID(); var player = player(id); doReturn(List.of(player)).when(server).getOnlinePlayers();
        var shelter = mock(gg.ggwp.wildlands.survival.ShelterService.class); when(plugin.shelter()).thenReturn(shelter);
        when(shelter.assess(player)).thenReturn(new gg.ggwp.wildlands.survival.ShelterStatus(false, false, true, true));
        var camp = new LandmarkRecord(UUID.randomUUID(), world.getUID(), "previous-camp", LandmarkKind.CAMP, 0, 65, 0, id);
        var manager = new LandmarkManager(plugin, storage, List.of(camp)); manager.enable();
        assertTrue(manager.camp(player).startsWith("Mark camp")); assertEquals(camp, manager.find(id, "camp").orElseThrow());
        when(shelter.assess(player)).thenReturn(new gg.ggwp.wildlands.survival.ShelterStatus(true, false, true, true));
        assertTrue(manager.camp(player).startsWith("Camp waypoint saved")); var moved = manager.find(id, "camp").orElseThrow();
        assertEquals(camp.id(), moved.id()); assertEquals(100, moved.x()); assertEquals(-100, moved.z()); assertEquals(1, manager.records().size());
        assertEquals(camp.id(), manager.player(id).orElseThrow().target()); manager.disable();
    }
}
