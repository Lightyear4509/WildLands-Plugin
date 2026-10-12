package gg.ggwp.wildlands.wildlife.behaviors;

import gg.ggwp.wildlands.config.WildlifeExpansionSettings.Animal;
import gg.ggwp.wildlands.wildlife.Species;

/** Harmless ecological states. Foraging does not remove blocks or consume player crops. */
public final class PassiveBehavior {
    public enum Phase { RESTING, FORAGING, GROUPING, SWIMMING, RETREATING, RETURNING, FLITTING, PERCHING, HOPPING }
    public record Memory(Phase phase, double fleeRemaining) {
        public Memory { java.util.Objects.requireNonNull(phase); if (!Double.isFinite(fleeRemaining) || fleeRemaining < 0 || fleeRemaining > 30) throw new IllegalArgumentException("Invalid flee memory"); }
        public static Memory idle() { return new Memory(Phase.RESTING, 0); }
    }
    public record Observation(double playerDistance, double homeDistance, boolean provoked,
                              boolean swimming, boolean hasGroup, boolean waterNearby, boolean night, boolean fire) {
        public Observation { if (Double.isNaN(playerDistance) || playerDistance < 0 || !Double.isFinite(homeDistance) || homeDistance < 0) throw new IllegalArgumentException("Invalid passive observation"); }
    }
    private PassiveBehavior() { }
    public static Memory step(Species species, Memory memory, Observation observation, Animal settings, double seconds) {
        if (!species.passive() || !Double.isFinite(seconds) || seconds <= 0 || seconds > 10) throw new IllegalArgumentException("Invalid passive decision step");
        if (observation.homeDistance() > settings.roamRadius()) return new Memory(Phase.RETURNING, 0);
        if (observation.provoked() || observation.fire() || observation.playerDistance() <= settings.avoidanceRange())
            return new Memory(Phase.RETREATING, settings.fleeSeconds());
        double remaining = Math.max(0, memory.fleeRemaining() - seconds);
        if (remaining > 0) return new Memory(Phase.RETREATING, remaining);
        if (species == Species.TOUCAN) return new Memory(observation.night() ? Phase.PERCHING : Phase.FLITTING, 0);
        if (species == Species.FROG) return new Memory(observation.swimming() ? Phase.SWIMMING : Phase.HOPPING, 0);
        if (species == Species.MONKEY) return new Memory(observation.night() ? Phase.RESTING : observation.hasGroup() ? Phase.GROUPING : Phase.FORAGING, 0);
        if (species == Species.CAPYBARA) {
            if (observation.swimming() || observation.waterNearby()) return new Memory(Phase.SWIMMING, 0);
            if (observation.hasGroup()) return new Memory(Phase.GROUPING, 0);
            return new Memory(observation.night() ? Phase.RESTING : Phase.FORAGING, 0);
        }
        return new Memory(observation.night() ? Phase.FORAGING : Phase.RESTING, 0);
    }
}
