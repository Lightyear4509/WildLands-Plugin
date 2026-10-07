package gg.ggwp.wildlands.survival;

import org.bukkit.entity.Player;

record WaterExposure(boolean rain, boolean immersed) {
    static WaterExposure sample(Player player) {
        return new WaterExposure(player.isInRain(), player.isInWaterOrBubbleColumn());
    }
}
