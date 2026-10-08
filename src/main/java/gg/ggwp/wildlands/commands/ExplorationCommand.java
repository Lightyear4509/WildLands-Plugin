package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.storage.LandmarkRecord;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class ExplorationCommand implements CommandExecutor, TabCompleter {
    private final WildlandsPlugin plugin;
    public ExplorationCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    private void say(CommandSender sender, String text) { sender.sendMessage("[Wildlands] " + text); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.state() != WildlandsPlugin.State.READY) { say(sender, "Exploration is not ready."); return true; }
        String[] expanded = new String[args.length + 1]; expanded[0] = command.getName(); System.arraycopy(args, 0, expanded, 1, args.length);
        player(sender, expanded); return true;
    }
    public void player(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.use")) { say(sender, "Permission denied."); return; }
        if (!(sender instanceof Player player)) { say(sender, "This command requires a player. Use /wildlands admin landmarks list from console."); return; }
        var service = plugin.landmarks();
        if (service == null || !service.state(player.getUniqueId()).equals("LOADED")) {
            say(sender, "Exploration is " + (service == null ? "starting" : service.state(player.getUniqueId()).toLowerCase(Locale.ROOT)) + "."); return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "landmarks" -> {
                int page = 1;
                try { if (args.length > 2) throw new NumberFormatException(); if (args.length == 2) page = Integer.parseInt(args[1]); }
                catch (NumberFormatException invalid) { say(sender, "Usage: /landmarks [page]"); return; }
                var journal = service.journal(player.getUniqueId()); int pages = Math.max(1, (journal.size() + 19) / 20);
                if (page < 1 || page > pages) { say(sender, "Choose a page from 1 to " + pages + "."); return; }
                say(sender, "Discovery journal " + page + "/" + pages + " (" + journal.size() + " waypoints)");
                journal.stream().skip((page - 1L) * 20).limit(20).forEach(record -> say(sender, record.name() + " — " + record.kind().label()));
                if (journal.isEmpty()) say(sender, "Explore rainforest landmarks to record discoveries; /wildlands camp set marks your shelter.");
            }
            case "landmark", "navigate" -> {
                if (args.length != 2) { say(sender, "Usage: /wildlands " + args[0] + " <discovered-name|camp|off>"); return; }
                if (args[0].equalsIgnoreCase("navigate") && args[1].equalsIgnoreCase("off")) {
                    service.stopTracking(player); say(sender, "Navigation stopped; a held Wildlands compass is reset."); return;
                }
                var record = service.find(player.getUniqueId(), args[1]);
                if (record.isEmpty()) { say(sender, "That waypoint is not in your journal. Use /landmarks."); return; }
                describe(player, record.get());
                if (args[0].equalsIgnoreCase("navigate") && service.track(player, record.get()))
                    say(sender, "Route tracked in the HUD. Hold a compass in your main hand when setting a route to point it at the waypoint.");
            }
            case "camp" -> {
                if (args.length == 2 && args[1].equalsIgnoreCase("set")) say(sender, service.camp(player));
                else if (args.length == 1 || args.length == 2 && args[1].equalsIgnoreCase("info"))
                    service.find(player.getUniqueId(), "camp").ifPresentOrElse(record -> describe(player, record),
                            () -> say(sender, "No camp waypoint. Build a dry roofed shelter beside a lit campfire, then /wildlands camp set."));
                else say(sender, "Usage: /wildlands camp [set|info]");
            }
            case "expedition" -> {
                var record = service.player(player.getUniqueId()).orElseThrow();
                say(sender, "Trail knowledge: " + record.rank().name().toLowerCase(Locale.ROOT) + "; completed expeditions: " + record.completedTrips()
                        + "; farthest from camp: " + Math.round(record.longestDistance()) + "m" + (record.active() ? "; currently away from camp" : ""));
                say(sender, "Prepare clean water, preserved food, a cloak, a compass and shelter supplies. Depart at " + plugin.configuration().landmarks().departAt()
                        + "m and return within " + plugin.configuration().landmarks().returnAt() + "m of your saved camp to record an expedition.");
            }
            default -> say(sender, "Use /landmarks, /landmark <name>, /wildlands navigate <name>, camp or expedition.");
        }
    }
    private void describe(Player player, LandmarkRecord record) {
        var world = plugin.getServer().getWorld(record.worldUuid());
        String coordinates = plugin.configuration().landmarks().showCoordinates() ? " at " + record.x() + ", " + record.y() + ", " + record.z() : "";
        say(player, record.name() + " — " + record.kind().label() + " in " + (world == null ? record.worldUuid() : world.getName()) + coordinates);
        say(player, plugin.landmarks().route(player, record));
    }
    public void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ggwpwildlands.admin")) { say(sender, "Permission denied."); return; }
        var service = plugin.landmarks(); if (service == null) { say(sender, "Exploration is not ready."); return; }
        if (args.length >= 3 && args.length <= 4 && args[2].equalsIgnoreCase("list")) {
            say(sender, service.diagnostics()); UUID worldId = null;
            if (args.length == 4) {
                var world = plugin.getServer().getWorld(args[3]); if (world == null) { say(sender, "Specify a loaded world."); return; } worldId = world.getUID();
            }
            UUID filter = worldId;
            var records = service.records().stream().filter(record -> filter == null || record.worldUuid().equals(filter)).toList();
            records.stream().limit(20).forEach(record -> say(sender, record.name() + "; UUID " + record.id() + "; " + record.worldUuid() + "; " + record.x() + "," + record.y() + "," + record.z()));
            if (records.size() > 20) say(sender, "Showing 20 of " + records.size() + " records; filter by world.");
        } else if (args.length == 4 && args[2].equalsIgnoreCase("debug")) {
            UUID id; try { id = UUID.fromString(args[3]); } catch (IllegalArgumentException invalid) { say(sender, "Use a player UUID."); return; }
            plugin.storage().submit(() -> "UUID " + id + "; discoveries: " + plugin.storage().findDiscoveries(id).size()
                    + "; expedition: " + plugin.storage().findExpedition(id).map(Object::toString).orElse("none"))
                    .whenComplete((text, failure) -> plugin.onMain(() -> {
                        if (failure != null) { plugin.report("Exploration debug lookup failed", failure); say(sender, "Lookup failed; see server log."); }
                        else say(sender, text);
                    }));
        } else say(sender, "Usage: /wildlands admin landmarks list [world] | landmarks debug <player-uuid>");
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.state() != WildlandsPlugin.State.READY || !sender.hasPermission("ggwpwildlands.use") || !(sender instanceof Player player)
                || plugin.landmarks() == null || args.length != 1 || !command.getName().equalsIgnoreCase("landmark")) return List.of();
        return plugin.landmarks().journal(player.getUniqueId()).stream().map(LandmarkRecord::name)
                .filter(name -> name.startsWith(args[0].toLowerCase(Locale.ROOT))).limit(64).toList();
    }
}
