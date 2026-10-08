package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.Test;

class GenerationCompatibilityTest {
    @Test void frozenProfilesKeepTheirFullChunkOutput() throws Exception {
        var leaves = mock(BlockData.class); when(leaves.clone()).thenReturn(leaves); when(leaves.getMaterial()).thenReturn(Material.JUNGLE_LEAVES);
        var info = mock(WorldInfo.class);
        when(info.getSeed()).thenReturn(4509L); when(info.getMinHeight()).thenReturn(-64); when(info.getMaxHeight()).thenReturn(320);
        for (int version : new int[]{1, 2}) {
            var digest = MessageDigest.getInstance("SHA-256");
            var generator = new WorldGenerator(new WorldgenSettings(version, 63, .8, true, true), leaves);
            for (int[] chunk : new int[][]{{-1, 0}, {37, -8}, {0, 0}, {625, -625}, {-129, 128}}) {
                var buffer = new WorldGeneratorTest.Buffer();
                generator.generateNoise(info, new Random(123), chunk[0], chunk[1], buffer);
                for (var block : buffer.blocks) digest.update((block.name() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            String expected = version == 1 ? "2aeb40d5a364436a378264a1f522e7ee32a0652ec06006863897a31d69b1380b"
                    : "2041d8c027420328d25678ae2aef804c80edb88b6755484a123be9887f2678e4";
            assertEquals(expected, HexFormat.of().formatHex(digest.digest()), "Frozen generator profile " + version);
        }
    }
}
