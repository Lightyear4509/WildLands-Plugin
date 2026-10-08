package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.WorldgenSettings;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.*;
import org.bukkit.material.MaterialData;
import org.junit.jupiter.api.Test;

class WorldGeneratorTest {
    @SuppressWarnings({"deprecation", "removal"})
    static class Buffer implements ChunkGenerator.ChunkData {
        final Material[] blocks = new Material[16 * 384 * 16];
        Buffer() { Arrays.fill(blocks, Material.AIR); }
        int index(int x, int y, int z) {
            assertTrue(x >= 0 && x < 16 && z >= 0 && z < 16 && y >= -64 && y < 320, "Access outside chunk");
            return (x * 384 + y + 64) * 16 + z;
        }
        public int getMinHeight() { return -64; }
        public int getMaxHeight() { return 320; }
        public Material getType(int x, int y, int z) { return blocks[index(x, y, z)]; }
        public void setBlock(int x, int y, int z, Material material) { blocks[index(x, y, z)] = material; }
        public void setBlock(int x, int y, int z, BlockData block) { setBlock(x, y, z, block.getMaterial()); }
        public void setRegion(int x, int y, int z, int xx, int yy, int zz, Material material) {
            for (int a = x; a < xx; a++) for (int b = y; b < yy; b++) for (int c = z; c < zz; c++) setBlock(a, b, c, material);
        }
        public void setRegion(int x, int y, int z, int xx, int yy, int zz, BlockData block) { setRegion(x, y, z, xx, yy, zz, block.getMaterial()); }
        public void setBlock(int x, int y, int z, MaterialData block) { throw new UnsupportedOperationException(); }
        public void setRegion(int x, int y, int z, int xx, int yy, int zz, MaterialData block) { throw new UnsupportedOperationException(); }
        public Biome getBiome(int x, int y, int z) { throw new UnsupportedOperationException(); }
        public MaterialData getTypeAndData(int x, int y, int z) { throw new UnsupportedOperationException(); }
        public BlockData getBlockData(int x, int y, int z) { throw new UnsupportedOperationException(); }
        public byte getData(int x, int y, int z) { throw new UnsupportedOperationException(); }
        public int getHeight(HeightMap map, int x, int z) { throw new UnsupportedOperationException(); }
    }
    @Test void chunkBufferIsDeterministicWithIndependentRandomsAndBoundedWrites() {
        var leaves = mock(BlockData.class); when(leaves.clone()).thenReturn(leaves); when(leaves.getMaterial()).thenReturn(Material.JUNGLE_LEAVES);
        var generator = new WorldGenerator(new WorldgenSettings(1, 63, .8, true, true), leaves);
        var info = mock(WorldInfo.class);
        when(info.getSeed()).thenReturn(4509L); when(info.getMinHeight()).thenReturn(-64); when(info.getMaxHeight()).thenReturn(320);
        var first = new Buffer(); var second = new Buffer();
        generator.generateNoise(info, new Random(1), -1, 0, first);
        generator.generateNoise(info, new Random(2), -1, 0, second);
        assertArrayEquals(first.blocks, second.blocks);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) assertEquals(Material.BEDROCK, first.getType(x, -64, z));
        assertTrue(Arrays.asList(first.blocks).contains(Material.STONE));
        assertFalse(generator.shouldGenerateNoise()); assertFalse(generator.shouldGenerateStructures());
        var terrain = new TerrainModel(4509, 63, -64, 320);
        assertEquals(terrain.column(-1, 0).groundY() + 1, generator.getBaseHeight(info, new Random(), -1, 0, HeightMap.OCEAN_FLOOR));
    }
}
