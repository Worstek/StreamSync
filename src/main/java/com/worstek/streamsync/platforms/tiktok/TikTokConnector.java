package com.worstek.streamsync.platforms.tiktok;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.queue.StreamEvent;
import com.worstek.streamsync.utils.ColorUtil;
import io.github.jwdeveloper.tiktok.api.live.LiveClient;
import io.github.jwdeveloper.tiktok.client.TikTokLive;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TikTokConnector {

    private final StreamSync plugin;
    private LiveClient client;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private String currentUsername = "";
    private final AtomicInteger likeCounter = new AtomicInteger(0);

    private final AtomicLong totalLikes = new AtomicLong(0);
    private final AtomicLong totalGifts = new AtomicLong(0);
    private volatile String lastGift = "-";
    private volatile String lastGifter = "-";
    private volatile String topGifter = "-";
    private volatile String topGift = "-";
    private final Map<String, AtomicLong> userGiftTotals = new ConcurrentHashMap<>();

    public TikTokConnector(StreamSync plugin) {
        this.plugin = plugin;
    }

    public static String sanitizeUsername(String input) {
        if (input == null) return "";
        String s = input.trim();

        if (s.contains("?")) {
            s = s.substring(0, s.indexOf("?"));
        }

        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        if (s.contains("@")) {
            s = s.substring(s.lastIndexOf("@") + 1);
        } else if (s.contains("/")) {
            if (s.endsWith("/live")) {
                s = s.substring(0, s.length() - 5);
            }
            s = s.substring(s.lastIndexOf("/") + 1);
        }

        s = s.replace("@", "").trim();
        if (s.contains("/")) {
            s = s.substring(0, s.indexOf("/"));
        }

        return s;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("tiktok.enabled", true)) return;
        String rawUser = plugin.getConfig().getString("tiktok.username", "").trim();
        String user = sanitizeUsername(rawUser);
        if (!user.isEmpty() && !"your_tiktok_username".equalsIgnoreCase(user)) {
            if (isConnected.get() && user.equalsIgnoreCase(currentUsername) && client != null) {
                plugin.getLogger().info("[TikTok] Connection to @" + user + " is already active.");
                return;
            }
            connect(user, false);
        }
    }

    public void connect(String username) {
        connect(username, true);
    }

    public synchronized void connect(String username, boolean saveToConfig) {
        String cleanUser = sanitizeUsername(username);
        if (cleanUser.isEmpty() || "your_tiktok_username".equalsIgnoreCase(cleanUser)) {
            plugin.getLogger().info("[TikTok] Username not configured. Waiting for /ss connect <user>...");
            return;
        }

        final String targetUser = cleanUser;

        if (isConnected.get() && targetUser.equalsIgnoreCase(currentUsername) && client != null) {
            notifyStreamers(plugin.getConfigManager().getMessage("already-connected", "&#74ACF6● &aAlready connected to @%user%").replace("%user%", targetUser));
            return;
        }

        disconnect();
        this.currentUsername = targetUser;
        this.totalLikes.set(0);
        this.totalGifts.set(0);
        this.lastGift = "-";
        this.lastGifter = "-";
        this.topGifter = "-";
        this.topGift = "-";
        this.userGiftTotals.clear();

        if (saveToConfig) {
            plugin.getConfig().set("tiktok.username", targetUser);
            plugin.getConfig().set("tiktok.enabled", true);
            plugin.saveConfig();
        }

        plugin.getLogger().info("[TikTok] Connecting live client to @" + targetUser + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                client = TikTokLive.newClient(targetUser)
                        .onConnected((liveClient, event) -> {
                            isConnected.set(true);
                            plugin.getLogger().info("[TikTok] Successfully connected to @" + targetUser + " LIVE stream!");
                            notifyStreamers(plugin.getConfigManager().getMessage("connected", "%prefix% &fSuccessfully connected to TikTok: &#74ACF6@%user%").replace("%user%", targetUser));
                        })
                        .onDisconnected((liveClient, event) -> {
                            if (isConnected.compareAndSet(true, false)) {
                                plugin.getLogger().info("[TikTok] Disconnected from @" + targetUser);
                                notifyStreamers(plugin.getConfigManager().getMessage("disconnected", "%prefix% &fDisconnected from TikTok: &#74ACF6@%user%").replace("%user%", targetUser));
                            }
                        })
                        .onError((liveClient, event) -> {
                            isConnected.set(false);
                            plugin.getLogger().warning("[TikTok] Connection error: " + event.getException().getMessage());
                            notifyStreamers(plugin.getConfigManager().getMessage("error", "%prefix% &fTikTok error: &c%error%").replace("%error%", event.getException().getMessage()));
                        })
                        .onComment((liveClient, event) -> {
                            String userName = (event.getUser() != null) ? event.getUser().getProfileName() : "Viewer";
                            String text = event.getText();

                            if (plugin.getConfig().getBoolean("chat-show", true)) {
                                String chatFormat = plugin.getConfigManager().getMessages().getString("tiktok-chat-format",
                                        "&#928F86[&#928F85T&#928E84i&#928E83k&#918D82T&#918D81o&#918C80k&#918C7F] &f%user%&8: &f%message%");
                                notifyStreamers(chatFormat.replace("%user%", userName).replace("%message%", text));
                            }

                            boolean globalChatEnabled = plugin.getConfig().getBoolean("chat-triggers-enabled", true);
                            boolean actionsChatEnabled = plugin.getConfigManager().getActions().getBoolean("chat-triggers.enabled", true);
                            if (globalChatEnabled && actionsChatEnabled) {
                                ConfigurationSection triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                                if (triggers == null) {
                                    triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                                }
                                if (triggers != null) {
                                    String lowerText = text.toLowerCase().trim();
                                    for (String triggerKey : triggers.getKeys(false)) {
                                        if ("enabled".equalsIgnoreCase(triggerKey)) continue;
                                        if (lowerText.contains(triggerKey.toLowerCase())) {
                                            ConfigurationSection triggerCfg = triggers.getConfigurationSection(triggerKey);
                                            if (triggerCfg != null) {
                                                plugin.getQueueManager().addEvent(new StreamEvent(
                                                        StreamEvent.EventType.TIKTOK_COMMENT, userName, triggerKey, 1, text, triggerCfg));
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        })
                        .onLike((liveClient, event) -> {
                            int likes = event.getLikes();
                            totalLikes.addAndGet(likes);
                            if (!plugin.getConfig().getBoolean("likes-enabled", true)) return;
                            ConfigurationSection likesCfg = plugin.getConfigManager().getActions().getConfigurationSection("likes");
                            if (likesCfg == null || !likesCfg.getBoolean("enabled", true)) return;

                            int step = Math.max(1, likesCfg.getInt("step", 10));

                            int current = likeCounter.addAndGet(likes);
                            if (current >= step) {
                                likeCounter.set(current % step);
                                String userName = (event.getUser() != null) ? event.getUser().getProfileName() : "Viewer";
                                plugin.getQueueManager().addEvent(new StreamEvent(
                                        StreamEvent.EventType.TIKTOK_LIKE, userName, "", likes, likesCfg));
                            }
                        })
                        .onGift((liveClient, event) -> {
                            String giftName = (event.getGift() != null) ? event.getGift().getName() : "Rose";
                            String userName = (event.getUser() != null) ? event.getUser().getProfileName() : "Viewer";
                            int combo = Math.max(1, event.getCombo());

                            totalGifts.addAndGet(combo);
                            lastGift = giftName;
                            lastGifter = userName;

                            if (userName != null && !userName.isEmpty()) {
                                AtomicLong userCount = userGiftTotals.computeIfAbsent(userName, k -> new AtomicLong(0));
                                long newTotal = userCount.addAndGet(combo);
                                long topVal = userGiftTotals.getOrDefault(topGifter, new AtomicLong(0)).get();
                                if (newTotal >= topVal) {
                                    topGifter = userName;
                                    topGift = giftName;
                                }
                            }

                            if (!plugin.getConfig().getBoolean("gifts-enabled", true)) return;
                            if (!plugin.getConfigManager().getActions().getBoolean("gifts.enabled", true)) return;

                            ConfigurationSection giftCfg = resolveGiftConfig(giftName);
                            if (giftCfg != null) {
                                plugin.getQueueManager().addEvent(new StreamEvent(
                                        StreamEvent.EventType.TIKTOK_GIFT, userName, giftName, combo, giftCfg));
                            }
                        })
                        .onFollow((liveClient, event) -> {
                            if (!plugin.getConfig().getBoolean("follow-enabled", true)) return;
                            ConfigurationSection followCfg = plugin.getConfigManager().getActions().getConfigurationSection("follow");
                            if (followCfg == null || !followCfg.getBoolean("enabled", true)) return;

                            String userName = (event.getUser() != null) ? event.getUser().getProfileName() : "Viewer";
                            plugin.getQueueManager().addEvent(new StreamEvent(
                                    StreamEvent.EventType.TIKTOK_FOLLOW, userName, "", 1, followCfg));
                        })
                        .buildAndConnect();

            } catch (Exception e) {
                plugin.getLogger().warning("[TikTok] Failed to initialize connection: " + e.getMessage());
                isConnected.set(false);
            }
        });
    }

    public void disconnect() {
        LiveClient toDisconnect;
        String user;
        boolean wasConnected;

        synchronized (this) {
            toDisconnect = this.client;
            this.client = null;
            user = this.currentUsername;
            wasConnected = isConnected.compareAndSet(true, false);
        }

        if (toDisconnect != null) {
            try {
                toDisconnect.disconnect();
            } catch (Exception ignored) {}
        }

        if (wasConnected && user != null && !user.isEmpty()) {
            notifyStreamers(plugin.getConfigManager().getMessage("disconnected",
                    "%prefix% &fDisconnected from TikTok: &#74ACF6@%user%").replace("%user%", user));
        }
    }

    public void stop() {
        disconnect();
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    private ConfigurationSection resolveGiftConfig(String giftName) {
        ConfigurationSection gifts = plugin.getConfigManager().getActions().getConfigurationSection("gifts");
        if (gifts == null) return null;

        for (String key : gifts.getKeys(false)) {
            if ("enabled".equalsIgnoreCase(key)) continue;
            if (key.equalsIgnoreCase(giftName)) {
                return gifts.getConfigurationSection(key);
            }
        }

        ConfigurationSection giftLists = plugin.getConfigManager().getActions().getConfigurationSection("giftLists");
        if (giftLists == null) {
            giftLists = plugin.getConfigManager().getActions().getConfigurationSection("gift-lists");
        }
        if (giftLists != null) {
            for (String groupKey : giftLists.getKeys(false)) {
                java.util.List<String> list = giftLists.getStringList(groupKey);
                for (String item : list) {
                    if (item.equalsIgnoreCase(giftName)) {
                        for (String key : gifts.getKeys(false)) {
                            if (key.equalsIgnoreCase(groupKey)) {
                                return gifts.getConfigurationSection(key);
                            }
                        }
                    }
                }
            }
        }

        return gifts.getConfigurationSection("default");
    }

    public long getTotalLikes() {
        return totalLikes.get();
    }

    public long getTotalGifts() {
        return totalGifts.get();
    }

    public String getLastGift() {
        return lastGift;
    }

    public String getLastGifter() {
        return lastGifter;
    }

    public String getTopGifter() {
        return topGifter;
    }

    public String getTopGift() {
        return topGift;
    }

    private void notifyStreamers(String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String colored = ColorUtil.colorize(message);
            Set<Player> recipients = new HashSet<>(plugin.getQueueManager().getTargetStreamers());
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("streamsync.admin")) {
                    recipients.add(p);
                }
            }
            for (Player p : recipients) {
                p.sendMessage(colored);
            }
        });
    }
}
