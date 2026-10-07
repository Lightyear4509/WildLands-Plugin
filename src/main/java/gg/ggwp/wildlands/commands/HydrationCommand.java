package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/** Hydration command handling shared by /hydration and /wildlands. */
public final class HydrationCommand implements CommandExecutor, TabCompleter {
    private final WildlandsPlugin plugin;
    public HydrationCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    private void say(CommandSender sender, String message) { sender.sendMessage("[Wildlands] " + message); }
    private boolean ready(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) { say(sender, "Permission denied."); return false; }
        if (plugin.state() != WildlandsPlugin.State.READY) { say(sender, "Wildlands is still starting or stopping."); return false; }
        return true;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) { say(sender, "Usage: /hydration"); return true; }
        status(sender);
        return true;
    }
    public void status(CommandSender sender) {
        if (!ready(sender, "ggwpwildlands.use")) return;
        if (!(sender instanceof Player player)) { say(sender, "Use this command in-game."); return; }
        if (plugin.modules().states().get("hydration") != gg.ggwp.wildlands.core.ModuleManager.State.ENABLED) {
            say(sender, "Hydration: disabled"); return;
        }
        var record = plugin.hydration().record(player.getUniqueId());
        if (record.isEmpty()) { say(sender, "Hydration: " + plugin.hydration().state(player.getUniqueId())); return; }
        say(sender, "Hydration: " + String.format(Locale.ROOT, "%.1f", record.get().hydration())
                + "% | HUD preference: " + (record.get().hudEnabled() ? "on" : "off"));
    }
    public void hud(CommandSender sender, String[] args) {
        if (!ready(sender, "ggwpwildlands.use")) return;
        if (!(sender instanceof Player player)) { say(sender, "Use this command in-game."); return; }
        if (args.length != 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            say(sender, "Usage: /wildlands hud on|off"); return;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        if (!plugin.hydration().hud(player.getUniqueId(), enabled)) {
            say(sender, "HUD data is unavailable; enable the hud module and wait for your data to load."); return;
        }
        if (!enabled) player.sendActionBar(net.kyori.adventure.text.Component.empty());
        say(sender, "HUD preference saved: " + (enabled ? "on" : "off") + ". The hud module must also be enabled.");
    }
    public void admin(CommandSender sender, String[] args) {
        if (!ready(sender, "ggwpwildlands.admin")) return;
        if (args.length != 4) { say(sender, "Usage: /wildlands admin hydration <online-player|uuid> <0..100>"); return; }
        double value;
        try { value = Double.parseDouble(args[3]); }
        catch (NumberFormatException invalid) { say(sender, "Hydration must be a number from 0 to 100."); return; }
        if (!Double.isFinite(value) || value < 0 || value > 100) { say(sender, "Hydration must be a finite number from 0 to 100."); return; }
        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            try { target = plugin.getServer().getPlayer(UUID.fromString(args[2])); }
            catch (IllegalArgumentException ignored) { /* Names never become persistent identifiers. */ }
        }
        if (target == null || sender instanceof Player viewer && !viewer.canSee(target)) {
            say(sender, "Player must be online and visible."); return;
        }
        if (!plugin.hydration().set(target.getUniqueId(), value)) {
            say(sender, "Hydration is disabled or the player's data has not loaded yet."); return;
        }
        say(sender, "Hydration for " + target.getName() + " set to " + value + "%.");
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
