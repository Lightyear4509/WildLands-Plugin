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
        var environment = plugin.environment() == null ? java.util.Optional.<gg.ggwp.wildlands.storage.EnvironmentRecord>empty()
                : plugin.environment().record(player.getUniqueId());
        var states = plugin.modules().states();
        Double temperature = states.get("temperature") == ModuleManager.State.ENABLED
                ? environment.map(gg.ggwp.wildlands.storage.EnvironmentRecord::temperature).orElse(null) : null;
        Double wetness = states.get("wetness") == ModuleManager.State.ENABLED
                ? environment.map(gg.ggwp.wildlands.storage.EnvironmentRecord::wetness).orElse(null) : null;
        var season = plugin.seasons() == null ? null : plugin.seasons().state(player.getWorld()).map(state -> state.season()).orElse(null);
        var route = plugin.landmarks() == null ? null : plugin.landmarks().navigation(player).orElse(null);
        return HudFormatter.format(hydrationEnabled() ? record.hydration() : null, temperature, wetness, season, route);
    }
}
