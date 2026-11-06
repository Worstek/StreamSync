package com.worstek.streamsync.actions;

import com.worstek.streamsync.StreamSync;
import com.worstek.streamsync.queue.StreamEvent;
import com.worstek.streamsync.utils.ColorUtil;
import com.worstek.streamsync.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ActionExecutor {

    private static final Random random = new Random();

    private static final String[] DEFAULT_MOBS = {
            "CREEPER", "ZOMBIE", "SKELETON", "SPIDER", "CAVE_SPIDER",
            "ENDERMAN", "WITCH", "PIGLIN", "BLAZE", "SLIME", "MAGMA_CUBE",
            "COW", "SHEEP", "PIG", "CHICKEN", "VILLAGER", "IRON_GOLEM",
            "WOLF", "LLAMA", "FOX", "BEE", "PHANTOM", "DROWNED", "HUSK", "STRAY"
    };

    private static final String[] DEFAULT_EFFECTS = {
            "SPEED", "SLOW", "FAST_DIGGING", "SLOW_DIGGING", "INCREASE_DAMAGE",
            "HEAL", "HARM", "JUMP", "CONFUSION", "REGENERATION", "DAMAGE_RESISTANCE",
            "FIRE_RESISTANCE", "WATER_BREATHING", "INVISIBILITY", "BLINDNESS",
            "NIGHT_VISION", "HUNGER", "WEAKNESS", "POISON", "WITHER", "GLOWING",
            "LEVITATION", "SLOW_FALLING", "DARKNESS"
    };

    private static final String[] DEFAULT_ITEMS = {
            "DIAMOND", "EMERALD", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE",
            "IRON_INGOT", "GOLD_INGOT", "COOKED_BEEF", "BREAD", "APPLE",
            "ENDER_PEARL", "BOW", "ARROW", "TOTEM_OF_UNDYING", "TORCH",
            "TNT", "FIREWORK_ROCKET", "SHIELD", "DIAMOND_SWORD", "GOLDEN_CARROT"
    };

    private static final String[] DEFAULT_TRAP_BLOCKS = {
            "COBWEB", "OBSIDIAN", "SLIME_BLOCK", "HONEY_BLOCK", "ICE",
            "PACKED_ICE", "BLUE_ICE", "GLASS", "TINTED_GLASS", "SAND",
            "GRAVEL", "DIRT", "IRON_BARS"
    };

    private static final String[] DEFAULT_SCARY_SOUNDS = {
            "ENTITY_ENDER_DRAGON_GROWL", "ENTITY_WARDEN_ROAR", "ENTITY_CREEPER_PRIMED",
            "ENTITY_GHAST_SCREAM", "ENTITY_ELDER_GUARDIAN_CURSE", "ENTITY_WITHER_SPAWN",
            "ENTITY_LIGHTNING_BOLT_THUNDER"
    };

    private static final String[] DEFAULT_SOUNDS = {
            "ENTITY_PLAYER_LEVELUP", "ENTITY_EXPERIENCE_ORB_PICKUP", "BLOCK_NOTE_BLOCK_PLING",
            "BLOCK_BELL_USE", "ENTITY_CREEPER_PRIMED", "ENTITY_FIREWORK_ROCKET_BLAST",
            "UI_TOAST_CHALLENGE_COMPLETE", "ENTITY_ENDERMAN_TELEPORT", "ENTITY_VILLAGER_YES"
    };

    public static void execute(StreamSync plugin, Player player, StreamEvent event) {
        if (player == null || !player.isOnline()) return;

        ConfigurationSection cfg = event.getConfig();
        if (cfg == null) return;

        if (cfg.isList("actions")) {
            List<Map<?, ?>> list = cfg.getMapList("actions");
            if (list != null && !list.isEmpty()) {
                for (Map<?, ?> map : list) {
                    MemoryConfiguration subCfg = new MemoryConfiguration();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        subCfg.set(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    execute(plugin, player, new StreamEvent(event.getType(), event.getUsername(), event.getGiftName(), event.getAmount(), event.getMessage(), subCfg));
                }
                return;
            }
        }

        String action = cfg.getString("action", "NONE").toUpperCase();

        String context = "";
        if ("SPAWN_MOB".equalsIgnoreCase(action)) {
            context = resolveString(cfg, "mob", "ZOMBIE", DEFAULT_MOBS);
        } else if ("EFFECT".equalsIgnoreCase(action)) {
            context = resolveString(cfg, "effect", "BLINDNESS", DEFAULT_EFFECTS);
        } else if ("ITEM_GIVE".equalsIgnoreCase(action) || "GIVE_ITEM".equalsIgnoreCase(action)) {
            context = resolveString(cfg, "item", "GOLDEN_APPLE", DEFAULT_ITEMS);
        } else if ("TRAP".equalsIgnoreCase(action)) {
            context = resolveString(cfg, "block", "COBWEB", DEFAULT_TRAP_BLOCKS);
        }

        String soundName = cfg.getString("sound", null);
        if (soundName != null && !soundName.isEmpty() && !"NONE".equalsIgnoreCase(soundName)) {
            if ("RANDOM".equalsIgnoreCase(soundName)) {
                List<String> pool = cfg.getStringList("sound-pool");
                if (pool == null || pool.isEmpty()) {
                    pool = Arrays.asList(DEFAULT_SOUNDS);
                }
                soundName = pool.get(random.nextInt(pool.size()));
            }
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                float volume = (float) resolveDouble(cfg, "volume", 1.0, 0.5, 2.0);
                float pitch = (float) resolveDouble(cfg, "pitch", 1.0, 0.5, 2.0);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception ignored) {}
        }

        String title = cfg.getString("title", "");
        String subtitle = cfg.getString("subtitle", "");

        if (!title.isEmpty() || !subtitle.isEmpty()) {
            player.sendTitle(format(title, player, event, context), format(subtitle, player, event, context), 10, 40, 10);
        }

        String chatMsg = cfg.getString("chat", "");
        if (!chatMsg.isEmpty()) {
            String prefix = plugin.getConfigManager().getMessages().getString("prefix", "&#74ACF6● &fStreamSync &8» ");
            player.sendMessage(ColorUtil.colorize(prefix + format(chatMsg, player, event, context)));
        }

        Location loc = player.getLocation();

        switch (action) {
            case "SPAWN_MOB": {
                String rawMob = cfg.getString("mob", "ZOMBIE");
                int amount = resolveInt(cfg, "amount", 1, 1, 5);
                double spreadRadius = resolveDouble(cfg, "radius", 3.0, 1.0, 5.0);

                List<String> pool = cfg.getStringList("pool");
                if (pool == null || pool.isEmpty()) {
                    pool = Arrays.asList(DEFAULT_MOBS);
                }

                for (int i = 0; i < amount; i++) {
                    String chosenMob = rawMob;
                    if ("RANDOM".equalsIgnoreCase(chosenMob)) {
                        chosenMob = pool.get(random.nextInt(pool.size()));
                    }

                    try {
                        EntityType type = EntityType.valueOf(chosenMob.toUpperCase());
                        double ox = (random.nextDouble() - 0.5) * (spreadRadius * 2);
                        double oz = (random.nextDouble() - 0.5) * (spreadRadius * 2);
                        Location spawnLoc = loc.clone().add(ox, 0, oz);

                        Entity entity = player.getWorld().spawnEntity(spawnLoc, type);
                        if (entity instanceof LivingEntity) {
                            LivingEntity living = (LivingEntity) entity;
                            String name = cfg.getString("name", "&d@%user%");
                            living.setCustomName(format(name, player, event, chosenMob));
                            living.setCustomNameVisible(true);
                            if (entity instanceof Creeper && cfg.getBoolean("charged", false)) {
                                ((Creeper) entity).setPowered(true);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid mob: " + chosenMob);
                    }
                }
                break;
            }

            case "EFFECT": {
                String effName = resolveString(cfg, "effect", "BLINDNESS", DEFAULT_EFFECTS);
                int duration = resolveInt(cfg, "duration", 5, 3, 15) * 20;
                int amplifier = Math.max(0, resolveInt(cfg, "amplifier", 1, 1, 3) - 1);
                boolean ambient = cfg.getBoolean("ambient", false);
                boolean particles = cfg.getBoolean("particles", true);

                PotionEffectType type = PotionEffectType.getByName(effName.toUpperCase());
                if (type == null && "CONFUSION".equalsIgnoreCase(effName)) {
                    type = PotionEffectType.getByName("NAUSEA");
                }
                if (type != null) {
                    player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
                }
                break;
            }

            case "ITEM_GIVE":
            case "GIVE_ITEM": {
                String itemName = resolveString(cfg, "item", "GOLDEN_APPLE", DEFAULT_ITEMS);
                int amount = resolveInt(cfg, "amount", 1, 1, 5);
                try {
                    Material mat = Material.valueOf(itemName.toUpperCase());
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, amount));
                    if (!leftover.isEmpty() && cfg.getBoolean("drop-if-full", true)) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid item: " + itemName);
                }
                break;
            }

            case "TRAP": {
                String blockMatName = resolveString(cfg, "block", "COBWEB", DEFAULT_TRAP_BLOCKS);
                int radius = resolveInt(cfg, "radius", 1, 1, 3);
                int height = resolveInt(cfg, "height", 2, 1, 4);
                int duration = resolveInt(cfg, "duration", 5, 3, 10);
                boolean fillInside = cfg.getBoolean("fill-inside", false);

                try {
                    Material trapMat = Material.valueOf(blockMatName.toUpperCase());
                    List<Block> changedBlocks = new ArrayList<>();
                    Location base = player.getLocation().getBlock().getLocation();

                    for (int x = -radius; x <= radius; x++) {
                        for (int y = 0; y <= height; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                boolean isEdge = (x == -radius || x == radius || z == -radius || z == radius || y == height);
                                if (fillInside || isEdge) {
                                    Block b = base.clone().add(x, y, z).getBlock();
                                    if (b.getType().isAir()) {
                                        b.setType(trapMat);
                                        b.setMetadata("StreamSync_Trap", new FixedMetadataValue(plugin, true));
                                        changedBlocks.add(b);
                                    }
                                }
                            }
                        }
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Block b : changedBlocks) {
                            if (b.getType() == trapMat) {
                                b.removeMetadata("StreamSync_Trap", plugin);
                                b.setType(Material.AIR);
                            }
                        }
                    }, duration * 20L);

                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid trap block: " + blockMatName);
                }
                break;
            }

            case "HOTBAR_SCRAMBLE": {
                ItemStack[] hotbar = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    hotbar[i] = player.getInventory().getItem(i);
                }
                List<ItemStack> list = new ArrayList<>(Arrays.asList(hotbar));
                Collections.shuffle(list);
                for (int i = 0; i < 9; i++) {
                    player.getInventory().setItem(i, list.get(i));
                }
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                break;
            }

            case "FREEZE": {
                int duration = resolveInt(cfg, "duration", 3, 2, 6);
                PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, duration * 20, 255, false, false);
                PotionEffect jumpLock = new PotionEffect(PotionEffectType.JUMP, duration * 20, 128, false, false);
                player.addPotionEffect(slowness);
                player.addPotionEffect(jumpLock);
                break;
            }

            case "SLAP": {
                double power = resolveDouble(cfg, "power", 1.5, 0.8, 3.0);
                double angle = random.nextDouble() * 2 * Math.PI;
                double vx = Math.cos(angle) * power;
                double vz = Math.sin(angle) * power;
                player.setVelocity(new Vector(vx, 0.4, vz));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 1.0f);
                break;
            }

            case "JUMPSCARE_SOUND": {
                String roar = resolveString(cfg, "sound-name", cfg.getString("sound", "ENTITY_ENDER_DRAGON_GROWL"), DEFAULT_SCARY_SOUNDS);
                try {
                    Sound s = Sound.valueOf(roar.toUpperCase());
                    player.playSound(player.getLocation(), s, 2.0f, 0.5f);
                } catch (Exception e) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                }
                break;
            }

            case "LAUNCH_UP": {
                double power = resolveDouble(cfg, "power", 1.2, 0.8, 2.5);
                player.setVelocity(new Vector(0, power, 0));
                break;
            }

            case "DROP_HAND": {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                    player.getInventory().setItemInMainHand(null);
                }
                break;
            }

            case "CLEAR_HAND": {
                player.getInventory().setItemInMainHand(null);
                break;
            }

            case "LIGHTNING": {
                player.getWorld().strikeLightning(player.getLocation());
                break;
            }

            case "EXPLOSION": {
                float power = (float) resolveDouble(cfg, "power", 2.0, 1.0, 4.0);
                boolean breakBlocks = cfg.getBoolean("break-blocks", false);
                boolean setFire = cfg.getBoolean("set-fire", false);
                player.getWorld().createExplosion(player.getLocation(), power, setFire, breakBlocks);
                break;
            }

            case "IGNITE": {
                int duration = resolveInt(cfg, "duration", 5, 2, 8);
                player.setFireTicks(duration * 20);
                break;
            }

            case "SPIN": {
                String rawYaw = cfg.getString("yaw", "180.0");
                float yaw;
                if ("RANDOM".equalsIgnoreCase(rawYaw)) {
                    float[] angles = {90.0f, 180.0f, 270.0f, -90.0f, -180.0f};
                    yaw = angles[random.nextInt(angles.length)];
                } else {
                    yaw = (float) resolveDouble(cfg, "yaw", 180.0, -180.0, 180.0);
                }
                Location spinLoc = player.getLocation();
                spinLoc.setYaw(spinLoc.getYaw() + yaw);
                player.teleport(spinLoc);
                break;
            }

            case "FAKE_HISS": {
                player.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.5f, 0.5f);
                break;
            }

            case "RANDOM_EFFECT": {
                List<String> pool = cfg.getStringList("pool");
                if (pool == null || pool.isEmpty()) {
                    pool = Arrays.asList(DEFAULT_EFFECTS);
                }
                String effName = pool.get(random.nextInt(pool.size())).toUpperCase();
                PotionEffectType type = PotionEffectType.getByName(effName);
                if (type == null && "CONFUSION".equalsIgnoreCase(effName)) {
                    type = PotionEffectType.getByName("NAUSEA");
                }
                if (type != null) {
                    int duration = resolveInt(cfg, "duration", 6, 3, 15) * 20;
                    int amplifier = Math.max(0, resolveInt(cfg, "amplifier", 1, 1, 3) - 1);
                    boolean particles = cfg.getBoolean("particles", true);
                    player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, particles));

                    String effectTitle = cfg.getString("title", "&d&lRANDOM EFFECT!");
                    String effectSub = cfg.getString("subtitle", "&#74ACF6%effect%")
                            .replace("%effect%", type.getName());
                    player.sendTitle(format(effectTitle, player, event, effName), format(effectSub, player, event, effName), 10, 35, 10);
                }
                break;
            }

            case "RANDOM_EVENT": {
                List<String> pool = cfg.getStringList("pool");
                ConfigurationSection triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers.triggers");
                if (triggers == null) {
                    triggers = plugin.getConfigManager().getActions().getConfigurationSection("chat-triggers");
                }

                ConfigurationSection chosenCfg = null;
                String chosenKey = "";

                if (pool != null && !pool.isEmpty()) {
                    chosenKey = pool.get(random.nextInt(pool.size()));
                    if (triggers != null && triggers.isConfigurationSection(chosenKey)) {
                        chosenCfg = triggers.getConfigurationSection(chosenKey);
                    }
                } else if (triggers != null) {
                    List<String> keys = new ArrayList<>(triggers.getKeys(false));
                    keys.remove("enabled");
                    keys.remove(event.getGiftName());
                    keys.remove("random");
                    keys.remove("!random");
                    if (!keys.isEmpty()) {
                        chosenKey = keys.get(random.nextInt(keys.size()));
                        chosenCfg = triggers.getConfigurationSection(chosenKey);
                    }
                }

                if (chosenCfg != null) {
                    StreamEvent delegated = new StreamEvent(event.getType(), event.getUsername(), chosenKey, event.getAmount(), event.getMessage(), chosenCfg);
                    ActionExecutor.execute(plugin, player, delegated);
                }
                break;
            }

            case "ANVIL_FALL": {
                Location anvilLoc = player.getLocation().clone().add(0, 3.5, 0);
                try {
                    player.getWorld().spawnFallingBlock(anvilLoc, Material.ANVIL.createBlockData());
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to spawn falling anvil: " + e.getMessage());
                }
                break;
            }

            case "DROP_RANDOM": {
                List<Integer> filledSlots = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    ItemStack it = player.getInventory().getItem(i);
                    if (it != null && it.getType() != Material.AIR) {
                        filledSlots.add(i);
                    }
                }
                if (!filledSlots.isEmpty()) {
                    int slot = filledSlots.get(random.nextInt(filledSlots.size()));
                    ItemStack dropped = player.getInventory().getItem(slot);
                    if (dropped != null) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped.clone());
                        player.getInventory().setItem(slot, null);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                    }
                }
                break;
            }

            case "LOOK": {
                String dir = cfg.getString("direction", "SKY").toUpperCase();
                Location targetLook = player.getLocation();
                if ("RANDOM".equalsIgnoreCase(dir)) {
                    dir = random.nextBoolean() ? "SKY" : "GROUND";
                }
                if ("GROUND".equalsIgnoreCase(dir) || "DOWN".equalsIgnoreCase(dir)) {
                    targetLook.setPitch(90.0f);
                } else {
                    targetLook.setPitch(-90.0f);
                }
                player.teleport(targetLook);
                break;
            }

            case "LOOK_SKY": {
                Location skyLoc = player.getLocation();
                skyLoc.setPitch(-90.0f);
                player.teleport(skyLoc);
                break;
            }

            case "LOOK_GROUND": {
                Location groundLoc = player.getLocation();
                groundLoc.setPitch(90.0f);
                player.teleport(groundLoc);
                break;
            }

            case "COMMAND": {
                List<String> cmds = new ArrayList<>();
                if (cfg.isList("commands")) {
                    cmds.addAll(cfg.getStringList("commands"));
                } else {
                    String single = cfg.getString("command", "");
                    if (!single.isEmpty()) cmds.add(single);
                }
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), format(cmd, player, event, context));
                }
                break;
            }

            case "GOAL_ADDTIME": {
                String timeStr = cfg.getString("time", String.valueOf(cfg.getLong("seconds", 30)));
                long sec;
                if ("RANDOM".equalsIgnoreCase(timeStr)) {
                    List<String> pool = cfg.getStringList("pool");
                    if (pool != null && !pool.isEmpty()) {
                        timeStr = pool.get(random.nextInt(pool.size()));
                        sec = TimeUtil.parseTimeSeconds(timeStr);
                    } else {
                        long[] rndTimes = {15, 30, 45, 60, 90, 120, 180, 300};
                        sec = rndTimes[random.nextInt(rndTimes.length)];
                    }
                } else {
                    sec = TimeUtil.parseTimeSeconds(timeStr);
                }
                if (sec > 0) {
                    plugin.getGoalManager().addTime(sec);
                }
                break;
            }

            case "GOAL_REMOVETIME": {
                String timeStr = cfg.getString("time", String.valueOf(cfg.getLong("seconds", 30)));
                long sec;
                if ("RANDOM".equalsIgnoreCase(timeStr)) {
                    List<String> pool = cfg.getStringList("pool");
                    if (pool != null && !pool.isEmpty()) {
                        timeStr = pool.get(random.nextInt(pool.size()));
                        sec = TimeUtil.parseTimeSeconds(timeStr);
                    } else {
                        long[] rndTimes = {15, 30, 45, 60, 90, 120, 180, 300};
                        sec = rndTimes[random.nextInt(rndTimes.length)];
                    }
                } else {
                    sec = TimeUtil.parseTimeSeconds(timeStr);
                }
                if (sec > 0) {
                    plugin.getGoalManager().removeTime(sec);
                }
                break;
            }

            case "GOAL_WIN": {
                plugin.getGoalManager().completeGoal();
                break;
            }

            case "GOAL_LOSE": {
                plugin.getGoalManager().failGoal();
                break;
            }

            case "HEART": {
                String mode = cfg.getString("mode", cfg.getString("type", "GIVE")).toUpperCase();
                if ("REMOVE".equalsIgnoreCase(mode) || "TAKE".equalsIgnoreCase(mode)) {
                    executeHeartRemove(player, cfg);
                } else if ("REGEN".equalsIgnoreCase(mode) || "REGENERATE".equalsIgnoreCase(mode) || "HEAL".equalsIgnoreCase(mode)) {
                    executeHeartRegenerate(player, cfg);
                } else {
                    executeHeartGive(player, cfg);
                }
                break;
            }

            case "HEART_GIVE": {
                executeHeartGive(player, cfg);
                break;
            }

            case "HEART_REMOVE": {
                executeHeartRemove(player, cfg);
                break;
            }

            case "HEART_REGENERATE": {
                executeHeartRegenerate(player, cfg);
                break;
            }
        }
    }

    private static void executeHeartGive(Player player, ConfigurationSection cfg) {
        double hearts = Math.max(0.5, resolveDouble(cfg, "amount", 1.0, 1.0, 3.0));
        double addHp = hearts * 2.0;
        Attribute attr = getHealthAttribute();
        if (attr != null && player.getAttribute(attr) != null) {
            double currentMax = player.getAttribute(attr).getBaseValue();
            double newMax = currentMax + addHp;
            player.getAttribute(attr).setBaseValue(newMax);
            player.setHealth(Math.min(newMax, player.getHealth() + addHp));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }

    private static void executeHeartRemove(Player player, ConfigurationSection cfg) {
        double hearts = Math.max(0.5, resolveDouble(cfg, "amount", 1.0, 1.0, 3.0));
        double subHp = hearts * 2.0;
        Attribute attr = getHealthAttribute();
        if (attr != null && player.getAttribute(attr) != null) {
            double currentMax = player.getAttribute(attr).getBaseValue();
            double newMax = Math.max(2.0, currentMax - subHp);
            player.getAttribute(attr).setBaseValue(newMax);
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }
    }

    private static void executeHeartRegenerate(Player player, ConfigurationSection cfg) {
        double hearts = Math.max(0.5, resolveDouble(cfg, "amount", 1.0, 1.0, 3.0));
        double healHp = hearts * 2.0;
        Attribute attr = getHealthAttribute();
        double maxHp = 20.0;
        if (attr != null && player.getAttribute(attr) != null) {
            maxHp = player.getAttribute(attr).getBaseValue();
        }
        double newHp = Math.min(maxHp, player.getHealth() + healHp);
        player.setHealth(newHp);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
    }

    private static String resolveString(ConfigurationSection cfg, String key, String defaultVal, String[] defaultPool) {
        String val = cfg.getString(key, defaultVal);
        if ("RANDOM".equalsIgnoreCase(val)) {
            List<String> pool = cfg.getStringList("pool");
            if (pool != null && !pool.isEmpty()) {
                return pool.get(random.nextInt(pool.size()));
            }
            if (defaultPool != null && defaultPool.length > 0) {
                return defaultPool[random.nextInt(defaultPool.length)];
            }
        }
        return val;
    }

    private static int resolveInt(ConfigurationSection cfg, String key, int defaultVal, int minRnd, int maxRnd) {
        String raw = cfg.getString(key, String.valueOf(defaultVal));
        if (raw == null || raw.trim().isEmpty()) return defaultVal;
        String s = raw.trim();

        if ("RANDOM".equalsIgnoreCase(s)) {
            return random.nextInt(Math.max(1, maxRnd - minRnd + 1)) + minRnd;
        }

        if (s.contains("-")) {
            try {
                String[] parts = s.split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                if (max < min) { int t = min; min = max; max = t; }
                return random.nextInt(Math.max(1, max - min + 1)) + min;
            } catch (Exception ignored) {}
        }

        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static double resolveDouble(ConfigurationSection cfg, String key, double defaultVal, double minRnd, double maxRnd) {
        String raw = cfg.getString(key, String.valueOf(defaultVal));
        if (raw == null || raw.trim().isEmpty()) return defaultVal;
        String s = raw.trim();

        if ("RANDOM".equalsIgnoreCase(s)) {
            return minRnd + (random.nextDouble() * (maxRnd - minRnd));
        }

        if (s.contains("-")) {
            try {
                String[] parts = s.split("-");
                double min = Double.parseDouble(parts[0].trim());
                double max = Double.parseDouble(parts[1].trim());
                if (max < min) { double t = min; min = max; max = t; }
                return min + (random.nextDouble() * (max - min));
            } catch (Exception ignored) {}
        }

        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static Attribute getHealthAttribute() {
        try {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (Exception e) {
            try {
                return Attribute.valueOf("MAX_HEALTH");
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String format(String str, Player player, StreamEvent event, String context) {
        if (str == null) return "";
        String text = str
                .replace("%user%", event.getUsername())
                .replace("%gift%", event.getGiftName())
                .replace("%amount%", String.valueOf((int) event.getAmount()))
                .replace("%message%", event.getMessage())
                .replace("%streamer%", (player != null) ? player.getName() : "")
                .replace("%mob%", context)
                .replace("%item%", context)
                .replace("%block%", context)
                .replace("%effect%", context);
        return ColorUtil.colorize(text);
    }
}