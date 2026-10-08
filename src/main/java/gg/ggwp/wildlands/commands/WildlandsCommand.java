package gg.ggwp.wildlands.commands;

import gg.ggwp.wildlands.core.*;
import java.time.Instant;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class WildlandsCommand implements CommandExecutor, TabCompleter {
    private static final String USE = "ggwpwildlands.use";
    private static final String ADMIN = "ggwpwildlands.admin";
    private static final String DEBUG = "ggwpwildlands.admin.debug";
    private final WildlandsPlugin plugin;

    public WildlandsCommand(WildlandsPlugin plugin) { this.plugin = plugin; }
    private void say(CommandSender sender, String text) { sender.sendMessage((plugin.configuration() == null ? "[Wildlands] " : plugin.configuration().messages().get("prefix")) + text); }
    private boolean permitted(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(plugin.configuration().message("no-permission"));
        return false;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.state() != WildlandsPlugin.State.READY) {
            say(sender, "Foundation is " + plugin.state().name().toLowerCase(Locale.ROOT) + ".");
            return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "help" -> {
                if (!permitted(sender, USE)) return true;
                say(sender, "/wildlands status | hud on|off | help; /hydration; /season info");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands reload | admin modules | admin hydration <player> <0..100> | admin temperature <player>");
                if (sender.hasPermission(DEBUG)) say(sender, "/wildlands admin health | debug [online-player|uuid]");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands admin season <season> [world]");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands admin world list | world create <new-name> <seed>");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands admin wildlife list | wildlife spawn <world> <x> <y> <z>");
                say(sender, "Collect and treat water to maintain hydration.");
                say(sender, "/wildlands crafting — survival equipment and station guide");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands admin crafting info | crafting give <player> <item>");
                say(sender, "/landmarks [page] | /landmark <name|camp> | /wildlands navigate <name|camp|off> | camp [set|info] | expedition");
                if (sender.hasPermission(ADMIN)) say(sender, "/wildlands admin landmarks list [world] | landmarks debug <player-uuid>");
            }
            case "status" -> {
                if (!permitted(sender, USE)) return true;
                say(sender, "GGWP Wildlands " + plugin.getPluginMeta().getVersion() + " — foundation ready.");
                if (sender instanceof Player player) {
                    say(sender, "UUID: " + player.getUniqueId() + "; platform: " + plugin.crossplay().platform(player.getUniqueId()));
                    say(sender, "Record: " + plugin.players().state(player.getUniqueId()));
                    if (plugin.hydration() != null) new HydrationCommand(plugin).status(sender);
                    if (plugin.environment() != null) say(sender, plugin.environment().status(player));
                    if (plugin.seasons() != null) say(sender, plugin.seasons().status(player.getWorld()));
                    if (plugin.worlds() != null) plugin.worlds().region(player.getLocation()).ifPresent(region -> say(sender, "Region: " + region));
                }
            }
            case "hud" -> new HydrationCommand(plugin).hud(sender, args);
            case "crafting" -> { if (permitted(sender, USE)) new CraftingCommand(plugin).guide(sender); }
            case "landmarks", "landmark", "navigate", "camp", "expedition" -> new ExplorationCommand(plugin).player(sender, args);
            case "reload" -> {
                if (permitted(sender, ADMIN)) {
                    if (args.length != 1) say(sender, "Usage: /wildlands reload");
                    else plugin.reload(message -> say(sender, message));
                }
            }
            case "admin" -> admin(sender, args);
            default -> sender.sendMessage(plugin.configuration().message("unknown-command"));
        }
        return true;
    }
    private void admin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (permitted(sender, ADMIN)) say(sender, "Usage: /wildlands admin modules | debug [online-player|uuid]");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "crafting" -> new CraftingCommand(plugin).admin(sender, args);
            case "landmarks" -> new ExplorationCommand(plugin).admin(sender, args);
            case "wildlife" -> new WildlifeCommand(plugin).admin(sender, args);
            case "world" -> new WorldCommand(plugin).admin(sender, args);
            case "season" -> new SeasonCommand(plugin).admin(sender, args);
            case "hydration" -> new HydrationCommand(plugin).admin(sender, args);
            case "temperature" -> environment(sender, args);
            case "modules" -> {
                if (permitted(sender, ADMIN))
                    plugin.modules().states().forEach((id, state) -> say(sender, id + ": " + state));
            }
            case "health", "debug" -> {
                if (!permitted(sender, DEBUG)) return;
                if (!plugin.configuration().settings().debugEnabled()) { say(sender, "Debug is disabled in config.yml."); return; }
                if (args.length > 3 || args[1].equalsIgnoreCase("health") && args.length != 2) { say(sender, "Usage: /wildlands admin health | debug [online-player|uuid]"); return; }
                if (args.length == 2) { debug(sender); return; }
                Player player = plugin.getServer().getPlayerExact(args[2]);
                UUID uuid;
                if (player != null) uuid = player.getUniqueId();
                else {
                    try { uuid = UUID.fromString(args[2]); }
                    catch (IllegalArgumentException invalid) {
                        say(sender, "Use an exact online name or a UUID. Offline names are not identifiers.");
                        return;
                    }
                }
                say(sender, "UUID: " + uuid + "; session: " + plugin.players().state(uuid));
                if (plugin.getServer().getPlayer(uuid) != null)
                    say(sender, "Platform: " + plugin.crossplay().platform(uuid));
                plugin.storage().submit(() -> plugin.storage().find(uuid)).whenComplete((record, error) -> plugin.onMain(() -> {
                    if (error != null) { plugin.report("Debug record lookup failed", error); say(sender, "Record lookup failed; see server log."); }
                    else if (record.isEmpty()) say(sender, "No persisted record.");
                    else {
                        var value = record.get();
                        say(sender, "Stored name: " + value.lastKnownName() + "; first seen: " + Instant.ofEpochMilli(value.firstSeen())
                                + "; last seen: " + Instant.ofEpochMilli(value.lastSeen()));
                    }
                }));
            }
            default -> {
                if (permitted(sender, ADMIN)) say(sender, "Unknown admin command. Use /wildlands help.");
            }
        }
    }
    private void debug(CommandSender sender) {
        say(sender, "Plugin: " + plugin.getPluginMeta().getVersion() + "; server: " + plugin.getServer().getVersion()
                + "; Java: " + Runtime.version());
        say(sender, "State: " + plugin.state() + "; sessions: " + plugin.players().size());
        say(sender, "SQLite: " + plugin.storage().health() + "; queued jobs: " + plugin.storage().queueSize()
                + "; pending records: " + plugin.storage().pendingCount());
        long last = plugin.storage().lastSuccessfulSave();
        say(sender, "Last successful save: " + (last == 0 ? "none this session" : Instant.ofEpochMilli(last)));
        say(sender, "Geyser: " + plugin.crossplay().geyserStatus() + "; Floodgate: " + plugin.crossplay().floodgateStatus());
        if (plugin.crafting() != null) say(sender, plugin.crafting().diagnostics());
        if (plugin.landmarks() != null) say(sender, plugin.landmarks().diagnostics());
        if (plugin.wildlife() != null) plugin.wildlife().diagnostics().stream().limit(1).forEach(line -> say(sender, line));
        if (plugin.worlds() != null) say(sender, "Registered rainforest worlds: " + plugin.worlds().records().size());
        double[] tps = plugin.getServer().getTPS();
        if (tps != null && tps.length > 0) say(sender, String.format(Locale.ROOT,
                "Server: %.2f TPS (1m); %.2f ms average tick", Math.min(20, tps[0]), plugin.getServer().getAverageTickTime()));
        plugin.modules().states().forEach((id, state) -> say(sender, id + ": " + state));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.state() != WildlandsPlugin.State.READY) return List.of();
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission(USE)) options.addAll(List.of("help", "status", "hud", "crafting", "landmarks", "landmark", "navigate", "camp", "expedition"));
            if (sender.hasPermission(ADMIN)) options.add("reload");
            if (sender.hasPermission(ADMIN) || sender.hasPermission(DEBUG)) options.add("admin");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("hud") && sender.hasPermission(USE)) {
            options.addAll(List.of("on", "off"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission(ADMIN)) options.addAll(List.of("modules", "hydration", "temperature", "season", "world", "wildlife", "crafting", "landmarks"));
            if (sender.hasPermission(DEBUG)) options.addAll(List.of("debug", "health"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && ((args[1].equalsIgnoreCase("debug") && sender.hasPermission(DEBUG))
                || ((args[1].equalsIgnoreCase("hydration") || args[1].equalsIgnoreCase("temperature")) && sender.hasPermission(ADMIN)))) {
            for (Player player : plugin.getServer().getOnlinePlayers())
                if (!(sender instanceof Player viewer) || viewer.canSee(player)) options.add(player.getName());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("season") && sender.hasPermission(ADMIN))
            for (var season : gg.ggwp.wildlands.seasons.Season.values()) options.add(season.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("season") && sender.hasPermission(ADMIN))
            plugin.getServer().getWorlds().forEach(world -> options.add(world.getName()));
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("world") && sender.hasPermission(ADMIN))
            options.addAll(List.of("list", "create"));
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("wildlife") && sender.hasPermission(ADMIN))
            options.addAll(List.of("list", "spawn"));
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("wildlife") && sender.hasPermission(ADMIN))
            plugin.getServer().getWorlds().forEach(world -> options.add(world.getName()));
        if (args.length >= 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("crafting") && sender.hasPermission(ADMIN)) {
            if (args.length == 3) options.addAll(List.of("info", "give"));
            if (args.length == 4 && args[2].equalsIgnoreCase("give")) for (Player player : plugin.getServer().getOnlinePlayers())
                if (!(sender instanceof Player viewer) || viewer.canSee(player)) options.add(player.getName());
            if (args.length == 5 && args[2].equalsIgnoreCase("give")) for (var kind : gg.ggwp.wildlands.items.SurvivalItems.Kind.values())
                options.add(kind.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        }
        if (args.length == 2 && sender.hasPermission(USE)) {
            if (args[0].equalsIgnoreCase("camp")) options.addAll(List.of("set", "info"));
            if (args[0].equalsIgnoreCase("navigate")) options.add("off");
            if ((args[0].equalsIgnoreCase("navigate") || args[0].equalsIgnoreCase("landmark")) && sender instanceof Player player && plugin.landmarks() != null)
                plugin.landmarks().journal(player.getUniqueId()).stream().limit(64).forEach(record -> options.add(record.name()));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("landmarks") && sender.hasPermission(ADMIN))
            options.addAll(List.of("list", "debug"));
        String partial = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(partial)).sorted().toList();
    }
    private void environment(CommandSender sender, String[] args) {
        if (!permitted(sender, ADMIN)) return;
        if (args.length != 3) { say(sender, "Usage: /wildlands admin temperature <online-player>"); return; }
        Player player = plugin.getServer().getPlayerExact(args[2]);
        if (player == null || sender instanceof Player viewer && !viewer.canSee(player)) {
            say(sender, "Use an exact visible online player name."); return;
        }
        if (plugin.environment() == null) { say(sender, "Environment module is unavailable."); return; }
        say(sender, player.getName() + " — " + plugin.environment().status(player));
    }
}
