package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import org.bukkit.command.CommandSender;

public final class WorldCommand {
    private final WildlandsPlugin plugin;
    public WorldCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    public void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.admin")) { sender.sendMessage("[Wildlands] Permission denied."); return; }
        if (plugin.worlds() == null) { sender.sendMessage("[Wildlands] World service is not ready."); return; }
        if (args.length == 3 && args[2].equalsIgnoreCase("list")) {
            var worlds = plugin.worlds().records();
            if (worlds.isEmpty()) sender.sendMessage("[Wildlands] No rainforest worlds registered.");
            worlds.forEach(world -> sender.sendMessage("[Wildlands] " + world.name() + ": seed=" + world.seed()
                    + ", UUID=" + world.uuid() + ", generator=" + world.settings().version()));
        } else if (args.length == 5 && args[2].equalsIgnoreCase("create")) {
            long seed;
            try { seed = Long.parseLong(args[4]); }
            catch (NumberFormatException invalid) { sender.sendMessage("[Wildlands] Seed must be a signed 64-bit integer."); return; }
            plugin.worlds().create(args[3], seed, message -> sender.sendMessage("[Wildlands] " + message));
        } else sender.sendMessage("[Wildlands] /wildlands admin world list | world create <new-name> <seed>");
    }
}
