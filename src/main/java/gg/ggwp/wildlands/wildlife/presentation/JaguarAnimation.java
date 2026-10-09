package gg.ggwp.wildlands.wildlife.presentation;

import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Phase;

/** Shared state order matches Bedrock's ggwpwildlands:phase integer property. */
public final class JaguarAnimation {
    private JaguarAnimation() { }
    public static float leg(Phase phase, double seconds, boolean moving, int index) {
        double amplitude = moving ? (phase == Phase.STALKING ? .15 : .42) : 0;
        double speed = phase == Phase.ATTACKING || phase == Phase.RETREATING ? 11 : 6;
        return (float) (Math.sin(seconds * speed + (index == 0 || index == 3 ? 0 : Math.PI)) * amplitude);
    }
    public static float crouch(Phase phase) { return phase == Phase.STALKING ? -.08f : 0; }
}
