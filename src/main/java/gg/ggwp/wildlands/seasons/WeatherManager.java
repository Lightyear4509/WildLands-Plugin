package gg.ggwp.wildlands.seasons;

import gg.ggwp.wildlands.config.SeasonSettings;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.World;

/** Only called for loaded, configured Overworlds on the server thread. */
public final class WeatherManager {
    public record Snapshot(boolean rain, boolean thunder, int weatherTicks, int thunderTicks, int clearTicks) {
        public static Snapshot capture(World world) {
            return new Snapshot(world.hasStorm(), world.isThundering(), world.getWeatherDuration(),
                    world.getThunderDuration(), world.getClearWeatherDuration());
        }
        public void restore(World world) {
            world.setStorm(rain); world.setThundering(thunder);
            world.setWeatherDuration(weatherTicks); world.setThunderDuration(thunderTicks);
            world.setClearWeatherDuration(clearTicks);
        }
    }
    public void evaluate(World world, SeasonSettings.Profile profile, int seconds) {
        var random = ThreadLocalRandom.current();
        SeasonRules.Weather result = SeasonRules.weather(profile, random.nextDouble(), random.nextDouble());
        int ticks = Math.multiplyExact(seconds, 20);
        world.setClearWeatherDuration(result.rain() ? 0 : ticks);
        world.setStorm(result.rain());
        world.setThundering(result.thunder());
        world.setWeatherDuration(ticks); world.setThunderDuration(ticks);
    }
}
