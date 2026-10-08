package gg.ggwp.wildlands.wildlife.behaviors;

/** Pure, bounded territorial behavior; navigation, sounds and damage belong to the server adapter. */
public final class JaguarBehavior {
    public enum Phase { IDLE, STALKING, WARNING, ATTACKING, RETREATING }
    public record Limits(double detectionRange, double warningRange, double engagementRange, double territoryRadius,
                         double warningSeconds, double chaseSeconds, double retreatSeconds, int deterrentGroupSize) {
        public Limits {
            if (!Double.isFinite(detectionRange) || !Double.isFinite(warningRange) || !Double.isFinite(engagementRange)
                    || !Double.isFinite(territoryRadius) || !Double.isFinite(warningSeconds) || !Double.isFinite(chaseSeconds)
                    || !Double.isFinite(retreatSeconds) || engagementRange <= 0 || warningRange < engagementRange
                    || detectionRange < warningRange || detectionRange > 32 || territoryRadius < detectionRange
                    || territoryRadius > 96 || warningSeconds < 2 || chaseSeconds < warningSeconds
                    || chaseSeconds > 60 || retreatSeconds < 5 || retreatSeconds > 300 || deterrentGroupSize < 2)
                throw new IllegalArgumentException("Invalid jaguar behavior limits");
        }
    }
    public record Memory(Phase phase, double seconds) {
        public Memory {
            java.util.Objects.requireNonNull(phase);
            if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid behavior age");
        }
        public static Memory idle() { return new Memory(Phase.IDLE, 0); }
    }
    public record Observation(double targetDistance, double distanceFromHome, int nearbyPlayers, boolean fire,
                              boolean night, boolean provoked, boolean lineOfSight, boolean prey) {
        public Observation {
            if (Double.isNaN(targetDistance) || targetDistance < 0 || !Double.isFinite(distanceFromHome)
                    || distanceFromHome < 0 || nearbyPlayers < 0) throw new IllegalArgumentException("Invalid observation");
        }
    }
    private JaguarBehavior() {}
    public static Memory step(Memory previous, Observation observation, Limits limits, double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 10) throw new IllegalArgumentException("Invalid step duration");
        double age = previous.seconds() + seconds;
        boolean danger = observation.fire() || observation.nearbyPlayers() >= limits.deterrentGroupSize()
                || observation.distanceFromHome() > limits.territoryRadius();
        boolean target = observation.lineOfSight() && observation.targetDistance() <= limits.detectionRange();
        if (danger) return transition(previous, Phase.RETREATING, age);
        if (previous.phase() == Phase.RETREATING)
            return age >= limits.retreatSeconds() ? Memory.idle() : new Memory(Phase.RETREATING, age);
        if (!target) return previous.phase() == Phase.ATTACKING || previous.phase() == Phase.STALKING
                ? transition(previous, Phase.RETREATING, age) : Memory.idle();
        return switch (previous.phase()) {
            case IDLE -> observation.provoked() || observation.targetDistance() <= limits.warningRange()
                    ? transition(previous, observation.prey() ? Phase.ATTACKING : Phase.WARNING, age)
                    : observation.night() ? transition(previous, Phase.STALKING, age) : Memory.idle();
            case STALKING -> age >= limits.chaseSeconds() ? transition(previous, Phase.RETREATING, age)
                    : observation.targetDistance() <= limits.warningRange()
                        ? transition(previous, observation.prey() ? Phase.ATTACKING : Phase.WARNING, age)
                        : new Memory(Phase.STALKING, age);
            case WARNING -> observation.targetDistance() > limits.warningRange() ? transition(previous, Phase.RETREATING, age)
                    : age >= limits.warningSeconds() && observation.targetDistance() <= limits.engagementRange()
                        ? transition(previous, Phase.ATTACKING, age)
                        : age >= limits.chaseSeconds() ? transition(previous, Phase.RETREATING, age) : new Memory(Phase.WARNING, age);
            case ATTACKING -> age >= limits.chaseSeconds() ? transition(previous, Phase.RETREATING, age) : new Memory(Phase.ATTACKING, age);
            case RETREATING -> throw new IllegalStateException("Retreat handled above");
        };
    }
    private static Memory transition(Memory previous, Phase phase, double advancedAge) {
        return new Memory(phase, previous.phase() == phase ? advancedAge : 0);
    }
}
