package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.junit.jupiter.api.Test;

class LandmarkPlannerTest {
    @Test void eachCellHasExactlyOneOrderIndependentCandidateIncludingNegativeCells() {
        var settings = new WorldgenSettings(2, 63, .8, true, true);
        for (int cellX : new int[]{-1, 0, 1}) for (int cellZ : new int[]{-1, 0, 1}) {
            var found = new ArrayList<LandmarkPlanner.Candidate>();
            for (int cx = cellX * 16; cx < cellX * 16 + 16; cx++) for (int cz = cellZ * 16; cz < cellZ * 16 + 16; cz++) {
                var candidate = LandmarkPlanner.inChunk(4509, settings, -64, 320, cx, cz);
                assertEquals(candidate, LandmarkPlanner.inChunk(4509, settings, -64, 320, cx, cz)); candidate.ifPresent(found::add);
            }
            assertEquals(1, found.size()); var candidate = found.getFirst(); UUID world = UUID.randomUUID();
            assertEquals(candidate.record(world), candidate.record(world)); assertNotEquals(candidate.record(world).id(), candidate.record(UUID.randomUUID()).id());
            assertEquals(cellX, Math.floorDiv(candidate.x(), 256)); assertEquals(cellZ, Math.floorDiv(candidate.z(), 256));
        }
    }
    @Test void ruinsStayWithinTheirChunkAndOldProfilesNeverProduceThem() {
        var data = mock(ChunkData.class); when(data.getMinHeight()).thenReturn(-64); when(data.getMaxHeight()).thenReturn(320);
        var candidate = new LandmarkPlanner.Candidate(-8, 70, -8, LandmarkKind.RUINS);
        doAnswer(call -> {
            int x = call.getArgument(0), y = call.getArgument(1), z = call.getArgument(2);
            assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -64 && y < 320); return null;
        }).when(data).setBlock(anyInt(), anyInt(), anyInt(), any(Material.class));
        StructureManager.ruin(4509, candidate, -1, -1, data); verify(data).setBlock(8, 71, 3, Material.AIR);
        verify(data, atLeastOnce()).setBlock(anyInt(), eq(70), anyInt(), eq(Material.MOSSY_STONE_BRICKS));
        for (int cx = -32; cx <= 32; cx++) for (int cz = -32; cz <= 32; cz++)
            LandmarkPlanner.inChunk(4509, new WorldgenSettings(1, 63, .8, true, true), -64, 320, cx, cz)
                    .ifPresent(found -> assertNotEquals(LandmarkKind.RUINS, found.kind()));
    }
}
