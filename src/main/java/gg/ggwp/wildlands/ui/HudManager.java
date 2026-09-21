package gg.ggwp.wildlands.ui;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.survival.HydrationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Plain action-bar text works without either edition installing a resource pack. */
public final class HudManager implements WildlandsModule {
    private final WildlandsPlugin plugin;
    private final HydrationService hydration;
    private BukkitTask task;
    public HudManager(WildlandsPlugin plugin, HydrationService hydration) {
        this.plugin = plugin;
        this.hydration = hydration;
    }
    @Override public String id() { return "hud"; }
    @Override public void enable() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                hydration.record(player.getUniqueId()).filter(record -> record.hudEnabled()).ifPresent(record ->
                    player.sendActionBar(Component.text("Hydration " + Math.round(record.hydration()) + "%",
                            record.hydration() < 25 ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
            }
        }, 40, 40);
    }
    @Override public void disable() {
        if (task != null) { task.cancel(); task = null; }
    }
}
