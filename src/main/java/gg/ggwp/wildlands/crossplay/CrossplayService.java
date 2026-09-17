package gg.ggwp.wildlands.crossplay;

import java.util.UUID;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrossplayService implements PlatformAdapter, Listener {
    private final JavaPlugin plugin;
    private final GeyserBridge geyser;
    private final FloodgateBridge floodgate;
    public CrossplayService(JavaPlugin plugin) {
        this.plugin = plugin;
        geyser = new GeyserBridge(plugin.getLogger());
        floodgate = new FloodgateBridge(plugin.getLogger());
    }
    public void refresh() {
        geyser.refresh(plugin.getServer().getPluginManager());
        floodgate.refresh(plugin.getServer().getPluginManager());
    }
    @EventHandler public void enabled(PluginEnableEvent event) { if (relevant(event.getPlugin().getName())) refresh(); }
    @EventHandler public void disabled(PluginDisableEvent event) {
        if (relevant(event.getPlugin().getName()) && plugin.isEnabled())
            plugin.getServer().getScheduler().runTask(plugin, this::refresh);
    }
    private boolean relevant(String name) { return name.equals("Geyser-Spigot") || name.equals("floodgate"); }
    @Override public Platform platform(UUID uuid) {
        return floodgate.contains(uuid) || geyser.contains(uuid) ? Platform.BEDROCK : Platform.JAVA_OR_UNDETECTED;
    }
    public String geyserStatus() { return geyser.status(); }
    public String floodgateStatus() { return floodgate.status(); }
}
