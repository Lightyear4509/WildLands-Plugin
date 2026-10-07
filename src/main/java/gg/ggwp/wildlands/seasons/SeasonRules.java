package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.SeasonSettings;

public final class SeasonRules {
    public record Weather(boolean rain, boolean thunder) {}
    private SeasonRules() {}
    public static Weather weather(SeasonSettings.Profile profile, double rainRoll, double thunderRoll) {
        checkRoll(rainRoll); checkRoll(thunderRoll);
        boolean rain = rainRoll < profile.rainChance();
        return new Weather(rain, rain && thunderRoll < profile.thunderChance());
    }
    /** -1 cancels a natural growth event; an extra age step is capped at maturity. */
    public static int cropAge(int proposedAge, int maxAge, double multiplier, double roll) {
        checkRoll(roll);
        if (proposedAge < 0 || proposedAge > maxAge || !Double.isFinite(multiplier) || multiplier < 0 || multiplier > 2)
            throw new IllegalArgumentException("Invalid crop state/modifier");
        if (multiplier < 1) return roll < multiplier ? proposedAge : -1;
        return multiplier > 1 && roll < multiplier - 1 ? Math.min(maxAge, proposedAge + 1) : proposedAge;
    }
    private static void checkRoll(double roll) {
        if (!Double.isFinite(roll) || roll < 0 || roll >= 1) throw new IllegalArgumentException("Random roll must be in [0,1)");
    }
}
