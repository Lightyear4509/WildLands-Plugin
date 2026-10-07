package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.EnvironmentSettings;
import gg.ggwp.wildlands.storage.EnvironmentRecord;

/** Pure calculations make environmental tuning testable without Bukkit world state. */
public final class EnvironmentRules {
    private EnvironmentRules() {}
    public static double activityAdjustment(EnvironmentSettings settings, boolean sprinting) {
        return sprinting ? settings.sprintWarmth() : 0;
    }
    public static double dailyAdjustment(EnvironmentSettings settings, long worldTime, boolean dayCycle) {
        if (!dayCycle) return 0;
        return settings.dailyTemperatureAmplitude() * Math.sin(Math.floorMod(worldTime, 24000L) * Math.PI / 12000.0);
    }
    public static double temperature(EnvironmentSettings settings, double biomeTemperature, double y,
            boolean raining, boolean swimming, ShelterStatus shelter) {
        double value = biomeTemperature - Math.max(0, y - settings.seaLevel()) * settings.elevationCoolingPerBlock();
        if (raining && !shelter.protectedFromRain()) value -= settings.rainCooling();
        if (swimming) value -= settings.waterCooling();
        if (shelter.shaded()) value -= settings.shadeCooling();
        if (shelter.nearbyCampfire()) value += settings.campfireWarmth();
        return Math.clamp(value, -100, 100);
    }
    public static EnvironmentRecord wetness(EnvironmentRecord state, EnvironmentSettings settings, double seconds,
            boolean raining, boolean swimming, ShelterStatus shelter) {
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 10) throw new IllegalArgumentException("Invalid sample duration");
        double change = 0;
        if (swimming) change += settings.wetnessSwimPerSecond() * seconds;
        else if (raining && !shelter.protectedFromRain()) change += settings.wetnessRainPerSecond() * seconds;
        else change -= settings.wetnessDryPerSecond() * seconds * (shelter.protectedFromRain() || shelter.nearbyCampfire()
                ? settings.wetnessShelterDryMultiplier() : 1);
        return state.withWetness(Math.clamp(state.wetness() + change, 0, 100));
    }
    public static double hydrationMultiplier(EnvironmentRecord state, EnvironmentSettings settings) {
        return state.temperature() >= settings.hotAt() ? settings.hotHydrationMultiplier() : 1;
    }
    public static boolean coldWet(EnvironmentRecord state, EnvironmentSettings settings) {
        return state.temperature() <= settings.wetColdAt() && state.wetness() >= 50;
    }
}
