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
        else if ((args.length == 7 || args.length == 8) && args[2].equalsIgnoreCase("spawn")) {
            int offset = args.length == 8 ? 1 : 0;
            gg.ggwp.wildlands.wildlife.Species species = gg.ggwp.wildlands.wildlife.Species.JAGUAR;
            if (offset == 1) {
                try { species = gg.ggwp.wildlands.wildlife.Species.valueOf(args[3].toUpperCase(java.util.Locale.ROOT)); }
                catch (IllegalArgumentException invalid) { sender.sendMessage("[Wildlands] Species: " + java.util.Arrays.stream(gg.ggwp.wildlands.wildlife.Species.values()).map(gg.ggwp.wildlands.wildlife.Species::key).collect(java.util.stream.Collectors.joining(", ")) + "."); return; }
            }
            var world = plugin.getServer().getWorld(args[3 + offset]);
            if (world == null) { sender.sendMessage("[Wildlands] Specify a loaded world."); return; }
            try {
                double x = Double.parseDouble(args[4 + offset]), y = Double.parseDouble(args[5 + offset]), z = Double.parseDouble(args[6 + offset]);
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000)
                    throw new IllegalArgumentException("Invalid coordinates");
                var spawned = offset == 0 ? plugin.wildlife().spawn(new Location(world, x, y, z)) : plugin.wildlife().spawn(new Location(world, x, y, z), species);
                String display = species.displayName();
                sender.sendMessage(spawned.map(id -> "[Wildlands] " + display + " spawned: " + id)
                        .orElse("[Wildlands] Spawn refused: disabled/unmanaged world, unloaded/unsafe location, occupied territory or population cap."));
            } catch (IllegalArgumentException invalid) { sender.sendMessage("[Wildlands] Use finite coordinates within the world border."); }
        } else sender.sendMessage("[Wildlands] /wildlands admin wildlife list | wildlife spawn [species] <world> <x> <y> <z>");
    }
}
