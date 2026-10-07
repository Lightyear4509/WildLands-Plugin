package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.seasons.Season;
import java.util.*;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class SeasonCommand implements CommandExecutor, TabCompleter {
    private final WildlandsPlugin plugin;
    public SeasonCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    private void say(CommandSender sender, String message) { sender.sendMessage("[Wildlands] " + message); }
    private boolean ready(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) { say(sender, "Permission denied."); return false; }
        if (plugin.state() != WildlandsPlugin.State.READY || plugin.seasons() == null) {
            say(sender, "Season service is not ready."); return false;
        }
        return true;
    }
    private World world(CommandSender sender, String name) {
        if (name != null) return plugin.getServer().getWorld(name);
        if (sender instanceof Player player) return player.getWorld();
        var worlds = plugin.getServer().getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL
                        && plugin.configuration().seasons().worlds().contains(world.getName())).toList();
        return worlds.size() == 1 ? worlds.getFirst() : null;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!ready(sender, "ggwpwildlands.use")) return true;
        if (args.length > 2 || args.length > 0 && !args[0].equalsIgnoreCase("info")) {
            say(sender, "Usage: /season [info [world]]"); return true;
        }
        World world = world(sender, args.length == 2 ? args[1] : null);
        if (world == null) say(sender, "Specify a loaded world: /season info <world>");
        else say(sender, plugin.seasons().status(world));
        return true;
    }
    public void admin(CommandSender sender, String[] args) {
        if (!ready(sender, "ggwpwildlands.admin")) return;
        if (args.length < 3 || args.length > 4) { say(sender, "Usage: /wildlands admin season <season> [world]"); return; }
        Season season;
        try { season = Season.valueOf(args[2].toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException invalid) { say(sender, "Use: dry, transition-to-wet, wet, monsoon, transition-to-dry."); return; }
        World world = world(sender, args.length == 4 ? args[3] : null);
        if (world == null) { say(sender, "Specify a loaded world: /wildlands admin season <season> <world>"); return; }
        if (!plugin.seasons().set(world, season)) { say(sender, "Seasons are disabled, unmanaged, or still loading for that world."); return; }
        say(sender, "Season for " + world.getName() + " set to " + season + "; its day counter was reset.");
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.use")) return List.of();
        List<String> options = args.length == 1 ? List.of("info") : args.length == 2 && args[0].equalsIgnoreCase("info")
                ? plugin.getServer().getWorlds().stream().map(World::getName).toList() : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
