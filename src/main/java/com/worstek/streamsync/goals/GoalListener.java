package com.worstek.streamsync.goals;

import com.worstek.streamsync.StreamSync;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class GoalListener implements Listener {

    private final StreamSync plugin;

    public GoalListener(StreamSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        GoalManager gm = plugin.getGoalManager();
        if (gm == null || !gm.isActive()) return;

        Player victim = event.getEntity();
        List<Player> streamers = plugin.getQueueManager().getTargetStreamers();
        boolean isStreamer = streamers.contains(victim) || streamers.isEmpty();

        if (isStreamer) {
            if ("SURVIVE".equalsIgnoreCase(gm.getType())) {
                gm.failGoal();
                return;
            }
            boolean dieLose = plugin.getConfigManager().getGoal().getBoolean("goal.die-lose", false);
            if (dieLose) {
                gm.failGoal();
                return;
            }
        }

        if ("KILL_PLAYERS".equalsIgnoreCase(gm.getType())) {
            Player killer = victim.getKiller();
            if (killer != null && (streamers.contains(killer) || streamers.isEmpty())) {
                gm.addProgress(1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        GoalManager gm = plugin.getGoalManager();
        if (gm == null || !gm.isActive()) return;

        if ("KILL_MOBS".equalsIgnoreCase(gm.getType())) {
            Player killer = event.getEntity().getKiller();
            if (killer != null && plugin.getQueueManager().getTargetStreamers().contains(killer)) {
                String targetType = gm.getTargetType();
                if ("ANY".equalsIgnoreCase(targetType) || event.getEntityType().name().equalsIgnoreCase(targetType)) {
                    gm.addProgress(1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTrapBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("StreamSync_Trap")) {
            event.setDropItems(false);
            event.setExpToDrop(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("StreamSync_Trap")) {
            return;
        }

        GoalManager gm = plugin.getGoalManager();
        if (gm == null || !gm.isActive()) return;

        if ("MINE_BLOCKS".equalsIgnoreCase(gm.getType())) {
            Player player = event.getPlayer();
            List<Player> streamers = plugin.getQueueManager().getTargetStreamers();
            if (streamers.contains(player) || streamers.isEmpty()) {
                if (isMatchingBlock(gm.getTargetType(), event.getBlock().getType().name())) {
                    gm.addProgress(1);
                }
            }
        }
    }

    private boolean isMatchingBlock(String targetType, String brokenBlock) {
        if ("ANY".equalsIgnoreCase(targetType)) return true;
        if (targetType.equalsIgnoreCase(brokenBlock)) return true;

        if (targetType.endsWith("_ORE") && brokenBlock.endsWith("_ORE")) {
            String baseTarget = targetType.replace("DEEPSLATE_", "");
            String baseBroken = brokenBlock.replace("DEEPSLATE_", "");
            if (baseTarget.equalsIgnoreCase(baseBroken)) return true;
        }

        if (targetType.contains("LOG") && brokenBlock.contains("LOG")) {
            String baseTarget = targetType.replace("STRIPPED_", "");
            String baseBroken = brokenBlock.replace("STRIPPED_", "");
            if (baseTarget.equalsIgnoreCase(baseBroken)) return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GoalManager gm = plugin.getGoalManager();
        if (gm != null && gm.isActive() && gm.getBossBar() != null) {
            gm.getBossBar().addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        GoalManager gm = plugin.getGoalManager();
        if (gm != null && gm.getBossBar() != null) {
            gm.getBossBar().removePlayer(event.getPlayer());
        }
    }
}
