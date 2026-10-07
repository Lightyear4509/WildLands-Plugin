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
        hydration.trackHud(true);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                hydration.record(player.getUniqueId()).filter(record -> record.hudEnabled()).ifPresent(record ->
                    player.sendActionBar(Component.text(hudText(player, record),
                            hydrationEnabled() && record.hydration() < 25 ? NamedTextColor.YELLOW : NamedTextColor.AQUA)));
            }
        }, 40, 40);
    }
    @Override public void disable() {
        if (task != null) { task.cancel(); task = null; }
        hydration.trackHud(false);
    }
    private boolean hydrationEnabled() {
        return plugin.modules().states().get("hydration") == ModuleManager.State.ENABLED;
    }
    private String hudText(Player player, gg.ggwp.wildlands.storage.HydrationRecord record) {
        var fields = new java.util.ArrayList<String>();
        if (hydrationEnabled()) fields.add("Hydration " + Math.round(record.hydration()) + "%");
        String text = String.join(" | ", fields);
        if (plugin.environment() == null) return text;
        var environment = plugin.environment().record(player.getUniqueId());
        if (environment.isEmpty()) return text;
        var value = environment.get();
        if (plugin.modules().states().get("temperature") == ModuleManager.State.ENABLED)
            fields.add("Temp " + Math.round(value.temperature()) + "°C");
        if (plugin.modules().states().get("wetness") == ModuleManager.State.ENABLED)
            fields.add("Wet " + Math.round(value.wetness()) + "%");
        return String.join(" | ", fields);
    }
}
