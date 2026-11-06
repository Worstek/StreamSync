package com.worstek.streamsync.queue;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.actions.ActionExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class EventQueueManager {

    private final StreamSync plugin;
    private final Set<String> streamerNames = new LinkedHashSet<>();

    public EventQueueManager(StreamSync plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadStreamers();
    }

    public void stop() {
    }

    public void loadStreamers() {
        streamerNames.clear();
        List<String> list = plugin.getConfig().getStringList("streamers");
        if (list != null && !list.isEmpty()) {
            for (String s : list) {
                if (s != null && !s.trim().isEmpty()) {
                    streamerNames.add(s.trim());
                }
            }
        } else {
            String single = plugin.getConfig().getString("streamer", "");
            if (!single.isEmpty() && !"auto".equalsIgnoreCase(single)) {
                streamerNames.add(single.trim());
            }
        }
    }

    public boolean addStreamer(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String clean = name.trim();
        for (String s : streamerNames) {
            if (s.equalsIgnoreCase(clean)) {
                return false;
            }
        }
        streamerNames.add(clean);
        saveStreamers();
        return true;
    }

    public boolean removeStreamer(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String toRemove = null;
        for (String s : streamerNames) {
            if (s.equalsIgnoreCase(name.trim())) {
                toRemove = s;
                break;
            }
        }
        if (toRemove != null) {
            streamerNames.remove(toRemove);
            saveStreamers();
            return true;
        }
        return false;
    }

    private void saveStreamers() {
        plugin.getConfig().set("streamers", new ArrayList<>(streamerNames));
        plugin.saveConfig();
    }

    public List<String> getStreamerNames() {
        return new ArrayList<>(streamerNames);
    }

    public List<Player> getTargetStreamers() {
        List<Player> targets = new ArrayList<>();
        if (!streamerNames.isEmpty()) {
            for (String name : streamerNames) {
                Player p = Bukkit.getPlayerExact(name);
                if (p != null && p.isOnline()) {
                    targets.add(p);
                }
            }
        }
        if (targets.isEmpty()) {
            Player fallback = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (fallback != null) {
                targets.add(fallback);
            }
        }
        return targets;
    }

    public Player getTargetStreamer() {
        List<Player> targets = getTargetStreamers();
        return targets.isEmpty() ? null : targets.get(0);
    }

    public void setStreamer(Player player) {
        if (player != null) {
            addStreamer(player.getName());
        }
    }

    public void addEvent(StreamEvent event) {
        if (event == null || event.getConfig() == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Player> targets = getTargetStreamers();
            String action = event.getConfig().getString("action", "").toUpperCase();
            boolean isGlobalGoalAction = action.startsWith("GOAL_");

            if (isGlobalGoalAction) {
                Player primary = targets.isEmpty() ? Bukkit.getOnlinePlayers().stream().findFirst().orElse(null) : targets.get(0);
                ActionExecutor.execute(plugin, primary, event);
            } else {
                for (Player streamer : targets) {
                    ActionExecutor.execute(plugin, streamer, event);
                }
            }
        });
    }

    public void clearQueue() {
    }

    public int getQueueSize() {
        return 0;
    }
}