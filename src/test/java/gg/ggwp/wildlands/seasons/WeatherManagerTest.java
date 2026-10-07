package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.SeasonSettings;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class WeatherManagerTest {
    @Test void guaranteedDryAndStormProfilesSetBoundedDurationsWithoutBlockScans() {
        var world = mock(World.class);
        var manager = new WeatherManager();
        manager.evaluate(world, new SeasonSettings.Profile(14, 3, 0, 0, .8), 300);
        verify(world).setStorm(false); verify(world).setThundering(false);
        verify(world).setClearWeatherDuration(6000);
        clearInvocations(world);
        manager.evaluate(world, new SeasonSettings.Profile(5, -2, 1, 1, 1.1), 300);
        verify(world).setStorm(true); verify(world).setThundering(true);
        verify(world).setClearWeatherDuration(0);
        verify(world).setWeatherDuration(6000); verify(world).setThunderDuration(6000);
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }
}
