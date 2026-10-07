package gg.ggwp.wildlands.storage;

import gg.ggwp.wildlands.seasons.SeasonClock;
import java.util.Objects;
import java.util.UUID;

public record SeasonRecord(UUID worldUuid, SeasonClock.State state) {
    public SeasonRecord { Objects.requireNonNull(worldUuid); Objects.requireNonNull(state); }
}
