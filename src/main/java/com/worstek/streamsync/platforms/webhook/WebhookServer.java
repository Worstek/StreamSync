package com.worstek.streamsync.platforms.webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.queue.StreamEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebhookServer {

    private final StreamSync plugin;
    private HttpServer server;

    public WebhookServer(StreamSync plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("platforms.webhook.enabled", false)) return;

        int port = plugin.getConfig().getInt("platforms.webhook.port", 8082);

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/event", new EventHandler());
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("Webhook server started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not start Webhook server on port " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private class EventHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = new HashMap<>();

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
                                URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                    }
                }
            }

            String type = params.getOrDefault("type", "gift").toLowerCase();
            String user = params.getOrDefault("user", "Viewer");
            String giftName = params.getOrDefault("name", "Rose");
            double amount = 1.0;
            try {
                if (params.containsKey("amount")) {
                    amount = Double.parseDouble(params.get("amount"));
                }
            } catch (Exception ignored) {}

            ConfigurationSection actionConfig = null;
            StreamEvent.EventType eventType = StreamEvent.EventType.TIKTOK_GIFT;

            if ("likes".equalsIgnoreCase(type) || "like".equalsIgnoreCase(type)) {
                actionConfig = plugin.getConfigManager().getActions().getConfigurationSection("likes");
                eventType = StreamEvent.EventType.TIKTOK_LIKE;
            } else if ("follow".equalsIgnoreCase(type)) {
                actionConfig = plugin.getConfigManager().getActions().getConfigurationSection("follow");
                eventType = StreamEvent.EventType.TIKTOK_FOLLOW;
            } else {
                actionConfig = plugin.getConfigManager().getActions().getConfigurationSection("gifts." + giftName);
                if (actionConfig == null) {
                    actionConfig = plugin.getConfigManager().getActions().getConfigurationSection("gifts.default");
                }
                eventType = StreamEvent.EventType.TIKTOK_GIFT;
            }

            if (actionConfig != null) {
                plugin.getQueueManager().addEvent(new StreamEvent(eventType, user, giftName, amount, actionConfig));
            }

            String response = "{\"status\":\"ok\",\"queued\":true}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}