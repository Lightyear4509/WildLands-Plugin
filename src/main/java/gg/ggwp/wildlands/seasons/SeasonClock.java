package gg.ggwp.wildlands.seasons;

import java.util.Map;
import java.util.Objects;

/** Pure game-time clock. No wall-clock catch-up or Bukkit state is involved. */
public final class SeasonClock {
    public record State(Season season, long elapsedTicks) {
        public State {
            Objects.requireNonNull(season);
            if (elapsedTicks < 0) throw new IllegalArgumentException("Negative season age");
        }
    }

    private SeasonClock() {}

    public static State advance(State state, long ticks, Map<Season, Long> durations) {
        Objects.requireNonNull(state);
        if (ticks < 0) throw new IllegalArgumentException("Negative clock increment");
        long cycle = 0;
        for (Season season : Season.values()) {
            Long duration = durations.get(season);
            if (duration == null || duration <= 0) throw new IllegalArgumentException("Missing/invalid duration for " + season);
            cycle = Math.addExact(cycle, duration);
        }
        long offset = 0;
        for (Season season : Season.values()) {
            if (season == state.season()) break;
            offset = Math.addExact(offset, durations.get(season));
        }
        // Reduce separately so a large increment cannot overflow the state sum.
        long position = addModulo(offset, state.elapsedTicks() % cycle, cycle);
        position = addModulo(position, ticks % cycle, cycle);
        for (Season season : Season.values()) {
            long duration = durations.get(season);
            if (position < duration) return new State(season, position);
            position -= duration;
        }
        throw new IllegalStateException("Invalid cycle position");
    }

    private static long addModulo(long first, long second, long modulus) {
        return first >= modulus - second ? first - (modulus - second) : first + second;
    }
}
