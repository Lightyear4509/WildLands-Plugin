package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.config.HydrationSettings;
import gg.ggwp.wildlands.storage.HydrationRecord;

/** Pure rules; the service supplies activity sampled on the server thread. */
public final class HydrationRules {
    private HydrationRules() {}
    public record Step(HydrationRecord state, boolean damageDue) {}

    public static Step advance(HydrationRecord state, HydrationSettings settings, double seconds,
            boolean sprinting, boolean swimming, boolean combat, double environmentMultiplier) {
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 10)
            throw new IllegalArgumentException("Sampling duration must be between zero and ten seconds");
        if (!Double.isFinite(environmentMultiplier) || environmentMultiplier < 0 || environmentMultiplier > 10)
            throw new IllegalArgumentException("Environment multiplier must be between zero and ten");
        double activity = 1;
        if (sprinting) activity = Math.max(activity, settings.sprintMultiplier());
        if (swimming) activity = Math.max(activity, settings.swimMultiplier());
        if (combat) activity = Math.max(activity, settings.combatMultiplier());
        double rate = settings.lossPerSecond() * activity * environmentMultiplier;
        double next = Math.max(0, state.hydration() - rate * seconds);
        double dry = 0;
        if (next <= settings.severeAt()) {
            double timeToSevere = state.hydration() <= settings.severeAt() ? 0
                    : rate > 0 ? (state.hydration() - settings.severeAt()) / rate : seconds;
            dry = state.drySeconds() + Math.max(0, seconds - timeToSevere);
        }
        boolean damage = damageIndex(dry, settings) > damageIndex(state.drySeconds(), settings);
        return new Step(new HydrationRecord(state.uuid(), next, state.hudEnabled(), dry), damage);
    }

    private static long damageIndex(double duration, HydrationSettings settings) {
        if (duration < settings.damageDelaySeconds()) return 0;
        return 1 + (long) ((duration - settings.damageDelaySeconds()) / settings.damageIntervalSeconds());
    }

    public static HydrationRecord drink(HydrationRecord state, WaterQuality quality, HydrationSettings settings) {
        double next = Math.clamp(state.hydration() + settings.water().get(quality).restore(), 0, 100);
        return new HydrationRecord(state.uuid(), next, state.hudEnabled(),
                next > settings.severeAt() ? 0 : state.drySeconds());
    }

    public static boolean illness(WaterQuality quality, HydrationSettings settings, double random) {
        if (!Double.isFinite(random) || random < 0 || random >= 1)
            throw new IllegalArgumentException("Random sample must be in [0,1)");
        return random < settings.water().get(quality).illnessChance();
    }
}
