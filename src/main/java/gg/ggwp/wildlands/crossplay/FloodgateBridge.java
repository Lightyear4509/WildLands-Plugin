package gg.ggwp.wildlands.crossplay;

import java.util.logging.Logger;

public final class FloodgateBridge extends OptionalApiBridge {
    public FloodgateBridge(Logger logger) {
        super("floodgate", "org.geysermc.floodgate.api.FloodgateApi", "getInstance", "isFloodgatePlayer", true, logger);
    }
}
