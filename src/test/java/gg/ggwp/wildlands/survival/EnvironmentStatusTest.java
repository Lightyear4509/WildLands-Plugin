package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.StorageService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvironmentStatusTest {
    @Test void disabledModulesDoNotReportLoadingOrScanShelter() {
        var shelter = mock(ShelterService.class);
        var player = mock(Player.class);
        var service = new EnvironmentService(mock(WildlandsPlugin.class), mock(StorageService.class), shelter);
        assertEquals("Temperature: disabled; wetness: disabled; shelter: disabled", service.status(player));
        verify(shelter, never()).assess(any());
    }

    @Test void shelterOnlyInstallationReportsProtectionWithoutDatabaseSession() {
        var shelter = mock(ShelterService.class);
        var player = mock(Player.class);
        when(shelter.enabled()).thenReturn(true);
        when(shelter.assess(player)).thenReturn(new ShelterStatus(true, true, true, true));
        var service = new EnvironmentService(mock(WildlandsPlugin.class), mock(StorageService.class), shelter);
        assertEquals("Temperature: disabled; wetness: disabled; shelter: Sheltered; campfire warmth", service.status(player));
    }
}
