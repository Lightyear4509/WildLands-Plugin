package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.SeasonSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeasonRulesTest {
    @Test void thunderRequiresRainAndProbabilityEndpointsWork() {
        var dry = new SeasonSettings.Profile(14, 3, 0, 1, .8);
        assertEquals(new SeasonRules.Weather(false, false), SeasonRules.weather(dry, 0, 0));
        var monsoon = new SeasonSettings.Profile(5, -2, 1, 1, 1.1);
        assertEquals(new SeasonRules.Weather(true, true), SeasonRules.weather(monsoon, .999, .999));
        var wet = new SeasonSettings.Profile(14, 0, .65, .2, 1.2);
        assertEquals(new SeasonRules.Weather(true, false), SeasonRules.weather(wet, .4, .2));
        assertEquals(new SeasonRules.Weather(false, false), SeasonRules.weather(wet, .65, 0));
    }
    @Test void cropModifiersPreserveVanillaGrowthAndNeverExceedMaturity() {
        assertEquals(3, SeasonRules.cropAge(3, 7, 1, .9));
        assertEquals(-1, SeasonRules.cropAge(3, 7, .8, .8));
        assertEquals(3, SeasonRules.cropAge(3, 7, .8, .79));
        assertEquals(4, SeasonRules.cropAge(3, 7, 1.2, .1));
        assertEquals(3, SeasonRules.cropAge(3, 7, 1.2, .4));
        assertEquals(7, SeasonRules.cropAge(7, 7, 2, 0));
        assertEquals(-1, SeasonRules.cropAge(1, 7, 0, 0));
    }
    @Test void invalidRollsAndCropStatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SeasonRules.cropAge(8, 7, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> SeasonRules.cropAge(1, 7, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> SeasonRules.cropAge(1, 7, 1, 1));
    }
}
