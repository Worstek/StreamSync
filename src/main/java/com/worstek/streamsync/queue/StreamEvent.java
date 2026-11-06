package com.worstek.streamsync.queue;

import org.bukkit.configuration.ConfigurationSection;

public class StreamEvent {

    public enum EventType {
        TIKTOK_GIFT,
        TIKTOK_LIKE,
        TIKTOK_FOLLOW,
        TIKTOK_COMMENT,
        CUSTOM
    }

    private final EventType type;
    private final String username;
    private final String giftName;
    private final double amount;
    private final String message;
    private final ConfigurationSection config;

    public StreamEvent(EventType type, String username, String giftName, double amount, String message, ConfigurationSection config) {
        this.type = type;
        this.username = (username == null || username.trim().isEmpty()) ? "Anonymous" : username;
        this.giftName = giftName != null ? giftName : "";
        this.amount = amount;
        this.message = message != null ? message : "";
        this.config = config;
    }

    public StreamEvent(EventType type, String username, String giftName, double amount, ConfigurationSection config) {
        this(type, username, giftName, amount, "", config);
    }

    public EventType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getGiftName() {
        return giftName;
    }

    public double getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public ConfigurationSection getConfig() {
        return config;
    }
}