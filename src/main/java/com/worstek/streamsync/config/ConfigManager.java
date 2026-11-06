package com.worstek.streamsync.config;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final StreamSync plugin;
    private FileConfiguration actionsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration goalConfig;
    private File actionsFile;
    private File messagesFile;
    private File goalFile;

    public ConfigManager(StreamSync plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        actionsFile = new File(plugin.getDataFolder(), "actions.yml");
        if (!actionsFile.exists()) {
            plugin.saveResource("actions.yml", false);
        }
        actionsConfig = loadYaml(actionsFile);

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = loadYaml(messagesFile);

        goalFile = new File(plugin.getDataFolder(), "goal.yml");
        if (!goalFile.exists()) {
            plugin.saveResource("goal.yml", false);
        }
        goalConfig = loadYaml(goalFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        actionsConfig = loadYaml(actionsFile);
        messagesConfig = loadYaml(messagesFile);
        goalConfig = loadYaml(goalFile);
    }

    private YamlConfiguration loadYaml(File file) {
        YamlConfiguration config = new YamlConfiguration();
        if (file != null && file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                config.load(reader);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load " + file.getName() + " with UTF-8: " + e.getMessage());
            }
        }
        return config;
    }

    public FileConfiguration getActions() {
        if (actionsConfig == null) loadConfigs();
        return actionsConfig;
    }

    public FileConfiguration getMessages() {
        if (messagesConfig == null) loadConfigs();
        return messagesConfig;
    }

    public FileConfiguration getGoal() {
        if (goalConfig == null) loadConfigs();
        return goalConfig;
    }

    public void saveGoal() {
        if (goalConfig != null && goalFile != null) {
            try {
                goalConfig.save(goalFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save goal.yml: " + e.getMessage());
            }
        }
    }

    public void saveActions() {
        if (actionsConfig != null && actionsFile != null) {
            try {
                actionsConfig.save(actionsFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save actions.yml: " + e.getMessage());
            }
        }
    }

    public String getMessage(String path, String def) {
        String msg = getMessages().getString(path, def);
        String prefix = getMessages().getString("prefix", "&#74ACF6● StreamSync&8:");
        return ColorUtil.colorize(msg.replace("%prefix%", prefix));
    }
}
