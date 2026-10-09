package gg.ggwp.wildlands.wildlife.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.*;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.crossplay.CrossplayService;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class RendererLifecycleTest {
    @TempDir Path directory;
    WildlandsPlugin plugin; Server server; World world; Player player; Ocelot cat;
    WildlifePresentation renderer; Runnable tick; Interaction proxy;
    List<ItemDisplay> displays;
    @BeforeEach void setup() throws Exception {
        plugin = mock(WildlandsPlugin.class); server = mock(Server.class); world = mock(World.class);
        var config = new ConfigurationManager(directory).load();
        when(plugin.configuration()).thenReturn(new ConfigurationManager.Snapshot(config.settings(), config.messages(), config.hydration(),
                config.environment(), config.seasons(), config.worldgen(), config.wildlife(), config.crafting(), config.food(), config.landmarks(),
                new VisualSettings(true, "https://example.com/wildlands.zip", "a".repeat(40), 1)));
        when(plugin.getServer()).thenReturn(server); when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getWorlds()).thenReturn(List.of(world)); when(world.getEntities()).thenReturn(List.of());
        when(plugin.crossplay()).thenReturn(mock(CrossplayService.class));
        player = mock(Player.class); cat = mock(Ocelot.class); proxy = mock(Interaction.class); displays = new ArrayList<>();
        when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(cat.getUniqueId()).thenReturn(UUID.randomUUID()); when(proxy.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world); when(cat.getWorld()).thenReturn(world); when(cat.isValid()).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(world, 0, 65, 2)); when(player.getEyeLocation()).thenReturn(new Location(world, 0, 66, 2));
        when(cat.getLocation()).thenReturn(new Location(world, 0, 65, 0)); when(cat.getBoundingBox()).thenReturn(new BoundingBox(-.3, 65, -.3, .3, 65.7, .3));
        when(player.hasLineOfSight(cat)).thenReturn(true); when(world.getPlayers()).thenReturn(List.of(player)); when(server.getPlayer(player.getUniqueId())).thenReturn(player);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class), org.mockito.ArgumentMatchers.<java.util.function.Consumer<? super ItemDisplay>>any())).thenAnswer(call -> { var display = mock(ItemDisplay.class); displays.add(display); return display; });
        when(world.spawn(any(Location.class), eq(Interaction.class), org.mockito.ArgumentMatchers.<java.util.function.Consumer<? super Interaction>>any())).thenReturn(proxy);
        var scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(4L), eq(4L))).thenAnswer(call -> { tick = call.getArgument(1); return mock(BukkitTask.class); });
        renderer = new WildlifePresentation(plugin, ignored -> 3); renderer.enable(); renderer.attach(cat);
    }
    private void accept() {
        renderer.choose(player, true);
        renderer.pack(new PlayerResourcePackStatusEvent(player, WildlifePresentation.PACK_ID, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED));
    }
    @AfterEach void cleanup() { renderer.disable(); }
    @Test void declinedPackNeverHidesAnimalOrSpawnsDisplays() {
        renderer.choose(player, true);
        renderer.pack(new PlayerResourcePackStatusEvent(player, WildlifePresentation.PACK_ID, PlayerResourcePackStatusEvent.Status.DECLINED)); tick.run();
        verify(player, never()).hideEntity(plugin, cat); assertTrue(displays.isEmpty());
    }
    @Test void bedrockWithoutOptionalBridgeRemainsOnNativeAnimal() {
        when(plugin.crossplay().platform(player.getUniqueId())).thenReturn(gg.ggwp.wildlands.crossplay.PlatformAdapter.Platform.BEDROCK);
        assertTrue(renderer.choose(player, true).contains("unavailable")); tick.run();
        verify(player, never()).hideEntity(plugin, cat); assertTrue(displays.isEmpty());
    }
    @Test void visualReloadPublishesExistingAnimalsWithoutWaitingForAiOrRespawn() {
        var previous = plugin.configuration(); renderer.phase(cat.getUniqueId(), gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Phase.WARNING);
        when(plugin.configuration()).thenReturn(new ConfigurationManager.Snapshot(previous.settings(), previous.messages(), previous.hydration(),
                previous.environment(), previous.seasons(), previous.worldgen(), previous.wildlife(), previous.crafting(), previous.food(), previous.landmarks()));
        renderer.configurationChanged(); PresentationRegistry.viewer(player.getUniqueId(), true);
        assertEquals(-1, PresentationRegistry.phase(cat.getUniqueId(), player.getUniqueId()));
        when(plugin.configuration()).thenReturn(previous); renderer.configurationChanged();
        assertEquals(2, PresentationRegistry.phase(cat.getUniqueId(), player.getUniqueId()));
    }
    @Test void successfulOwnPackCreatesSevenBonesAndOffRestoresNativeHitbox() {
        accept(); tick.run(); assertEquals(7, displays.size()); verify(player).hideEntity(plugin, cat);
        var attack = new PrePlayerAttackEntityEvent(player, proxy, true); renderer.attack(attack);
        assertTrue(attack.isCancelled()); verify(player).attack(cat);
        renderer.choose(player, false); verify(player).showEntity(plugin, cat);
        tick.run(); displays.forEach(display -> verify(display).remove()); verify(proxy).remove();
    }
    @Test void latePackSuccessAfterOffDoesNotReactivateAndOtherPacksDoNotActivate() {
        renderer.choose(player, true); renderer.choose(player, false);
        renderer.pack(new PlayerResourcePackStatusEvent(player, WildlifePresentation.PACK_ID, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED));
        renderer.pack(new PlayerResourcePackStatusEvent(player, UUID.randomUUID(), PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED)); tick.run();
        assertTrue(displays.isEmpty()); verify(player, never()).hideEntity(plugin, cat);
    }
    @Test void detachRemovesTemporaryEntitiesAndUnauthorizedProxyAttackIsRejected() {
        accept(); tick.run(); renderer.choose(player, false);
        renderer.attack(new PrePlayerAttackEntityEvent(player, proxy, true)); verify(player, never()).attack(cat);
        renderer.detach(cat.getUniqueId()); displays.forEach(display -> verify(display).remove()); verify(proxy).remove();
        assertTrue(renderer.diagnostics().contains("animals=0; displays=0"));
    }
    @Test void displayBudgetLimitsModelsAndOutOfRangeAttacksNeverReachAnimal() {
        var other = mock(Ocelot.class); when(other.getUniqueId()).thenReturn(UUID.randomUUID()); when(other.getWorld()).thenReturn(world);
        when(other.isValid()).thenReturn(true); when(other.getLocation()).thenReturn(new Location(world, 2, 65, 0)); renderer.attach(other);
        accept(); tick.run(); assertEquals(7, displays.size());
        when(player.getEyeLocation()).thenReturn(new Location(world, 3.1, 68.5, 3.1));
        renderer.attack(new PrePlayerAttackEntityEvent(player, proxy, true)); verify(player, never()).attack(cat);
        when(player.getEyeLocation()).thenReturn(new Location(world, 30, 66, 30));
        renderer.attack(new PrePlayerAttackEntityEvent(player, proxy, true)); verify(player, never()).attack(cat);
        renderer.disable(); displays.forEach(display -> verify(display).remove());
    }
    @Test void soundEmissionHasAnimalCooldownAndManagerWideBurstLimit() throws Exception {
        accept(); var ids = new ArrayList<UUID>();
        for (int index = 1; index <= 8; index++) {
            var animal = mock(Ocelot.class); UUID id = new UUID(0, index); ids.add(id);
            when(animal.getUniqueId()).thenReturn(id); when(animal.getWorld()).thenReturn(world); when(animal.getLocation()).thenReturn(new Location(world, 0, 65, 0));
            renderer.attach(animal); renderer.sound(id, "warning", null);
        }
        verify(player, times(4)).playSound(any(Location.class), eq("ggwpwildlands:jaguar.warning"), eq(SoundCategory.NEUTRAL), eq(.7f), eq(1f));
        var clock = renderer.getClass().getDeclaredField("ticks"); clock.setAccessible(true); clock.setLong(renderer, 20);
        renderer.sound(ids.getFirst(), "warning", null); // This animal must wait two seconds.
        renderer.sound(ids.get(4), "warning", null); // New manager window, previously budget-refused animal.
        verify(player, times(5)).playSound(any(Location.class), eq("ggwpwildlands:jaguar.warning"), eq(SoundCategory.NEUTRAL), eq(.7f), eq(1f));
    }
}
