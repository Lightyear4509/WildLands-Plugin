package gg.ggwp.wildlands.world;

import java.nio.file.Path;

/** Paper 26.2 dimensions live inside the shared level directory. */
public final class WorldPaths {
    private WorldPaths() {}
    public static Path dimension(Path levelDirectory, String name) {
        if (!name.matches("[a-z][a-z0-9_-]{0,47}")) throw new IllegalArgumentException("Unsafe world name");
        return levelDirectory.resolve("dimensions").resolve("minecraft").resolve(name);
    }
}
