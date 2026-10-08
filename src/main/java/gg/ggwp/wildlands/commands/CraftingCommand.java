package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.items.SurvivalItems;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CraftingCommand {
    private final WildlandsPlugin plugin;
    public CraftingCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    public void guide(CommandSender sender) {
        sender.sendMessage("[Wildlands] Survival recipes appear in your crafting recipe book. Stations use ordinary container interfaces.");
        for (var kind : SurvivalItems.Kind.values()) sender.sendMessage("[Wildlands] " + kind.label + ": " + kind.description);
        sender.sendMessage("[Wildlands] Drying racks use charcoal, a roof and a lit campfire within three blocks. Filters use one charcoal per bottle.");
    }
    public void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.admin")) { sender.sendMessage("[Wildlands] Permission denied."); return; }
        if (args.length == 3 && args[2].equalsIgnoreCase("info")) {
            sender.sendMessage("[Wildlands] " + plugin.crafting().diagnostics()); return;
        }
        if (args.length != 5 || !args[2].equalsIgnoreCase("give")) {
            sender.sendMessage("[Wildlands] /wildlands admin crafting info | crafting give <online-player> <item>"); return;
        }
        Player player = plugin.getServer().getPlayerExact(args[3]);
        if (player == null || sender instanceof Player viewer && !viewer.canSee(player)) {
            sender.sendMessage("[Wildlands] Specify an exact visible online player."); return;
        }
        try {
            var kind = SurvivalItems.Kind.valueOf(args[4].toUpperCase(Locale.ROOT).replace('-', '_'));
            var item = plugin.crafting().items().create(kind);
            if (player.getInventory().firstEmpty() < 0) { sender.sendMessage("[Wildlands] The player's inventory is full."); return; }
            player.getInventory().addItem(item);
            sender.sendMessage("[Wildlands] Gave " + kind.label + " to " + player.getName() + ".");
        } catch (IllegalArgumentException invalid) { sender.sendMessage("[Wildlands] Unknown survival item. Use tab completion."); }
    }
}
