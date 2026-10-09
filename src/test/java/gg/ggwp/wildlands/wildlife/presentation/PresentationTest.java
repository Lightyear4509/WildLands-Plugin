package gg.ggwp.wildlands.wildlife.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.config.VisualSettings;
import gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Phase;
import java.util.UUID;
import org.junit.jupiter.api.*;

class PresentationTest {
    @AfterEach void cleanup() { PresentationRegistry.clear(); }
    @Test void registryRequiresBothAnOwnedAnimalAndExplicitBedrockOptIn() {
        UUID animal = UUID.randomUUID(), viewer = UUID.randomUUID(), other = UUID.randomUUID();
        PresentationRegistry.publish(animal, Phase.WARNING.ordinal());
        assertEquals(-1, PresentationRegistry.phase(animal, viewer));
        PresentationRegistry.viewer(viewer, true);
        assertEquals(2, PresentationRegistry.phase(animal, viewer));
        assertEquals(-1, PresentationRegistry.phase(other, viewer));
        PresentationRegistry.viewer(viewer, false); assertEquals(-1, PresentationRegistry.phase(animal, viewer));
        PresentationRegistry.viewer(viewer, true); PresentationRegistry.remove(animal);
        assertEquals(-1, PresentationRegistry.phase(animal, viewer));
    }
    @Test void locomotionIsBoundedAndStandingLegsDoNotMove() {
        for (Phase phase : Phase.values()) for (int bone = 0; bone < 4; bone++) for (int tick = 0; tick < 1000; tick++) {
            assertEquals(0, JaguarAnimation.leg(phase, tick / 20.0, false, bone), .00001);
            assertTrue(Math.abs(JaguarAnimation.leg(phase, tick / 20.0, true, bone)) <= .42001);
        }
        assertEquals(-.08f, JaguarAnimation.crouch(Phase.STALKING));
        assertEquals(0, JaguarAnimation.crouch(Phase.IDLE));
    }
    @Test void packSettingsRejectMalformedHashesCredentialsAndUnboundedDisplayCounts() {
        assertDoesNotThrow(VisualSettings::disabled);
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "http://example.com/a.zip", "a".repeat(40), 32));
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "https://user:password@example.com/a.zip", "a".repeat(40), 32));
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "https://example.com/a.zip", "wrong", 32));
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "", "a".repeat(40), 32));
        assertThrows(IllegalArgumentException.class, () -> new VisualSettings(true, "", "", 65));
    }
}
