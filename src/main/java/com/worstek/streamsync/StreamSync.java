package com.worstek.streamsync;

import com.worstek.streamsync.commands.StreamSyncCommand;
import com.worstek.streamsync.config.ConfigManager;
import com.worstek.streamsync.goals.GoalListener;
import com.worstek.streamsync.goals.GoalManager;
import com.worstek.streamsync.platforms.tiktok.TikTokConnector;
import com.worstek.streamsync.platforms.webhook.WebhookServer;
import com.worstek.streamsync.queue.EventQueueManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class StreamSync extends JavaPlugin {

    private static StreamSync instance;
    private ConfigManager configManager;
    private EventQueueManager queueManager;
    private GoalManager goalManager;
    private WebhookServer webhookServer;
    private TikTokConnector tikTokConnector;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        queueManager = new EventQueueManager(this);
        queueManager.start();

        goalManager = new GoalManager(this);
        getServer().getPluginManager().registerEvents(new GoalListener(this), this);

        webhookServer = new WebhookServer(this);
        webhookServer.start();

        tikTokConnector = new TikTokConnector(this);
        tikTokConnector.start();

        StreamSyncCommand cmdHandler = new StreamSyncCommand(this);
        PluginCommand cmd = getCommand("streamsync");
        if (cmd != null) {
            cmd.setExecutor(cmdHandler);
            cmd.setTabCompleter(cmdHandler);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.worstek.streamsync.placeholders.StreamSyncExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        getLogger().info("StreamSync v" + getDescription().getVersion() + " has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (goalManager != null) {
            goalManager.stopGoal();
        }
        if (queueManager != null) {
            queueManager.stop();
        }
        if (webhookServer != null) {
            webhookServer.stop();
        }
        if (tikTokConnector != null) {
            tikTokConnector.stop();
        }

        getLogger().info("StreamSync has been disabled.");
    }

    public static StreamSync getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EventQueueManager getQueueManager() {
        return queueManager;
    }

    public GoalManager getGoalManager() {
        return goalManager;
    }

    public WebhookServer getWebhookServer() {
        return webhookServer;
    }

    public TikTokConnector getTikTokConnector() {
        return tikTokConnector;
    }
}