package gg.ggwp.wildlands.crossplay;

import java.util.UUID;

public interface PlatformAdapter {
    enum Platform { BEDROCK, JAVA_OR_UNDETECTED }
    Platform platform(UUID uuid);
}
