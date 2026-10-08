package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.items.SurvivalItems;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/** Vanilla hunger remains authoritative. Preserved food adds only a modest recovery reserve. */
public final class NutritionService implements WildlandsModule, Listener {
    private final WildlandsPlugin plugin;
    private final SurvivalItems items;
    public NutritionService(WildlandsPlugin plugin, SurvivalItems items) { this.plugin = plugin; this.items = items; }
    @Override public String id() { return "nutrition"; }
    @Override public void enable() { plugin.getServer().getPluginManager().registerEvents(this, plugin); }
    @Override public void disable() { HandlerList.unregisterAll(this); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!items.preserved(event.getItem())) return;
        var id = event.getPlayer().getUniqueId();
        double bonus = plugin.configuration().food().preservedSaturation();
        // Consumption applies native food stats after the event. Apply the bonus next tick.
        plugin.onMain(() -> {
            var player = plugin.getServer().getPlayer(id);
            if (player != null && !player.isDead() && plugin.modules().states().get(id()) == ModuleManager.State.ENABLED)
                player.setSaturation((float) Math.min(player.getFoodLevel(), player.getSaturation() + bonus));
        });
    }
}
