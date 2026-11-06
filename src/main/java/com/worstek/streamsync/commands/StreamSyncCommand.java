package com.worstek.streamsync.commands;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.queue.StreamEvent;
import com.worstek.streamsync.utils.ColorUtil;
import com.worstek.streamsync.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class StreamSyncCommand implements CommandExecutor, TabCompleter {

    private final StreamSync plugin;

    public StreamSyncCommand(StreamSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("streamsync.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission", "&#FC4E4E● You do not have permission to use StreamSync."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "connect": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &fUsage: &#74ACF6/ss connect <tiktok_username>"));
                    return true;
                }
                String cleanUser = com.worstek.streamsync.platforms.tiktok.TikTokConnector.sanitizeUsername(args[1]);
                if (cleanUser.isEmpty()) {
                    sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Invalid TikTok username: &#74ACF6" + args[1]));
                    return true;
                }
                sender.sendMessage(plugin.getConfigManager().getMessage("connecting",
                        "%prefix% &fConnecting to TikTok: &#74ACF6@%user%").replace("%user%", cleanUser));
                plugin.getTikTokConnector().connect(cleanUser, true);
                return true;
            }

            case "disconnect": {
                if (!plugin.getTikTokConnector().isConnected()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("not-connected",
                            "%prefix% &fNot connected to any TikTok live."));
                    return true;
                }
                plugin.getTikTokConnector().disconnect();
                return true;
            }

            case "streamer": {
                handleStreamer(sender, args);
                return true;
            }

            case "settings": {
                handleSettings(sender, args);
                return true;
            }

            case "status": {
                List<String> streamers = plugin.getQueueManager().getStreamerNames();
                String streamerText;
                if (streamers.isEmpty()) {
                    Player auto = plugin.getQueueManager().getTargetStreamer();
                    streamerText = (auto != null) ? "&7Auto: &#74ACF6" + auto.getName() + " &a[ONLINE]" : "&cNone";
                } else {
                    List<String> formatted = new ArrayList<>();
                    for (String name : streamers) {
                        Player p = Bukkit.getPlayerExact(name);
                        boolean isOnline = (p != null && p.isOnline());
                        formatted.add("&#74ACF6" + name + " " + (isOnline ? "&a[ONLINE]" : "&7[OFFLINE]"));
                    }
                    streamerText = String.join("&7, ", formatted);
                }

                boolean tt = plugin.getTikTokConnector().isConnected();

                sender.sendMessage(ColorUtil.colorize(
                        "&8&m----------------------------------------\n" +
                        "&#74ACF6● &fStreamSync Status:\n" +
                        "  &7Active Streamers: " + streamerText + "\n" +
                        "  &7TikTok Live: " + (tt ? "&aONLINE (@" + plugin.getTikTokConnector().getCurrentUsername() + ")" : "&#FC4E4EOFFLINE / IDLE") + "\n" +
                        "&8&m----------------------------------------"));
                return true;
            }

            case "clear": {
                plugin.getQueueManager().clearQueue();
                sender.sendMessage(plugin.getConfigManager().getMessage("queue-cleared", "&#74ACF6● &fEvent queue has been cleared."));
                return true;
            }

            case "goal": {
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fThe goal command has moved to: &#74ACF6/ss settings goal"));
                return true;
            }

            case "reload": {
                plugin.getConfigManager().reloadConfigs();
                plugin.getQueueManager().start();
                plugin.getWebhookServer().start();
                plugin.getTikTokConnector().start();
                sender.sendMessage(plugin.getConfigManager().getMessage("reloaded", "&#74ACF6● &fConfigurations reloaded successfully!"));
                return true;
            }

            case "test": {
                handleTest(sender, args);
                return true;
            }

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void handleStreamer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendStreamerHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss streamer add <player>"));
                    return;
                }
                String name = args[2];
                boolean added = plugin.getQueueManager().addStreamer(name);
                if (added) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("streamer-added",
                            "%prefix% &fStreamer &#74ACF6%player% &fhas been added!").replace("%player%", name));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("streamer-already-added",
                            "%prefix% &#FC4E4EStreamer &#74ACF6%player% &cis already in the active list!").replace("%player%", name));
                }
                break;
            }

            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss streamer remove <player>"));
                    return;
                }
                String name = args[2];
                boolean removed = plugin.getQueueManager().removeStreamer(name);
                if (removed) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("streamer-removed",
                            "%prefix% &fStreamer &#74ACF6%player% &fhas been removed!").replace("%player%", name));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("streamer-not-found",
                            "%prefix% &#FC4E4EStreamer &#74ACF6%player% &cnot found in the active list!").replace("%player%", name));
                }
                break;
            }

            case "list": {
                List<String> list = plugin.getQueueManager().getStreamerNames();
                if (list.isEmpty()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("streamer-list-empty",
                            "%prefix% &7No streamers configured. Auto-targeting is active."));
                    return;
                }
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fConfigured Streamers: &#74ACF6" + String.join("&7, &#74ACF6", list)));
                break;
            }

            default:
                sendStreamerHelp(sender);
                break;
        }
    }

    private void sendStreamerHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize(
                "&#74ACF6● &fStreamer Commands:\n" +
                "  &#74ACF6/ss streamer add <player> &7- Add player to streamers list\n" +
                "  &#74ACF6/ss streamer remove <player> &7- Remove player from streamers list\n" +
                "  &#74ACF6/ss streamer list &7- View all active streamers"));
    }

    private void handleSettings(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize(
                    "&8&m----------------------------------------\n" +
                    "&#74ACF6● &fStreamSync In-Game Settings:\n" +
                    "  &#74ACF6/ss settings actions &7- Manage triggers and actions\n" +
                    "  &#74ACF6/ss settings config &7- View and toggle config values\n" +
                    "  &#74ACF6/ss settings goal &7- Configure stream challenges\n" +
                    "&8&m----------------------------------------"));
            return;
        }

        String category = args[1].toLowerCase();

        switch (category) {
            case "actions":
                handleSettingsActions(sender, args);
                break;
            case "config":
                handleSettingsConfig(sender, args);
                break;
            case "goal":
                handleSettingsGoal(sender, args);
                break;
            default:
                sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Unknown category: &#74ACF6" + category + " &7(options: actions, config, goal)"));
                break;
        }
    }

    private void handleSettingsActions(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize(
                    "&#74ACF6● &fActions Settings:\n" +
                    "  &#74ACF6/ss settings actions list &7- View all triggers\n" +
                    "  &#74ACF6/ss settings actions remove <name> &7- Remove trigger\n" +
                    "  &#74ACF6/ss settings actions create <name> <action> [options...] &7- Create trigger"));
            return;
        }

        String sub = args[2].toLowerCase();

        switch (sub) {
            case "list": {
                ConfigurationSection triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                if (triggers == null) {
                    triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                }
                if (triggers == null || triggers.getKeys(false).isEmpty()) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fNo chat triggers configured."));
                    return;
                }
                StringBuilder sb = new StringBuilder("&8&m----------------------------------------\n&#74ACF6● &fChat Triggers List:\n");
                for (String key : triggers.getKeys(false)) {
                    if ("enabled".equalsIgnoreCase(key)) continue;
                    ConfigurationSection tc = triggers.getConfigurationSection(key);
                    String act = (tc != null) ? tc.getString("action", "UNKNOWN") : "NONE";
                    sb.append("  &#74ACF6").append(key).append(" &7» &#74ACF6").append(act);
                    if (tc != null && tc.contains("mob")) sb.append(" &8(&7mob: &#74ACF6").append(tc.getString("mob")).append("&8)");
                    if (tc != null && tc.contains("effect")) sb.append(" &8(&7eff: &#74ACF6").append(tc.getString("effect")).append("&8)");
                    if (tc != null && tc.contains("item")) sb.append(" &8(&7item: &#74ACF6").append(tc.getString("item")).append("&8)");
                    if (tc != null && tc.contains("block")) sb.append(" &8(&7block: &#74ACF6").append(tc.getString("block")).append("&8)");
                    sb.append("\n");
                }
                sb.append("&8&m----------------------------------------");
                sender.sendMessage(ColorUtil.colorize(sb.toString()));
                break;
            }

            case "remove": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings actions remove <name>"));
                    return;
                }
                String name = args[3].toLowerCase();
                ConfigurationSection sec = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                String path = "chat-triggers.triggers." + name;
                if (sec == null || !sec.contains(name)) {
                    if (sec != null && sec.contains("!" + name)) {
                        name = "!" + name;
                        path = "chat-triggers.triggers." + name;
                    } else if (sec != null && name.startsWith("!") && sec.contains(name.substring(1))) {
                        name = name.substring(1);
                        path = "chat-triggers.triggers." + name;
                    } else {
                        sec = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                        path = "chat-triggers." + name;
                    }
                }
                if (sec != null && sec.contains(name)) {
                    plugin.getConfigManager().getActions().set(path, null);
                    plugin.getConfigManager().saveActions();
                    plugin.getConfigManager().reloadConfigs();
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fTrigger &#74ACF6" + name + " &fhas been successfully removed."));
                } else {
                    sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Trigger &#74ACF6" + name + " &cnot found."));
                }
                break;
            }

            case "create": {
                if (args.length < 5) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings actions create <name> <action> [options...]"));
                    return;
                }
                String name = args[3].toLowerCase();
                String action = args[4].toUpperCase();

                String path = "chat-triggers.triggers." + name;
                ConfigurationSection actCfg = plugin.getConfigManager().getActions().createSection(path);
                actCfg.set("action", action);

                if ("SPAWN_MOB".equalsIgnoreCase(action)) {
                    String mob = (args.length >= 6) ? args[5].toUpperCase() : "RANDOM";
                    String amount = (args.length >= 7) ? args[6] : "1";
                    actCfg.set("mob", mob);
                    actCfg.set("amount", isNumeric(amount) ? Integer.parseInt(amount) : amount);
                    actCfg.set("radius", 2.5);
                    actCfg.set("name", "&c%mob% from &#74ACF6%user%");
                    actCfg.set("sound", "ENTITY_CREEPER_PRIMED");
                    actCfg.set("title", "&c&l" + mob);
                    actCfg.set("subtitle", "&7Spawned by &#74ACF6%user%");
                } else if ("EFFECT".equalsIgnoreCase(action)) {
                    String eff = (args.length >= 6) ? args[5].toUpperCase() : "RANDOM";
                    String dur = (args.length >= 7) ? args[6] : "5";
                    actCfg.set("effect", eff);
                    actCfg.set("duration", isNumeric(dur) ? Integer.parseInt(dur) : dur);
                    actCfg.set("amplifier", 1);
                    actCfg.set("title", "&d&l" + eff);
                    actCfg.set("subtitle", "&7Applied by &#74ACF6%user%");
                } else if ("ITEM_GIVE".equalsIgnoreCase(action) || "GIVE_ITEM".equalsIgnoreCase(action)) {
                    String item = (args.length >= 6) ? args[5].toUpperCase() : "RANDOM";
                    String amount = (args.length >= 7) ? args[6] : "1";
                    actCfg.set("item", item);
                    actCfg.set("amount", isNumeric(amount) ? Integer.parseInt(amount) : amount);
                    actCfg.set("drop-if-full", true);
                    actCfg.set("title", "&a&lITEM: %item%");
                    actCfg.set("subtitle", "&7Gifted by &#74ACF6%user%");
                } else if ("TRAP".equalsIgnoreCase(action)) {
                    String block = (args.length >= 6) ? args[5].toUpperCase() : "COBWEB";
                    String dur = (args.length >= 7) ? args[6] : "5";
                    actCfg.set("block", block);
                    actCfg.set("radius", 1);
                    actCfg.set("height", 2);
                    actCfg.set("duration", isNumeric(dur) ? Integer.parseInt(dur) : dur);
                    actCfg.set("fill-inside", true);
                    actCfg.set("title", "&f&lTRAP: %block%");
                    actCfg.set("subtitle", "&7Trapped by &#74ACF6%user%");
                } else if ("HEART".equalsIgnoreCase(action)) {
                    String mode = (args.length >= 6) ? args[5].toUpperCase() : "GIVE";
                    String amount = (args.length >= 7) ? args[6] : "1";
                    actCfg.set("mode", mode);
                    actCfg.set("amount", isNumeric(amount) ? Double.parseDouble(amount) : 1.0);
                    actCfg.set("title", "&c&lHEART " + mode);
                    actCfg.set("subtitle", "&7From &#74ACF6%user%");
                } else if ("COMMAND".equalsIgnoreCase(action)) {
                    String cmd = (args.length >= 6) ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : "say %user% triggered command";
                    actCfg.set("command", cmd);
                } else {
                    actCfg.set("title", "&#74ACF6&l" + action);
                    actCfg.set("subtitle", "&7Triggered by &#74ACF6%user%");
                }

                plugin.getConfigManager().saveActions();
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fTrigger &#74ACF6" + name + " &fwith action &#74ACF6" + action + " &fhas been created and saved!"));
                break;
            }

            default:
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings actions <list|create|remove>"));
                break;
        }
    }

    private void handleSettingsConfig(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize(
                    "&8&m----------------------------------------\n" +
                    "&#74ACF6● &fStreamSync Configuration Values:\n" +
                    "  &7tiktok-enabled: " + formatBool(plugin.getConfig().getBoolean("tiktok.enabled", true)) + "\n" +
                    "  &7tiktok-username: &#74ACF6" + plugin.getConfig().getString("tiktok.username", "-") + "\n" +
                    "  &7likes-enabled: " + formatBool(plugin.getConfig().getBoolean("likes-enabled", true)) + "\n" +
                    "  &7gifts-enabled: " + formatBool(plugin.getConfig().getBoolean("gifts-enabled", true)) + "\n" +
                    "  &7chat-triggers-enabled: " + formatBool(plugin.getConfig().getBoolean("chat-triggers-enabled", true)) + "\n" +
                    "  &7follow-enabled: " + formatBool(plugin.getConfig().getBoolean("follow-enabled", true)) + "\n" +
                    "  &7chat-show: " + formatBool(plugin.getConfig().getBoolean("chat-show", true)) + "\n" +
                    "&8&m----------------------------------------\n" +
                    "&7Type: &#74ACF6/ss settings config <key> [value] &7to edit"));
            return;
        }

        String key = args[2].toLowerCase();
        String configPath = mapConfigKey(key);

        if (configPath == null) {
            sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Unknown config key: &#74ACF6" + key + "\n&7Available: tiktok-enabled, likes-enabled, gifts-enabled, chat-triggers-enabled, follow-enabled, chat-show, tiktok-username"));
            return;
        }

        if (args.length == 3) {
            Object current = plugin.getConfig().get(configPath);
            sender.sendMessage(ColorUtil.colorize("&#74ACF6● &f" + key + " is currently: &#74ACF6" + current));
            return;
        }

        String val = args[3];
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
            boolean b = Boolean.parseBoolean(val);
            plugin.getConfig().set(configPath, b);
        } else if (isNumeric(val)) {
            plugin.getConfig().set(configPath, Integer.parseInt(val));
        } else {
            plugin.getConfig().set(configPath, val);
        }

        plugin.saveConfig();
        plugin.getConfigManager().reloadConfigs();
        sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fConfiguration updated: &#74ACF6" + key + " &7» &#74ACF6" + val));
    }

    private String mapConfigKey(String key) {
        switch (key) {
            case "tiktok-enabled":
            case "tiktok_enabled":
                return "tiktok.enabled";
            case "tiktok-username":
            case "tiktok_username":
                return "tiktok.username";
            case "likes-enabled":
            case "likes_enabled":
                return "likes-enabled";
            case "gifts-enabled":
            case "gifts_enabled":
                return "gifts-enabled";
            case "chat-triggers-enabled":
            case "chat_triggers_enabled":
                return "chat-triggers-enabled";
            case "follow-enabled":
            case "follow_enabled":
                return "follow-enabled";
            case "chat-show":
            case "chat_show":
                return "chat-show";
            default:
                return null;
        }
    }

    private String formatBool(boolean b) {
        return b ? "&aENABLED" : "&#FC4E4EDISABLED";
    }

    private void handleSettingsGoal(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.colorize(
                    "&#74ACF6● &fGoal Settings Commands:\n" +
                    "  &#74ACF6/ss settings goal list &7- View goal settings\n" +
                    "  &#74ACF6/ss settings goal start &7- Start challenge\n" +
                    "  &#74ACF6/ss settings goal stop &7- Stop active challenge\n" +
                    "  &#74ACF6/ss settings goal set <type> [target] [target-type] &7- Set type and targets\n" +
                    "  &#74ACF6/ss settings goal settings [key] [true|false] &7- Toggle boolean settings\n" +
                    "  &#74ACF6/ss settings goal test <win|lose> &7- Simulate challenge win or loss\n" +
                    "  &#74ACF6/ss settings goal time <add|remove> <time> &7- Add or remove time (e.g. 30s)"));
            return;
        }

        String sub = args[2].toLowerCase();

        switch (sub) {
            case "list": {
                ConfigurationSection g = plugin.getConfigManager().getGoal().getConfigurationSection("goal");
                boolean active = plugin.getGoalManager().isActive();
                String type = (g != null) ? g.getString("type", "SURVIVE") : "SURVIVE";
                String target = (g != null) ? g.getString("target", "300") : "300";
                String targetType = (g != null) ? g.getString("target-type", "ANY") : "ANY";
                boolean dieLose = (g != null) && g.getBoolean("die-lose", false);
                boolean clearInv = (g != null) && g.getBoolean("goal-completed-clearInventory", false);
                boolean bb = (g != null) && g.getBoolean("bossbar.enabled", true);
                boolean ab = (g != null) && g.getBoolean("actionbar.enabled", false);
                boolean autoRestart = (g != null) && g.getBoolean("random-settings.auto-restart", true);
                boolean newPos = (g != null) && g.getBoolean("newGoalNewPosition", true);

                sender.sendMessage(ColorUtil.colorize(
                        "&8&m----------------------------------------\n" +
                        "&#74ACF6● &fStream Challenge Settings (goal.yml):\n" +
                        "  &7Active: " + (active ? "&aYES (" + plugin.getGoalManager().getType() + ")" : "&7NO") + "\n" +
                        "  &7Type: &#74ACF6" + type + "\n" +
                        "  &7Target: &#74ACF6" + target + " &8(&7Type: &#74ACF6" + targetType + "&8)\n" +
                        "  &7Die-Lose: " + formatBool(dieLose) + "\n" +
                        "  &7Clear-Inventory: " + formatBool(clearInv) + "\n" +
                        "  &7BossBar: " + formatBool(bb) + "\n" +
                        "  &7ActionBar: " + formatBool(ab) + "\n" +
                        "  &7Auto-Restart: " + formatBool(autoRestart) + "\n" +
                        "  &7New-Position-On-Win: " + formatBool(newPos) + "\n" +
                        "&8&m----------------------------------------"));
                break;
            }

            case "start": {
                if (plugin.getGoalManager().isActive()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("goal-already-active",
                            "%prefix% &#FC4E4EA goal is already active! Stop it first with &#74ACF6/ss settings goal stop&c."));
                    return;
                }
                plugin.getGoalManager().startGoal();
                if (plugin.getGoalManager().isRandomMode()) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fRandom goal challenge started!"));
                }
                break;
            }

            case "stop": {
                if (!plugin.getGoalManager().isActive()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("goal-not-active",
                            "%prefix% &#FC4E4ENo goal is currently active!"));
                    return;
                }
                plugin.getGoalManager().stopGoal();
                sender.sendMessage(plugin.getConfigManager().getMessage("goal-stopped", "%prefix% &fGoal has been stopped."));
                break;
            }

            case "set": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings goal set <type> [target] [target-type]"));
                    return;
                }
                String type = args[3].toUpperCase();
                List<String> validTypes = Arrays.asList("SURVIVE", "KILL_PLAYERS", "KILL_MOBS", "GET_ITEM", "MINE_BLOCKS", "RANDOM");
                if (!validTypes.contains(type)) {
                    sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Unknown goal type: &#74ACF6" + type + "\n&7Available: &#74ACF6" + String.join("&7, &#74ACF6", validTypes)));
                    return;
                }

                plugin.getConfigManager().getGoal().set("goal.type", type);

                if (args.length >= 5) {
                    String targetVal = args[4];
                    if (args.length >= 6 && isTimeComponent(args[5])) {
                        targetVal = targetVal + " " + args[5];
                    }
                    plugin.getConfigManager().getGoal().set("goal.target", targetVal);
                }

                if (args.length >= 6 && !isTimeComponent(args[5])) {
                    String targetType = args[5].toUpperCase();
                    plugin.getConfigManager().getGoal().set("goal.target-type", targetType);
                } else if (args.length >= 7) {
                    String targetType = args[6].toUpperCase();
                    plugin.getConfigManager().getGoal().set("goal.target-type", targetType);
                }

                plugin.getConfigManager().saveGoal();
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fGoal configuration updated: Type &#74ACF6" + type));
                break;
            }

            case "test": {
                if (args.length < 4) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings goal test <win|lose>"));
                    return;
                }
                String testType = args[3].toLowerCase();
                if ("win".equals(testType)) {
                    plugin.getGoalManager().completeGoal();
                } else if ("lose".equals(testType)) {
                    plugin.getGoalManager().failGoal();
                } else {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings goal test <win|lose>"));
                }
                break;
            }

            case "time": {
                if (args.length < 5) {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings goal time <add|remove> <time (e.g. 30s, 1m)>"));
                    return;
                }
                String mode = args[3].toLowerCase();
                String timeStr = args[4];
                long sec = TimeUtil.parseTimeSeconds(timeStr);
                if (sec <= 0) {
                    sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Invalid time format: &#74ACF6" + timeStr + " &7(use e.g. 30s, 1m, 1h)"));
                    return;
                }

                if (!plugin.getGoalManager().isActive()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("goal-not-active",
                            "%prefix% &#FC4E4ENo goal is currently active!"));
                    return;
                }

                if (!"SURVIVE".equalsIgnoreCase(plugin.getGoalManager().getType())) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("goal-time-only-survive",
                            "%prefix% &#FC4E4ETime can only be added/removed during a SURVIVE goal!"));
                    return;
                }

                if ("add".equals(mode)) {
                    plugin.getGoalManager().addTime(sec);
                } else if ("remove".equals(mode)) {
                    plugin.getGoalManager().removeTime(sec);
                } else {
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fUsage: &#74ACF6/ss settings goal time <add|remove> <time>"));
                }
                break;
            }

            case "settings": {
                if (args.length < 4) {
                    FileConfiguration gc = plugin.getConfigManager().getGoal();
                    boolean dieLose = gc.getBoolean("goal.die-lose", false);
                    boolean clearInv = gc.getBoolean("goal.goal-completed-clearInventory", false);
                    boolean bb = gc.getBoolean("goal.bossbar.enabled", true);
                    boolean ab = gc.getBoolean("goal.actionbar.enabled", false);
                    boolean autoRestart = gc.getBoolean("goal.random-settings.auto-restart", true);
                    boolean newPos = gc.getBoolean("goal.newGoalNewPosition", true);

                    sender.sendMessage(ColorUtil.colorize(
                            "&8&m----------------------------------------\n" +
                            "&#74ACF6● &fGoal Settings (Boolean Toggles):\n" +
                            "  &#74ACF6die-lose: " + formatBool(dieLose) + "\n" +
                            "  &#74ACF6clear-inventory: " + formatBool(clearInv) + "\n" +
                            "  &#74ACF6bossbar: " + formatBool(bb) + "\n" +
                            "  &#74ACF6actionbar: " + formatBool(ab) + "\n" +
                            "  &#74ACF6auto-restart: " + formatBool(autoRestart) + "\n" +
                            "  &#74ACF6newGoalNewPosition: " + formatBool(newPos) + "\n" +
                            "  &7Toggle: &#74ACF6/ss settings goal settings <key> <true|false>\n" +
                            "&8&m----------------------------------------"));
                    return;
                }

                String key = args[3].toLowerCase();
                String path = resolveGoalSettingPath(key);
                if (path == null) {
                    sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Unknown setting: &#74ACF6" + key + " &7(options: die-lose, clear-inventory, bossbar, actionbar, auto-restart, newGoalNewPosition)"));
                    return;
                }

                if (args.length < 5) {
                    boolean curr = plugin.getConfigManager().getGoal().getBoolean(path, false);
                    sender.sendMessage(ColorUtil.colorize("&#74ACF6● &f" + key + " is currently: " + formatBool(curr)));
                    return;
                }

                boolean val = Boolean.parseBoolean(args[4]);
                plugin.getConfigManager().getGoal().set(path, val);
                plugin.getConfigManager().saveGoal();
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &f" + key + " set to: " + formatBool(val)));
                break;
            }

            default:
                sender.sendMessage(ColorUtil.colorize("&#74ACF6● &fGoal usage: &#74ACF6/ss settings goal <list|set|start|stop|settings|test|time>"));
                break;
        }
    }

    private String resolveGoalSettingPath(String key) {
        switch (key.toLowerCase()) {
            case "die-lose":
            case "die_lose":
                return "goal.die-lose";
            case "clear-inventory":
            case "clear_inventory":
                return "goal.goal-completed-clearInventory";
            case "bossbar":
                return "goal.bossbar.enabled";
            case "actionbar":
                return "goal.actionbar.enabled";
            case "auto-restart":
            case "auto_restart":
                return "goal.random-settings.auto-restart";
            case "newgoalnewposition":
            case "new-goal-new-position":
            case "newposition":
                return "goal.newGoalNewPosition";
            default:
                return null;
        }
    }

    private boolean isTimeComponent(String str) {
        if (str == null) return false;
        return str.matches("\\d+[smhdSMHD]");
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize(
                    "&#74ACF6● &fTest Usage:\n" +
                    "  &7/ss test gift <Rose/Panda/Lion> [user]\n" +
                    "  &7/ss test likes [amount]\n" +
                    "  &7/ss test chat <trigger e.g. !creeper or !web>\n" +
                    "  &7/ss test follow [user]"));
            return;
        }

        String type = args[1].toLowerCase();
        String user = (args.length >= 4) ? args[3] : ((args.length == 3 && !isNumeric(args[2])) ? args[2] : "Viewer_" + (new Random().nextInt(899) + 100));

        switch (type) {
            case "chat":
            case "comment": {
                String msg = (args.length >= 3) ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "!creeper";
                ConfigurationSection triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                if (triggers == null) {
                    triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                }
                ConfigurationSection triggerCfg = (triggers != null) ? triggers.getConfigurationSection(msg.toLowerCase()) : null;

                if (triggerCfg != null) {
                    plugin.getQueueManager().addEvent(new StreamEvent(StreamEvent.EventType.TIKTOK_COMMENT, user, msg, 1, msg, triggerCfg));
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &fTriggered chat command &#74ACF6" + msg + " &ffrom &d" + user));
                } else {
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &#FC4E4ENo trigger found for: &c" + msg + " &7(check actions.yml)"));
                }
                break;
            }

            case "gift": {
                String gift = (args.length >= 3) ? args[2] : "Rose";
                ConfigurationSection cfg = plugin.getConfigManager().getActions().getConfigurationSection("gifts." + gift);
                if (cfg == null) {
                    cfg = plugin.getConfigManager().getActions().getConfigurationSection("gifts.default");
                }
                if (cfg != null) {
                    plugin.getQueueManager().addEvent(new StreamEvent(StreamEvent.EventType.TIKTOK_GIFT, user, gift, 1, cfg));
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &fSimulated gift &d" + gift + " &ffrom &#74ACF6" + user));
                }
                break;
            }

            case "likes":
            case "like": {
                int amount = 10;
                if (args.length >= 3 && isNumeric(args[2])) {
                    amount = Integer.parseInt(args[2]);
                }
                ConfigurationSection cfg = plugin.getConfigManager().getActions().getConfigurationSection("likes");
                if (cfg != null) {
                    plugin.getQueueManager().addEvent(new StreamEvent(StreamEvent.EventType.TIKTOK_LIKE, user, "", amount, cfg));
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &fSimulated &d" + amount + " Likes &ffrom &#74ACF6" + user));
                }
                break;
            }

            case "follow": {
                ConfigurationSection cfg = plugin.getConfigManager().getActions().getConfigurationSection("follow");
                if (cfg != null) {
                    plugin.getQueueManager().addEvent(new StreamEvent(StreamEvent.EventType.TIKTOK_FOLLOW, user, "", 1, cfg));
                    sender.sendMessage(ColorUtil.colorize(
                            "&#74ACF6● &fSimulated Follow from &#74ACF6" + user));
                }
                break;
            }

            default:
                sender.sendMessage(ColorUtil.colorize("&#FC4E4E● Unknown test type: &#74ACF6" + type));
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize(
                "&8&m----------------------------------------\n" +
                "&#74ACF6● &fStreamSync &7v" + plugin.getDescription().getVersion() + " &fby &#74ACF6Worstek\n" +
                "  &#74ACF6/ss connect <user> &7- Connect directly to TikTok live\n" +
                "  &#74ACF6/ss disconnect &7- Disconnect from live stream\n" +
                "  &#74ACF6/ss settings <actions|config|goal> &7- In-game settings manager\n" +
                "  &#74ACF6/ss streamer <add|remove|list> &7- Manage active streamers\n" +
                "  &#74ACF6/ss status &7- Check live stream connections\n" +
                "  &#74ACF6/ss test <gift/likes/chat/follow> &7- Simulate events\n" +
                "  &#74ACF6/ss reload &7- Reload all configurations\n" +
                "&8&m----------------------------------------"));
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("connect", "disconnect", "settings", "streamer", "status", "test", "reload", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("settings")) {
            return handleSettingsTab(args);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("streamer")) {
            return Arrays.asList("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("streamer") && args[1].equalsIgnoreCase("add")) {
            List<String> current = plugin.getQueueManager().getStreamerNames();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !current.contains(name))
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("streamer") && args[1].equalsIgnoreCase("remove")) {
            return plugin.getQueueManager().getStreamerNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            return Arrays.asList("chat", "gift", "likes", "follow").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("test") && args[1].equalsIgnoreCase("chat")) {
            ConfigurationSection triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
            if (triggers == null) {
                triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
            }
            if (triggers != null) {
                List<String> list = new ArrayList<>(triggers.getKeys(false));
                list.remove("enabled");
                return list;
            }
            return Arrays.asList("!creeper", "!jump", "!web", "!tnt", "!diamond", "!blind", "!hiss", "!spin", "!scramble");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("test") && args[1].equalsIgnoreCase("gift")) {
            ConfigurationSection gifts = plugin.getConfigManager().getActions().getConfigurationSection("gifts");
            if (gifts != null) {
                List<String> list = new ArrayList<>(gifts.getKeys(false));
                list.remove("enabled");
                return list;
            }
            return Arrays.asList("Rose", "TikTok", "Panda", "Lion");
        }

        return Collections.emptyList();
    }

    private List<String> handleSettingsTab(String[] args) {
        if (args.length == 2) {
            return Arrays.asList("actions", "config", "goal").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String cat = args[1].toLowerCase();

        if ("actions".equals(cat)) {
            if (args.length == 3) {
                return Arrays.asList("list", "create", "remove").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("remove")) {
                ConfigurationSection sec = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                if (sec == null) sec = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                if (sec != null) {
                    List<String> keys = new ArrayList<>(sec.getKeys(false));
                    keys.remove("enabled");
                    return keys.stream().filter(s -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("create")) {
                if (args[3].isEmpty() || "Name".toLowerCase().startsWith(args[3].toLowerCase())) {
                    return Collections.singletonList("Name");
                }
                return Collections.emptyList();
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("create")) {
                return Arrays.asList("SPAWN_MOB", "EFFECT", "ITEM_GIVE", "TRAP", "HEART", "FREEZE", "SLAP", "JUMPSCARE_SOUND", "LAUNCH_UP", "LIGHTNING", "EXPLOSION", "IGNITE", "SPIN", "ANVIL_FALL", "LOOK", "COMMAND").stream()
                        .filter(s -> s.startsWith(args[4].toUpperCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 6 && args[2].equalsIgnoreCase("create")) {
                String act = args[4].toUpperCase();
                if ("SPAWN_MOB".equals(act)) {
                    return Arrays.asList("CREEPER", "ZOMBIE", "SKELETON", "SPIDER", "WARDEN", "BLAZE", "COW", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("EFFECT".equals(act)) {
                    return Arrays.asList("SPEED", "BLINDNESS", "LEVITATION", "GLOWING", "DARKNESS", "SLOWNESS", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("ITEM_GIVE".equals(act) || "GIVE_ITEM".equals(act)) {
                    return Arrays.asList("DIAMOND", "GOLDEN_APPLE", "TOTEM_OF_UNDYING", "IRON_INGOT", "EMERALD", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("TRAP".equals(act)) {
                    return Arrays.asList("COBWEB", "OBSIDIAN", "SLIME_BLOCK", "ICE", "GLASS", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("HEART".equals(act)) {
                    return Arrays.asList("GIVE", "REMOVE", "REGEN").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("LOOK".equals(act)) {
                    return Arrays.asList("SKY", "GROUND", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("FREEZE".equals(act) || "IGNITE".equals(act)) {
                    return Arrays.asList("3s", "5s", "10s").stream().filter(s -> s.startsWith(args[5].toLowerCase())).collect(Collectors.toList());
                }
            }
            if (args.length == 7 && args[2].equalsIgnoreCase("create")) {
                String act = args[4].toUpperCase();
                if ("SPAWN_MOB".equals(act) || "ITEM_GIVE".equals(act) || "GIVE_ITEM".equals(act)) {
                    return Arrays.asList("1", "2", "3", "5", "1-5", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[6].toLowerCase())).collect(Collectors.toList());
                } else if ("EFFECT".equals(act) || "TRAP".equals(act)) {
                    return Arrays.asList("3s", "5s", "10s", "15s", "30s").stream()
                            .filter(s -> s.startsWith(args[6].toLowerCase())).collect(Collectors.toList());
                } else if ("HEART".equals(act)) {
                    return Arrays.asList("1", "2", "3").stream()
                            .filter(s -> s.startsWith(args[6].toLowerCase())).collect(Collectors.toList());
                }
            }
        }

        if ("config".equals(cat)) {
            if (args.length == 3) {
                return Arrays.asList("tiktok-enabled", "likes-enabled", "gifts-enabled", "chat-triggers-enabled", "follow-enabled", "chat-show", "tiktok-username").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                if ("tiktok-username".equalsIgnoreCase(args[2])) {
                    return Collections.emptyList();
                }
                return Arrays.asList("true", "false").stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if ("goal".equals(cat)) {
            if (args.length == 3) {
                return Arrays.asList("list", "start", "stop", "set", "settings", "test", "time").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                String gSub = args[2].toLowerCase();
                if ("set".equals(gSub)) {
                    return Arrays.asList("SURVIVE", "KILL_PLAYERS", "KILL_MOBS", "GET_ITEM", "MINE_BLOCKS", "RANDOM").stream()
                            .filter(s -> s.startsWith(args[3].toUpperCase()))
                            .collect(Collectors.toList());
                }
                if ("test".equals(gSub)) {
                    return Arrays.asList("win", "lose").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if ("time".equals(gSub)) {
                    return Arrays.asList("add", "remove").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if ("settings".equals(gSub)) {
                    return Arrays.asList("die-lose", "clear-inventory", "bossbar", "actionbar", "auto-restart", "newGoalNewPosition").stream()
                            .filter(s -> s.startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (args.length == 5) {
                String gSub = args[2].toLowerCase();
                if ("settings".equals(gSub)) {
                    return Arrays.asList("true", "false").stream()
                            .filter(s -> s.startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if ("time".equals(gSub)) {
                    return Arrays.asList("10s", "30s", "1m", "5m", "10m").stream()
                            .filter(s -> s.startsWith(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if ("set".equals(gSub)) {
                    String type = args[3].toUpperCase();
                    if ("SURVIVE".equals(type)) {
                        return Arrays.asList("5m", "10m", "15m", "30m", "1h", "300s").stream()
                                .filter(s -> s.startsWith(args[4].toLowerCase())).collect(Collectors.toList());
                    } else if ("KILL_PLAYERS".equals(type)) {
                        return Arrays.asList("5", "10", "15").stream().filter(s -> s.startsWith(args[4].toLowerCase())).collect(Collectors.toList());
                    } else if ("KILL_MOBS".equals(type)) {
                        return Arrays.asList("5", "10", "20", "30").stream().filter(s -> s.startsWith(args[4].toLowerCase())).collect(Collectors.toList());
                    } else if ("GET_ITEM".equals(type)) {
                        return Arrays.asList("1", "5", "10", "32", "64").stream().filter(s -> s.startsWith(args[4].toLowerCase())).collect(Collectors.toList());
                    } else if ("MINE_BLOCKS".equals(type)) {
                        return Arrays.asList("15", "30", "50", "64").stream().filter(s -> s.startsWith(args[4].toLowerCase())).collect(Collectors.toList());
                    }
                }
            }
            if (args.length == 6 && args[2].equalsIgnoreCase("set")) {
                String type = args[3].toUpperCase();
                if ("KILL_MOBS".equals(type)) {
                    return Arrays.asList("ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ANY").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("GET_ITEM".equals(type)) {
                    return Arrays.asList("DIAMOND", "IRON_INGOT", "GOLDEN_APPLE", "DIORITE", "ANY").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                } else if ("MINE_BLOCKS".equals(type)) {
                    return Arrays.asList("STONE", "COAL_ORE", "DIORITE", "IRON_ORE", "DEEPSLATE", "ANY").stream()
                            .filter(s -> s.startsWith(args[5].toUpperCase())).collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}