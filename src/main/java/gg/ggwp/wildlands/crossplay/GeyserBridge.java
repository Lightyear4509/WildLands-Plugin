package gg.ggwp.wildlands.crossplay;

import java.util.logging.Logger;

public final class GeyserBridge extends OptionalApiBridge {
    public GeyserBridge(Logger logger) {
        super("Geyser-Spigot", "org.geysermc.geyser.api.GeyserApi", "api", "connectionByUuid", false, logger);
    }
}
