package com.worstek.streamsync.placeholders;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.goals.GoalManager;
import com.worstek.streamsync.platforms.tiktok.TikTokConnector;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class StreamSyncExpansion extends PlaceholderExpansion {

    private final StreamSync plugin;

    public StreamSyncExpansion(StreamSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "streamsync";
    }

    @Override
    public String getAuthor() {
        return "Worstek";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return "";

        String p = params.toLowerCase();
        TikTokConnector tt = plugin.getTikTokConnector();
        GoalManager gm = plugin.getGoalManager();

        switch (p) {
            case "status":
                return tt.isConnected() ? "ONLINE" : "OFFLINE";

            case "status_colored":
                return tt.isConnected() ? "&aONLINE" : "&#FC4E4EOFFLINE";

            case "account":
            case "user":
            case "tiktok_user":
                return tt.getCurrentUsername().isEmpty() ? "-" : tt.getCurrentUsername();

            case "streamer": {
                Player s = plugin.getQueueManager().getTargetStreamer();
                return (s != null) ? s.getName() : "-";
            }

            case "likes":
            case "total_likes":
                return String.valueOf(tt.getTotalLikes());

            case "gifts":
            case "total_gifts":
                return String.valueOf(tt.getTotalGifts());

            case "last_gift":
                return tt.getLastGift();

            case "last_gifter":
                return tt.getLastGifter();

            case "top_gifter":
                return tt.getTopGifter();

            case "top_gift":
                return tt.getTopGift();

            case "goal_active":
                return gm.isActive() ? "true" : "false";

            case "goal_type":
                return gm.isActive() ? gm.getType() : "-";

            case "goal_target":
                return gm.isActive() ? String.valueOf(gm.getTarget()) : "-";

            case "goal_current":
                return gm.isActive() ? String.valueOf(gm.getCurrent()) : "-";

            case "goal_progress":
                return gm.isActive() ? (gm.getProgressPercent() + "%") : "0%";

            case "goal_time":
                return gm.isActive() ? gm.formatTime(gm.getCurrent()) : "0s";

            case "goal_time_raw":
                return gm.isActive() ? String.valueOf(gm.getCurrent()) : "0";

            case "goal_target_type":
                return gm.isActive() ? gm.getTargetType() : "-";

            default:
                return null;
        }
    }
}