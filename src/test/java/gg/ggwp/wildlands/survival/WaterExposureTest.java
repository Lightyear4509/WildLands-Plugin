package gg.ggwp.wildlands.survival;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaterExposureTest {
    @Test void stormWithoutLocalRainDoesNotExposePlayer() {
        var player = mock(Player.class);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(world.hasStorm()).thenReturn(true);
        when(player.isInRain()).thenReturn(false);
        assertFalse(WaterExposure.sample(player).rain());
        verify(player, never()).getWorld();
    }

    @Test void standingInWaterCountsWithoutSwimmingPose() {
        var player = mock(Player.class);
        when(player.isInWaterOrBubbleColumn()).thenReturn(true);
        when(player.isSwimming()).thenReturn(false);
        assertTrue(WaterExposure.sample(player).immersed());
        verify(player, never()).isSwimming();
    }

    @Test void localRainIsSampledIndependentlyOfWater() {
        var player = mock(Player.class);
        when(player.isInRain()).thenReturn(true);
        assertEquals(new WaterExposure(true, false), WaterExposure.sample(player));
    }
}
