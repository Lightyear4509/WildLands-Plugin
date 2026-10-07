package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.ConfigurationManager;
import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.EnvironmentRecord;
import gg.ggwp.wildlands.storage.StorageService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvironmentRecoveryTest {
    @TempDir Path directory;

    @Test void coldWetRecoveryRequiresBothModulesAndOnlyChangesNaturalSurvivalHealing() throws Exception {
        var plugin = mock(WildlandsPlugin.class);
        when(plugin.configuration()).thenReturn(new ConfigurationManager(directory).load());
        var server = mock(Server.class);
        var scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getOnlinePlayers()).thenReturn(List.of());
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mock(BukkitTask.class));
        var service = spy(new EnvironmentService(plugin, mock(StorageService.class), mock(ShelterService.class)));
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        doReturn(Optional.of(new EnvironmentRecord(id, 8, 50))).when(service).record(id);
        service.temperatureModule().enable();
        assertEquals(2, recover(service, player, EntityRegainHealthEvent.RegainReason.SATIATED));
        service.wetnessModule().enable();
        assertEquals(1.5, recover(service, player, EntityRegainHealthEvent.RegainReason.SATIATED));
        assertEquals(2, recover(service, player, EntityRegainHealthEvent.RegainReason.MAGIC));
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        assertEquals(2, recover(service, player, EntityRegainHealthEvent.RegainReason.SATIATED));
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        doReturn(Optional.of(new EnvironmentRecord(id, 8, 49))).when(service).record(id);
        assertEquals(2, recover(service, player, EntityRegainHealthEvent.RegainReason.SATIATED));
    }

    private double recover(EnvironmentService service, Player player, EntityRegainHealthEvent.RegainReason reason) {
        var event = new EntityRegainHealthEvent(player, 2, reason);
        service.onRecovery(event);
        return event.getAmount();
    }
}
