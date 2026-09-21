package gg.ggwp.wildlands.survival;

import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.block.*;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CauldronWaterTest {
    @Test void rainIsCleanButBucketMixingAndWashingCannotLaunderWater() {
        var block = mock(Block.class);
        var world = mock(World.class);
        var chunk = mock(Chunk.class);
        var data = mock(PersistentDataContainer.class);
        Map<NamespacedKey, String> values = new HashMap<>();
        when(block.getWorld()).thenReturn(world);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getY()).thenReturn(64);
        when(world.hasStorm()).thenReturn(true);
        when(world.getHighestBlockYAt(0, 0)).thenReturn(64);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenAnswer(call -> values.get(call.getArgument(0)));
        doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(2)); return null; })
                .when(data).set(any(), eq(PersistentDataType.STRING), anyString());
        doAnswer(call -> { values.remove(call.getArgument(0)); return null; }).when(data).remove(any());
        var provenance = new CauldronWater();
        when(block.getType()).thenReturn(Material.CAULDRON);
        provenance.onLevel(change(block, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL));
        assertEquals(WaterQuality.CLEAN, provenance.quality(block));
        when(block.getType()).thenReturn(Material.WATER_CAULDRON);
        provenance.onLevel(change(block, CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY));
        assertEquals(WaterQuality.QUESTIONABLE, provenance.quality(block));
        provenance.onLevel(change(block, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL));
        assertEquals(WaterQuality.QUESTIONABLE, provenance.quality(block));
        provenance.onLevel(change(block, CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH));
        assertEquals(WaterQuality.CONTAMINATED, provenance.quality(block));
        var broken = mock(BlockBreakEvent.class);
        when(broken.getBlock()).thenReturn(block);
        provenance.onBreak(broken);
        assertTrue(values.isEmpty());
        assertEquals(WaterQuality.QUESTIONABLE, provenance.quality(block));
    }
    private CauldronLevelChangeEvent change(Block block, CauldronLevelChangeEvent.ChangeReason reason) {
        var event = mock(CauldronLevelChangeEvent.class);
        var state = mock(BlockState.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getReason()).thenReturn(reason);
        when(event.getNewState()).thenReturn(state);
        when(state.getType()).thenReturn(Material.WATER_CAULDRON);
        return event;
    }
}
