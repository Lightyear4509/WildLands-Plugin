package gg.ggwp.wildlands.survival;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;

/** Vanilla consumption and campfire cooking preserve client parity and bottle handling. */
public final class WaterTreatment implements Listener {
    private final WildlandsPlugin plugin;
    private final HydrationService hydration;
    private final WaterItems items = new WaterItems();
    private final NamespacedKey recipeKey = new NamespacedKey("ggwpwildlands", "boiled_water");

    public WaterTreatment(WildlandsPlugin plugin, HydrationService hydration) {
        this.plugin = plugin;
        this.hydration = hydration;
    }
    public void enable() {
        var recipe = new CampfireRecipe(recipeKey, items.bottle(WaterQuality.CLEAN),
                RecipeChoice.exactChoice(items.boilingInputs()), 0,
                plugin.configuration().hydration().boilingSeconds() * 20);
        if (!plugin.getServer().addRecipe(recipe)) throw new IllegalStateException("Could not register water boiling recipe");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    public void disable() {
        HandlerList.unregisterAll(this);
        plugin.getServer().removeRecipe(recipeKey);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        var quality = items.quality(event.getItem());
        if (quality.isEmpty()) return;
        if (hydration.record(event.getPlayer().getUniqueId()).isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[Wildlands] Your hydration data is still loading. Please try again shortly.");
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterConsume(PlayerItemConsumeEvent event) {
        items.quality(event.getItem()).ifPresent(quality -> hydration.drink(event.getPlayer(), quality));
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (event.getRecipe() == null || !event.getRecipe().getKey().equals(recipeKey)) return;
        var quality = items.quality(event.getSource());
        if (quality.isEmpty() || !quality.get().canBoil()) { event.setCancelled(true); return; }
        event.setResult(items.bottle(WaterQuality.CLEAN));
    }
}
