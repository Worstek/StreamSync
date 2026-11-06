package com.worstek.streamsync.goals;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.utils.ColorUtil;
import com.worstek.streamsync.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GoalManager {

    private static final Random random = new Random();

    private static final String[] SAFE_ITEMS = {
            "DIORITE", "IRON_INGOT", "DIAMOND", "GOLD_INGOT", "COAL", "BREAD",
            "APPLE", "COOKED_BEEF", "OAK_LOG", "FEATHER", "STRING", "LEATHER",
            "WHEAT", "SUGAR_CANE", "ARROW", "TORCH"
    };

    private static final String[] SAFE_BLOCKS = {
            "STONE", "COBBLESTONE", "DIRT", "GRAVEL", "SAND", "OAK_LOG",
            "BIRCH_LOG", "COAL_ORE", "IRON_ORE", "DIORITE", "ANDESITE", "GRANITE", "DEEPSLATE"
    };

    private static final String[] SAFE_MOBS = {
            "ZOMBIE", "SKELETON", "SPIDER", "CREEPER", "COW", "PIG", "SHEEP", "CHICKEN"
    };

    private final StreamSync plugin;
    private boolean active = false;
    private boolean isRandomMode = false;
    private String type = "SURVIVE";
    private String targetType = "ANY";
    private long target = 300;
    private long current = 0;
    private BukkitTask tickerTask;
    private BossBar bossBar;

    public GoalManager(StreamSync plugin) {
        this.plugin = plugin;
    }

    public boolean startGoal() {
        stopGoal();

        FileConfiguration cfg = plugin.getConfigManager().getGoal();
        String configuredType = cfg.getString("goal.type", "SURVIVE").toUpperCase();

        if ("RANDOM".equalsIgnoreCase(configuredType)) {
            this.isRandomMode = true;
            setupRandomGoal();
        } else {
            this.isRandomMode = false;
            this.type = configuredType;
            this.targetType = cfg.getString("goal.target-type", "ANY").toUpperCase();
            String targetRaw = cfg.getString("goal.target", "300");
            this.target = Math.max(1, TimeUtil.parseTimeSeconds(targetRaw));

            if ("SURVIVE".equalsIgnoreCase(type)) {
                this.current = this.target;
            } else {
                this.current = 0;
            }
        }

        boolean bbEnabled = cfg.getBoolean("goal.bossbar.enabled", true);
        if (bbEnabled) {
            String colorStr = cfg.getString("goal.bossbar.color", "BLUE").toUpperCase();
            String styleStr = cfg.getString("goal.bossbar.style", "SOLID").toUpperCase();

            BarColor barColor;
            try {
                barColor = BarColor.valueOf(colorStr);
            } catch (Exception e) {
                barColor = BarColor.BLUE;
            }

            BarStyle barStyle;
            try {
                barStyle = BarStyle.valueOf(styleStr);
            } catch (Exception e) {
                barStyle = BarStyle.SOLID;
            }

            String defaultTitle = getDynamicBossBarTitle();
            bossBar = Bukkit.createBossBar(formatPlaceholders(defaultTitle), barColor, barStyle);
            bossBar.setProgress(calculateProgress());
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
            }
            bossBar.setVisible(true);
        }

        this.active = true;

        if (!isRandomMode) {
            String displayTarget = ("SURVIVE".equalsIgnoreCase(type)) ? formatTime(target) : (target + " " + targetType);
            String startMsg = plugin.getConfigManager().getMessage("goal-started",
                    "%prefix% &fGoal started: &#74ACF6%type% &8(&fTarget: &#74ACF6%target%&8)")
                    .replace("%type%", type)
                    .replace("%target%", displayTarget);
            Bukkit.broadcastMessage(startMsg);
        }

        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        return true;
    }

    private void setupRandomGoal() {
        FileConfiguration cfg = plugin.getConfigManager().getGoal();
        ConfigurationSection rnd = cfg.getConfigurationSection("goal.random-settings");
        if (rnd == null) rnd = cfg.getConfigurationSection("goal.randomSettings");

        List<String> types = new ArrayList<>();
        types.add("SURVIVE");
        types.add("KILL_MOBS");
        types.add("GET_ITEM");
        types.add("MINE_BLOCKS");

        if (Bukkit.getOnlinePlayers().size() >= 3) {
            types.add("KILL_PLAYERS");
        }

        this.type = types.get(random.nextInt(types.size()));

        switch (this.type) {
            case "SURVIVE": {
                String minTimeStr = (rnd != null) ? rnd.getString("minTime", rnd.getString("min-time", "5m")) : "5m";
                String maxTimeStr = (rnd != null) ? rnd.getString("maxTime", rnd.getString("max-time", "30m")) : "30m";
                int minMinutes = (int) Math.max(1, TimeUtil.parseTimeSeconds(minTimeStr) / 60);
                int maxMinutes = (int) Math.max(minMinutes, TimeUtil.parseTimeSeconds(maxTimeStr) / 60);

                int chosenMinutes = random.nextInt(Math.max(1, maxMinutes - minMinutes + 1)) + minMinutes;
                this.target = (long) chosenMinutes * 60L;
                this.current = this.target;
                this.targetType = "ANY";
                break;
            }

            case "KILL_PLAYERS": {
                int minK = (rnd != null) ? rnd.getInt("minKills", rnd.getInt("min-kills", 5)) : 5;
                int maxK = (rnd != null) ? rnd.getInt("maxKills", rnd.getInt("max-kills", 15)) : 15;
                this.target = random.nextInt(Math.max(1, maxK - minK + 1)) + minK;
                this.current = 0;
                this.targetType = "PLAYERS";
                break;
            }

            case "KILL_MOBS": {
                int minK = (rnd != null) ? rnd.getInt("minKills", rnd.getInt("min-kills", 5)) : 5;
                int maxK = (rnd != null) ? rnd.getInt("maxKills", rnd.getInt("max-kills", 15)) : 15;
                this.target = random.nextInt(Math.max(1, maxK - minK + 1)) + minK;
                this.current = 0;
                this.targetType = SAFE_MOBS[random.nextInt(SAFE_MOBS.length)];
                break;
            }

            case "GET_ITEM": {
                int minC = (rnd != null) ? rnd.getInt("minCollectItems", rnd.getInt("min-collect-items", 5)) : 5;
                int maxC = (rnd != null) ? rnd.getInt("maxCollectItems", rnd.getInt("max-collect-items", 30)) : 30;
                this.target = random.nextInt(Math.max(1, maxC - minC + 1)) + minC;
                this.current = 0;
                this.targetType = SAFE_ITEMS[random.nextInt(SAFE_ITEMS.length)];
                break;
            }

            case "MINE_BLOCKS": {
                int minM = (rnd != null) ? rnd.getInt("minMineBlocks", rnd.getInt("min-mine-blocks", 15)) : 15;
                int maxM = (rnd != null) ? rnd.getInt("maxMineBlocks", rnd.getInt("max-mine-blocks", 30)) : 30;
                this.target = random.nextInt(Math.max(1, maxM - minM + 1)) + minM;
                this.current = 0;
                this.targetType = SAFE_BLOCKS[random.nextInt(SAFE_BLOCKS.length)];
                break;
            }
        }
    }

    public void stopGoal() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        this.active = false;
    }

    private void tick() {
        if (!active) return;

        FileConfiguration cfg = plugin.getConfigManager().getGoal();

        if ("SURVIVE".equalsIgnoreCase(type)) {
            current--;
            if (current <= 0) {
                completeGoal();
                return;
            }
        } else if ("GET_ITEM".equalsIgnoreCase(type)) {
            String targetItem = targetType;
            long count = 0;
            for (Player p : plugin.getQueueManager().getTargetStreamers()) {
                for (ItemStack it : p.getInventory().getContents()) {
                    if (it != null && it.getType().name().equalsIgnoreCase(targetItem)) {
                        count += it.getAmount();
                    }
                }
            }
            this.current = count;
            if (current >= target) {
                completeGoal();
                return;
            }
        }

        if (bossBar != null) {
            bossBar.setProgress(calculateProgress());
            bossBar.setTitle(formatPlaceholders(getDynamicBossBarTitle()));
        }

        boolean abEnabled = cfg.getBoolean("goal.actionbar.enabled", false);
        if (abEnabled) {
            String abText = formatPlaceholders(cfg.getString("goal.actionbar.text", ""));
            for (Player p : plugin.getQueueManager().getTargetStreamers()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(abText));
            }
        }
    }

    public void addTime(long seconds) {
        if (!active) return;
        if ("SURVIVE".equalsIgnoreCase(type)) {
            this.current += seconds;
            if (this.current > this.target) {
                this.target = this.current;
            }
            if (bossBar != null) {
                bossBar.setProgress(calculateProgress());
                bossBar.setTitle(formatPlaceholders(getDynamicBossBarTitle()));
            }
            String msg = plugin.getConfigManager().getMessage("goal-time-added",
                    "%prefix% &fAdded &#74ACF6+%time% &fto the challenge!").replace("%time%", formatTime(seconds));
            Bukkit.broadcastMessage(msg);
        }
    }

    public void removeTime(long seconds) {
        if (!active) return;
        if ("SURVIVE".equalsIgnoreCase(type)) {
            this.current -= seconds;
            if (this.current <= 0) {
                this.current = 0;
                completeGoal();
                return;
            }
            if (bossBar != null) {
                bossBar.setProgress(calculateProgress());
                bossBar.setTitle(formatPlaceholders(getDynamicBossBarTitle()));
            }
            String msg = plugin.getConfigManager().getMessage("goal-time-removed",
                    "%prefix% &#FC4E4ERemoved &#74ACF6-%time% &ffrom the challenge!").replace("%time%", formatTime(seconds));
            Bukkit.broadcastMessage(msg);
        }
    }

    public void addProgress(long amount) {
        if (!active || "SURVIVE".equalsIgnoreCase(type)) return;
        this.current += amount;
        if (this.current >= this.target) {
            completeGoal();
        } else if (bossBar != null) {
            bossBar.setProgress(calculateProgress());
            bossBar.setTitle(formatPlaceholders(getDynamicBossBarTitle()));
        }
    }

    public void completeGoal() {
        if (!active) return;
        FileConfiguration cfg = plugin.getConfigManager().getGoal();
        ConfigurationSection rewards = cfg.getConfigurationSection("goal.rewards");

        List<Player> streamers = plugin.getQueueManager().getTargetStreamers();

        if (rewards != null) {
            String title = rewards.getString("title", "&a&lGOAL COMPLETED!");
            String subtitle = rewards.getString("subtitle", "&fCongratulations!");
            String soundName = rewards.getString("sound", "UI_TOAST_CHALLENGE_COMPLETE");
            boolean firework = rewards.getBoolean("firework", true);

            for (Player p : streamers) {
                p.sendTitle(ColorUtil.colorize(title), ColorUtil.colorize(subtitle), 10, 60, 20);
                if (!"NONE".equalsIgnoreCase(soundName)) {
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                }
                if (firework) {
                    spawnFirework(p.getLocation());
                }
            }

            List<String> commands = rewards.getStringList("commands");
            if (commands != null) {
                for (String cmd : commands) {
                    String pName = (!streamers.isEmpty()) ? streamers.get(0).getName() : "Streamer";
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%streamer%", pName));
                }
            }
        }

        boolean clearInv = cfg.getBoolean("goal.goal-completed-clearInventory", false)
                || cfg.getBoolean("goal.rewards.clear-inventory", false);
        if (clearInv) {
            for (Player p : streamers) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.getInventory().setItemInOffHand(null);
                p.setExp(0.0f);
                p.setLevel(0);
                p.setTotalExperience(0);
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
            }
        }

        String completeMsg = plugin.getConfigManager().getMessage("goal-completed",
                "%prefix% &fGoal &#74ACF6%type% &fhas been successfully completed!").replace("%type%", type);
        Bukkit.broadcastMessage(completeMsg);

        boolean newPos = cfg.getBoolean("goal.newGoalNewPosition", true);
        if (newPos) {
            teleportStreamersToNewPosition(streamers);
        }

        stopGoal();

        checkAutoRestartRandom();
    }

    private void teleportStreamersToNewPosition(List<Player> streamers) {
        if (streamers == null || streamers.isEmpty()) return;

        Player primary = streamers.get(0);
        World world = primary.getWorld();
        if (world == null) return;

        Location targetLoc = findSafeNewLocation(world, primary.getLocation());
        if (targetLoc == null) return;

        PotionEffectType res = PotionEffectType.getByName("RESISTANCE");
        if (res == null) res = PotionEffectType.getByName("DAMAGE_RESISTANCE");
        PotionEffectType slowFall = PotionEffectType.getByName("SLOW_FALLING");

        for (Player p : streamers) {
            if (p != null && p.isOnline()) {
                p.teleport(targetLoc);
                p.setFallDistance(0.0f);
                if (res != null) {
                    p.addPotionEffect(new PotionEffect(res, 80, 5));
                }
                if (slowFall != null) {
                    p.addPotionEffect(new PotionEffect(slowFall, 80, 0));
                }
                try {
                    p.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } catch (Exception ignored) {}
                p.sendMessage(ColorUtil.colorize("&#74ACF6● &fTeleported to new coordinates: &#74ACF6X: " + targetLoc.getBlockX() + " Y: " + targetLoc.getBlockY() + " Z: " + targetLoc.getBlockZ()));
            }
        }
    }

    private Location findSafeNewLocation(World world, Location currentLoc) {
        int baseDistance = 2000;
        int spread = 8000;

        for (int attempt = 0; attempt < 25; attempt++) {
            int signX = random.nextBoolean() ? 1 : -1;
            int signZ = random.nextBoolean() ? 1 : -1;
            int dx = signX * (baseDistance + random.nextInt(spread));
            int dz = signZ * (baseDistance + random.nextInt(spread));

            int targetX = currentLoc.getBlockX() + dx;
            int targetZ = currentLoc.getBlockZ() + dz;

            int highestY = world.getHighestBlockYAt(targetX, targetZ);
            if (highestY <= -60) continue;

            org.bukkit.block.Block ground = world.getBlockAt(targetX, highestY, targetZ);
            org.bukkit.block.Block feet = world.getBlockAt(targetX, highestY + 1, targetZ);
            org.bukkit.block.Block head = world.getBlockAt(targetX, highestY + 2, targetZ);

            if (ground.isLiquid() || ground.getType().name().contains("LAVA") || ground.getType().name().contains("WATER")) {
                continue;
            }
            if (feet.isLiquid() || feet.getType().isSolid()) {
                continue;
            }
            if (head.isLiquid() || head.getType().isSolid()) {
                continue;
            }

            return new Location(world, targetX + 0.5, highestY + 1, targetZ + 0.5, currentLoc.getYaw(), currentLoc.getPitch());
        }

        int fallbackX = currentLoc.getBlockX() + (random.nextBoolean() ? 3000 : -3000);
        int fallbackZ = currentLoc.getBlockZ() + (random.nextBoolean() ? 3000 : -3000);
        int fallbackY = Math.max(70, world.getHighestBlockYAt(fallbackX, fallbackZ));
        return new Location(world, fallbackX + 0.5, fallbackY + 1, fallbackZ + 0.5, currentLoc.getYaw(), currentLoc.getPitch());
    }

    public void failGoal() {
        if (!active) return;
        FileConfiguration cfg = plugin.getConfigManager().getGoal();
        ConfigurationSection failed = cfg.getConfigurationSection("goal.failed");

        List<Player> streamers = plugin.getQueueManager().getTargetStreamers();

        if (failed != null) {
            String title = failed.getString("title", "&#FC4E4E&lCHALLENGE FAILED!");
            String subtitle = failed.getString("subtitle", "&7Better luck next stream!");
            String soundName = failed.getString("sound", "ENTITY_ENDER_DRAGON_GROWL");

            for (Player p : streamers) {
                p.sendTitle(ColorUtil.colorize(title), ColorUtil.colorize(subtitle), 10, 60, 20);
                if (!"NONE".equalsIgnoreCase(soundName)) {
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
                    } catch (Exception ignored) {}
                }
            }
        }

        String failMsg = plugin.getConfigManager().getMessage("goal-failed",
                "%prefix% &#FC4E4EGoal challenge &#74ACF6%type% &#FC4E4Efailed!").replace("%type%", type);
        Bukkit.broadcastMessage(failMsg);

        stopGoal();

        checkAutoRestartRandom();
    }

    private void checkAutoRestartRandom() {
        if (isRandomMode) {
            boolean autoRestart = plugin.getConfigManager().getGoal().getBoolean("goal.random-settings.auto-restart", true);
            if (autoRestart) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!active) {
                        startGoal();
                    }
                }, 100L);
            }
        }
    }

    private double calculateProgress() {
        if (target <= 0) return 0.0;
        double p = (double) current / (double) target;
        return Math.max(0.0, Math.min(1.0, p));
    }

    private String getDynamicBossBarTitle() {
        FileConfiguration cfg = plugin.getConfigManager().getGoal();
        if ("SURVIVE".equalsIgnoreCase(type)) {
            return cfg.getString("goal.bossbar.title", "&#74ACF6● &fSurvive: &#74ACF6%time% &8(&f%percent%%&8)");
        } else if ("KILL_PLAYERS".equalsIgnoreCase(type)) {
            return "&#74ACF6● &fKill Players: &#74ACF6%current%&7/&#74ACF6%target% &8(&f%percent%%&8)";
        } else if ("KILL_MOBS".equalsIgnoreCase(type)) {
            return "&#74ACF6● &fKill %target_type%: &#74ACF6%current%&7/&#74ACF6%target% &8(&f%percent%%&8)";
        } else if ("GET_ITEM".equalsIgnoreCase(type)) {
            return "&#74ACF6● &fCollect %target_type%: &#74ACF6%current%&7/&#74ACF6%target% &8(&f%percent%%&8)";
        } else if ("MINE_BLOCKS".equalsIgnoreCase(type)) {
            return "&#74ACF6● &fMine %target_type%: &#74ACF6%current%&7/&#74ACF6%target% &8(&f%percent%%&8)";
        }
        return cfg.getString("goal.bossbar.title", "&#74ACF6● &fGoal: %current%/%target%");
    }

    private String formatPlaceholders(String text) {
        if (text == null) return "";
        long timeVal = ("SURVIVE".equalsIgnoreCase(type)) ? current : 0;
        int percent = (int) Math.round(calculateProgress() * 100);

        String result = text
                .replace("%time%", formatTime(timeVal))
                .replace("%time_formatted%", formatTime(timeVal))
                .replace("%seconds%", String.valueOf(timeVal))
                .replace("%current%", String.valueOf(current))
                .replace("%target%", String.valueOf(target))
                .replace("%percent%", String.valueOf(percent))
                .replace("%type%", type)
                .replace("%target_type%", targetType);

        if (result.contains("%%")) {
            result = result.replace("%%", "%");
        }

        return ColorUtil.colorize(result);
    }

    public String formatTime(long seconds) {
        if (seconds <= 0) return "0" + plugin.getConfigManager().getGoal().getString("goal.format-seconds", "s");

        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        String sD = plugin.getConfigManager().getGoal().getString("goal.format-days", "d");
        String sH = plugin.getConfigManager().getGoal().getString("goal.format-hours", "h");
        String sM = plugin.getConfigManager().getGoal().getString("goal.format-minutes", "m");
        String sS = plugin.getConfigManager().getGoal().getString("goal.format-seconds", "s");

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append(sD).append(" ");
        if (h > 0) sb.append(h).append(sH).append(" ");
        if (m > 0) sb.append(m).append(sM).append(" ");
        if (s > 0 || sb.length() == 0) sb.append(s).append(sS);

        return sb.toString().trim();
    }

    private void spawnFirework(Location loc) {
        try {
            Firework fw = loc.getWorld().spawn(loc, Firework.class);
            FireworkMeta fwm = fw.getFireworkMeta();
            fwm.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.AQUA, Color.LIME)
                    .withFade(Color.WHITE)
                    .build());
            fwm.setPower(1);
            fw.setFireworkMeta(fwm);
        } catch (Exception ignored) {}
    }

    public boolean isActive() {
        return active;
    }

    public String getType() {
        return type;
    }

    public String getTargetType() {
        return targetType;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public long getTarget() {
        return target;
    }

    public long getCurrent() {
        return current;
    }

    public int getProgressPercent() {
        return (int) Math.round(calculateProgress() * 100);
    }

    public boolean isRandomMode() {
        return isRandomMode;
    }
}
