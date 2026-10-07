package gg.ggwp.wildlands.world;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorldPathsTest {
    @Test void modernDimensionPathsStayInsideSharedLevel() {
        var level = Path.of("server", "world").toAbsolutePath();
        assertEquals(level.resolve("dimensions/minecraft/rainforest"), WorldPaths.dimension(level, "rainforest"));
        assertTrue(WorldPaths.dimension(level, "rainforest").normalize().startsWith(level));
        assertThrows(IllegalArgumentException.class, () -> WorldPaths.dimension(level, "../world"));
    }
}
