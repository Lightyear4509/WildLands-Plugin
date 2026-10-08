package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public final class WildlifeCommand {
    private final WildlandsPlugin plugin;
    public WildlifeCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    public void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.admin")) { sender.sendMessage("[Wildlands] Permission denied."); return; }
        if (plugin.wildlife() == null) { sender.sendMessage("[Wildlands] Wildlife service is not ready."); return; }
        if (args.length == 3 && args[2].equalsIgnoreCase("list"))
            plugin.wildlife().diagnostics().forEach(line -> sender.sendMessage("[Wildlands] " + line));
        else if (args.length == 7 && args[2].equalsIgnoreCase("spawn")) {
            var world = plugin.getServer().getWorld(args[3]);
            if (world == null) { sender.sendMessage("[Wildlands] Specify a loaded world."); return; }
            try {
                double x = Double.parseDouble(args[4]), y = Double.parseDouble(args[5]), z = Double.parseDouble(args[6]);
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000)
                    throw new IllegalArgumentException("Invalid coordinates");
                var spawned = plugin.wildlife().spawn(new Location(world, x, y, z));
                sender.sendMessage(spawned.map(id -> "[Wildlands] Jaguar spawned: " + id)
                        .orElse("[Wildlands] Spawn refused: disabled/unmanaged world, unloaded/unsafe location, occupied territory or population cap."));
            } catch (IllegalArgumentException invalid) { sender.sendMessage("[Wildlands] Use finite coordinates within the world border."); }
        } else sender.sendMessage("[Wildlands] /wildlands admin wildlife list | wildlife spawn <world> <x> <y> <z>");
    }
}
