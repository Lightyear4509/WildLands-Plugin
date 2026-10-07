package gg.ggwp.wildlands.storage;

import java.util.UUID;

/** Persisted environment state; all values are bounded before they reach SQLite. */
public record EnvironmentRecord(UUID uuid, double temperature, double wetness) {
    public EnvironmentRecord {
        if (uuid == null || !Double.isFinite(temperature) || temperature < -100 || temperature > 100
                || !Double.isFinite(wetness) || wetness < 0 || wetness > 100)
            throw new IllegalArgumentException("Invalid environment record");
    }
    public EnvironmentRecord withTemperature(double value) { return new EnvironmentRecord(uuid, value, wetness); }
    public EnvironmentRecord withWetness(double value) { return new EnvironmentRecord(uuid, temperature, value); }
}
