package org.ThienNguyen.core;

import io.lumine.mythic.core.mobs.ActiveMob;
import org.ThienNguyen.Main;
import org.ThienNguyen.utils.ItemSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import io.lumine.mythic.bukkit.MythicBukkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class DungeonManager {
    private final Map<String, BukkitRunnable> stageGuardTasks = new HashMap<>();
    private final Map<String, Location> stageSpawnLocations = new HashMap<>();
    private final Map<String, BukkitRunnable> activeTimerTasks = new HashMap<>();
    public enum DungeonState { NONE, ACTIVE, END }
    private final Map<UUID, Double> playerDamageDealt = new HashMap<>();
    private FileConfiguration config;
    private final File timerFile = new File(Main.getInstance().getDataFolder(), "dungeon-timers.yml");
    private YamlConfiguration timerConfig;
    private final Map<UUID, DungeonState> playerState = new HashMap<>();
    private final Map<String, Long> dungeonEndTime = new HashMap<>();
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private final Map<UUID, BossBar> timeBars = new HashMap<>();
    private final Map<UUID, Integer> currentStageIndex = new HashMap<>();
    private final Map<String, Location> worldCheckpoints = new HashMap<>();
    private final Map<String, org.bukkit.scheduler.BukkitTask> autoNextTasks = new HashMap<>();
    private final Map<UUID, Integer> stageProgress = new HashMap<>();
    private final Map<String, Integer> worldProgress = new HashMap<>();
    private final Map<String, Set<UUID>> dungeonPlayers = new HashMap<>();
    private final Map<String, UUID> dungeonLeader = new HashMap<>();
    private final Map<String, Integer> maxLivesCache = new HashMap<>();
    private final Map<String, Set<UUID>> dungeonPartyMembers = new HashMap<>();
    private final Map<String, Long> dungeonLastActive = new HashMap<>();
    private final Map<Integer, ItemStack> requireItems = new HashMap<>();
    private final File requireFile;
    private final Map<String, String> worldDifficulty = new HashMap<>();
    private final YamlConfiguration requireConfig;
    public DungeonManager() {
        this.requireFile = new File(Main.getInstance().getDataFolder(), "require.yml");
        if (!timerFile.exists()) {
            try {
                timerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        timerConfig = YamlConfiguration.loadConfiguration(timerFile);
        if (!requireFile.exists()) {
            try {
                Main.getInstance().getDataFolder().mkdirs();
                requireFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }
        Main.getInstance().saveDefaultConfig();
        this.config = Main.getInstance().getConfig();
        this.requireConfig = YamlConfiguration.loadConfiguration(requireFile);
        loadRequireItems();
    }
    public FileConfiguration getPluginConfig() {
        return config;
    }

    public void addPlayerToDungeon(String worldName, UUID uuid) {
        dungeonPlayers.computeIfAbsent(worldName, k -> new HashSet<>()).add(uuid);
    }

    public void setDungeonLeader(String worldName, UUID leaderUuid) {
        dungeonLeader.put(worldName, leaderUuid);
    }
    public void updateDungeonLastActive(String worldName) {
        dungeonLastActive.put(worldName, System.currentTimeMillis());
    }
    public void setDungeonPartyMembers(String worldName, Set<UUID> members) {
        dungeonPartyMembers.put(worldName, new HashSet<>(members));
    }

    public Set<UUID> getDungeonPartyMembers(String worldName) {
        return dungeonPartyMembers.getOrDefault(worldName, new HashSet<>());
    }

    public Set<UUID> getPlayersInDungeon(String worldName) {
        return dungeonPlayers.getOrDefault(worldName, new HashSet<>());
    }



    public DungeonState getPlayerState(Player player) {
        return playerState.getOrDefault(player.getUniqueId(), DungeonState.NONE);
    }
    public boolean isCommandRestrictionEnabled() {
        return getPluginConfig().getBoolean("dungeon-command-restriction.enabled", true);
    }

    public List<String> getBlockedCommands() {
        return getPluginConfig().getStringList("dungeon-command-restriction.blocked-commands");
    }
    public void setPlayerState(Player player, DungeonState state) {
        playerState.put(player.getUniqueId(), state);
    }



    public int getDeathCount(Player player) {
        return deathCount.getOrDefault(player.getUniqueId(), 0);
    }

    public void incrementDeath(Player player) {
        UUID uuid = player.getUniqueId();
        deathCount.put(uuid, getDeathCount(player) + 1);
    }

    public int getCurrentStageIndex(Player player) {
        return currentStageIndex.getOrDefault(player.getUniqueId(), 1);
    }

    public void setCurrentStageIndex(Player player, int index) {
        currentStageIndex.put(player.getUniqueId(), index);
    }
    public void setStageProgress(Player player, int progress) {
        stageProgress.put(player.getUniqueId(), progress);
    }

    public Map<Integer, ItemStack> getRequireItems() {
        return new HashMap<>(requireItems);
    }

    public void reloadRequireConfig() {
        try {
            requireConfig.load(requireFile);
            loadRequireItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    public DungeonStage getCurrentStage(Player player, String dungeonId) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int index = getCurrentStageIndex(player);
        String path = "stages." + index;

        if (!config.contains(path)) return null;

        List<DungeonStage.MobTarget> multiTargets = new ArrayList<>();
        int totalGoal = 0;
        String representativeTarget = config.getString(path + ".target");


        if (config.contains(path + ".targets")) {
            List<?> list = config.getList(path + ".targets");
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {

                    String mobId = map.containsKey("mob") ? String.valueOf(map.get("mob")) : null;
                    String loc = map.containsKey("location") ? String.valueOf(map.get("location")) : null;
                    int mGoal = map.get("goal") instanceof Integer ? (int) map.get("goal") : 1;

                    multiTargets.add(new DungeonStage.MobTarget(mobId, loc, mGoal));
                    totalGoal += mGoal;


                    if (representativeTarget == null) {
                        representativeTarget = (mobId != null) ? mobId : loc;
                    }
                }
            }
        }


        if (totalGoal == 0) {
            totalGoal = config.getInt(path + ".goal", 1);
        }


        return new DungeonStage(
                config.getString(path + ".type"),
                representativeTarget,
                totalGoal,
                config.getString(path + ".name", ""),
                config.getString(path + ".message", ""),
                config.getString(path + ".location", null),
                config.getDouble(path + ".distance", 4.0),
                config.getStringList(path + ".commands"),
                config.getBoolean(path + ".ai", false),
                config.getString(path + ".ai-target", null),
                multiTargets
        );
    }

    public void startFirstStage(Player player) {
        setCurrentStageIndex(player, 1);
        setStageProgress(player, 0);

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("temp_")) return;

        String dungeonId = worldName.split("_")[1];
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Main.getInstance().getBaseManager().setupBase(worldName, dungeonId);

        removeTimeBar(player);

        if (config.getBoolean("time-limit.enabled", false)) {
            int seconds = config.getInt("time-limit.seconds", 60);
            startTimeLimit(worldName, seconds);
        }

        String firstMsg = config.getString("message-first");
        if (firstMsg != null && !firstMsg.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', firstMsg));
        }

        DungeonStage stage = getCurrentStage(player, dungeonId);
        if (stage != null) {
            String path = "stages.1";


            if (config.getBoolean(path + ".set-checkpoint", false)) {
                Location cpLoc = null;
                if (stage.getTarget() != null) {
                    cpLoc = parseLocationSimple(player.getWorld(), stage.getTarget());
                }

                if (cpLoc == null) {
                    String spawnStr = config.getString("spawn-location");
                    cpLoc = parseLocationSimple(player.getWorld(), spawnStr);
                }

                if (cpLoc != null) {
                    setWorldCheckpoint(worldName, cpLoc);
                    String msg = Main.getInstance().getMessagesConfig()
                            .getString("dungeon.stage.checkpoint-updated", "&a&l[!] &fĐiểm hồi sinh đã được lưu tại đây.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
            }


            String title = config.getString(path + ".title");
            String subtitle = config.getString(path + ".subtitle", config.getString(path + ".message", ""));

            if (title != null) {
                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', title),
                        ChatColor.translateAlternateColorCodes('&', subtitle),
                        10, 40, 10
                );
            }


            if (stage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) {
                int delaySeconds = config.getInt(path + ".delay", 0);

                if (delaySeconds > 0) {

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && player.getWorld().getName().equals(worldName)) {
                                spawnStageMob(player, stage);
                            }
                        }
                    }.runTaskLater(Main.getInstance(), delaySeconds * 20L);
                } else {
                    spawnStageMob(player, stage);
                }
            }
        }
    }
    public Location parseLocationSimple(World world, String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            String[] parts = str.split(",");
            int idx = 0;
            try {
                Double.parseDouble(parts[0].trim());
            } catch (NumberFormatException e) {
                idx = 1;
            }
            if (parts.length < idx + 3) return null;

            double x = Double.parseDouble(parts[idx].trim());
            double y = Double.parseDouble(parts[idx + 1].trim());
            double z = Double.parseDouble(parts[idx + 2].trim());

            float yaw = 0;
            float pitch = 0;


            if (parts.length > idx + 3) {
                yaw = Float.parseFloat(parts[idx + 3].trim());
            }
            if (parts.length > idx + 4) {
                pitch = Float.parseFloat(parts[idx + 4].trim());
            }

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {

            org.bukkit.Bukkit.getLogger().warning("[Dungeon] Loi parseLocationSimple tai: " + str);
            return null;
        }
    }
    public void nextStage(Player player) {

        if (getPlayerState(player) == DungeonState.END) {
            return;
        }

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("temp_")) return;
        String dungeonId = worldName.split("_")[1];


        int currentIndex = getCurrentStageIndex(player);
        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);



        String rewardPath = "stages." + currentIndex + ".reward-item-id";
        if (config.contains(rewardPath)) {
            int itemId = config.getInt(rewardPath);
            ItemStack reward = getRequireItems().get(itemId);

            if (reward != null) {
                for (Player p : player.getWorld().getPlayers()) {

                    if (getPlayerState(p) != DungeonState.END) {
                        p.getInventory().addItem(reward.clone());


                        String rewardMsg = Main.getInstance().getMessagesConfig()
                                .getString("dungeon.stage.reward-received", "&a&l[!] &fBạn đã nhận được phần thưởng hoàn thành Stage!");
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', rewardMsg));
                    }
                }
            }
        }


        int nextIndex = currentIndex + 1;
        setCurrentStageIndex(player, nextIndex);

        cancelGuard(worldName);
        setStageProgress(worldName, 0);

        DungeonStage next = getCurrentStage(player, dungeonId);

        if (next != null) {

            String msg = next.getMessage();
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }


            if (next.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) {
                int delaySeconds = config.getInt("stages." + nextIndex + ".delay", 0);

                if (delaySeconds > 0) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() &&
                                    player.getWorld().getName().equals(worldName) &&
                                    getPlayerState(player) != DungeonState.END) {
                                spawnStageMob(player, next);
                            }
                        }
                    }.runTaskLater(Main.getInstance(), delaySeconds * 20L);
                } else {
                    spawnStageMob(player, next);
                }
            }
        } else {

            winDungeon(player);
        }
    }
    public boolean canEnterTower(Player player, String dungeonId) {

        FileConfiguration dCfg = getDungeonConfig(dungeonId);


        if (dCfg == null || !dCfg.getBoolean("is-tower-stage", false)) {
            return true;
        }


        int stageNum = dCfg.getInt("stage-number", 0);
        long lastWin = Main.getInstance().getDatabase().getLastWin(player.getUniqueId(), stageNum);


        int cooldownSeconds = Main.getInstance().getConfig().getInt("tower-settings.reset-times." + stageNum, 86400);
        long currentTime = System.currentTimeMillis();


        if (currentTime - lastWin < cooldownSeconds * 1000L) {
            long secondsLeft = (cooldownSeconds * 1000L - (currentTime - lastWin)) / 1000;


            player.sendMessage("§c[!] Bạn phải chờ §e" + formatDetailedTime((int)secondsLeft) + " §cđể đánh lại tầng này.");
            return false;
        }

        return true;
    }
    public String formatDetailedTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0 phút";

        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(" ngày ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" giờ ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append(" phút ");
        }
        if (sb.length() == 0) {
            sb.append(seconds).append(" giây");
        }

        return sb.toString().trim();
    }
    public void winDungeon(Player player) {
        World world = player.getWorld();
        if (world == null) return;

        String worldName = world.getName();
        if (!worldName.startsWith("temp_")) return;



        if (getPlayerState(player) == DungeonState.END) return;


        List<Player> playersInWorld = new ArrayList<>(world.getPlayers());
        for (Player p : playersInWorld) {
            setPlayerState(p, DungeonState.END);
            removeTimeBar(p);
        }

        String dungeonId = worldName.split("_")[1];
        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);

        String currentDifficulty = getWorldDifficulty(worldName).toUpperCase();


        if (config.getBoolean("send-damage-summary", false)) {
            sendDamageSummary(worldName);
        }


        if (config.getBoolean("damage-rewards.enabled", false)) {
            List<String> tiers = config.getStringList("damage-rewards.tiers." + currentDifficulty);
            if (tiers == null || tiers.isEmpty()) {
                tiers = config.getStringList("damage-rewards.tiers.DEFAULT");
            }

            if (tiers != null && !tiers.isEmpty()) {
                for (Player p : playersInWorld) {
                    checkAndGiveDamageRewards(p, tiers);
                }
            }
        }


        List<String> winCommands = config.getStringList("win-commands");
        int quitTimeSeconds = config.getInt("quit-time", 5);

        for (Player p : playersInWorld) {
            if (winCommands != null && !winCommands.isEmpty()) {
                executeCommands(p, winCommands);
            }

            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);


        }



        new BukkitRunnable() {
            @Override
            public void run() {
                shutdownDungeon(worldName, "WIN");
            }
        }.runTaskLater(Main.getInstance(), quitTimeSeconds * 20L);
    }

    public void spawnStageMob(Player player, DungeonStage stage) {
        if (player == null || stage == null) return;

        World world = player.getWorld();
        if (world == null) return;

        String worldName = world.getName();


        int currentIndex = getCurrentStageIndex(player);
        String spawnKey = worldName + ":spawned_stage:" + currentIndex;

        if (worldProgress.containsKey(spawnKey)) {
            return;
        }
        worldProgress.put(spawnKey, 1);


        FileConfiguration cfg = getPluginConfig();
        String mode = cfg.getString("mob-spawn-animation.mode", "instant").toLowerCase();
        long delayTicks = cfg.getLong("mob-spawn-animation.delay-ticks", 20L);
        double randomRadius = cfg.getDouble("mob-spawn-animation.random-radius", 5.0);
        int batchSize = cfg.getInt("mob-spawn-animation.batch-size", 5);

        boolean useParticle = cfg.getBoolean("mob-spawn-animation.spawn-particle.enabled", true);
        boolean useSound = cfg.getBoolean("mob-spawn-animation.spawn-sound.enabled", true);


        if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
            for (DungeonStage.MobTarget mt : stage.getMultiTargets()) {
                Location baseLoc = parseLocationInWorld(world, mt.getSpawnLocation());
                if (baseLoc == null) continue;

                spawnMobsWithAnimation(
                        mt.getMobId(),
                        baseLoc,
                        mt.getMobGoal(),
                        stage,
                        mode, delayTicks, randomRadius, batchSize,
                        useParticle, useSound
                );
            }
        }

        else {
            Location baseLoc = parseLocationInWorld(world, stage.getLocation());
            if (baseLoc == null) return;

            spawnMobsWithAnimation(
                    stage.getTarget(),
                    baseLoc,
                    stage.getGoal(),
                    stage,
                    mode, delayTicks, randomRadius, batchSize,
                    useParticle, useSound
            );
        }
        String dungeonId = player.getWorld().getName().split("_")[1];
        startStageMobGuardian(player.getWorld().getName(), stage, dungeonId);
    }
    private void spawnMobsWithAnimation(
            String mobId, Location baseLoc, int totalAmount,
            DungeonStage stage, String mode, long delayTicks,
            double randomRadius, int batchSize,
            boolean useParticle, boolean useSound
    ) {
        if (totalAmount <= 0 || baseLoc == null) return;

        if ("instant".equals(mode)) {
            for (int i = 0; i < totalAmount; i++) {
                spawnSingleMythicMob(mobId, baseLoc.clone(), stage, useParticle, useSound);
            }
            return;
        }


        new BukkitRunnable() {
            int spawned = 0;

            @Override
            public void run() {
                if (spawned >= totalAmount) {
                    cancel();
                    return;
                }

                int thisBatch = Math.min(batchSize, totalAmount - spawned);

                for (int i = 0; i < thisBatch; i++) {

                    Location spawnLoc = findSafeAirSpawnLocation(baseLoc, randomRadius);

                    spawnSingleMythicMob(mobId, spawnLoc, stage, useParticle, useSound);
                    spawned++;
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, delayTicks);
    }

    /**
     * Tìm vị trí spawn an toàn trong không khí (air), quanh baseLoc
     * - Không kẹt block/tường
     * - Không bay cao (giữ Y gốc từ config + offset nhỏ)
     * - Có mặt đất hỗ trợ dưới chân (không rơi void)
     */
    private Location findSafeAirSpawnLocation(Location center, double radius) {
        if (radius <= 0) return center.clone();

        World world = center.getWorld();
        Location attempt = center.clone();
        int maxAttempts = 35;

        for (int attemptNum = 0; attemptNum < maxAttempts; attemptNum++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = Math.random() * radius;

            attempt.setX(center.getX() + Math.cos(angle) * dist);
            attempt.setZ(center.getZ() + Math.sin(angle) * dist);


            double baseY = center.getY();
            double yOffset = -1.0 + Math.random() * 3.0;
            attempt.setY(baseY + yOffset);

            Block feetBlock = attempt.getBlock();
            Block headBlock = attempt.clone().add(0, 1.8, 0).getBlock();


            boolean isAirSafe = feetBlock.getType().isAir() && headBlock.getType().isAir() &&
                    !feetBlock.isLiquid() && feetBlock.getType() != Material.LAVA &&
                    headBlock.getType() != Material.LAVA;


            Block below = attempt.clone().subtract(0, 0.1, 0).getBlock();
            boolean hasGroundSupport = !below.getType().isAir() && !below.isLiquid();

            if (isAirSafe && hasGroundSupport) {
                return attempt;
            }
        }


        return center.clone();
    }
    private void spawnSingleMythicMob(String mobId, Location loc, DungeonStage stage,
                                      boolean useParticle, boolean useSound) {
        if (mobId == null || loc == null) return;




        ActiveMob am = MythicBukkit.inst().getMobManager().spawnMob(mobId, loc);
        if (am == null || am.getEntity() == null) return;


        if (am.getEntity().getBukkitEntity() instanceof org.bukkit.entity.LivingEntity entity) {



            applyDifficultyToEntity(entity);


            Location actualLoc = entity.getLocation();
            FileConfiguration cfg = getPluginConfig();


            if (useParticle) {
                try {
                    String particleName = cfg.getString("mob-spawn-animation.spawn-particle.type", "SMOKE_NORMAL");
                    Particle p = Particle.valueOf(particleName);
                    double offset = cfg.getDouble("mob-spawn-animation.spawn-particle.offset", 0.4);

                    actualLoc.getWorld().spawnParticle(
                            p,
                            actualLoc.clone().add(0, 1.0, 0),
                            cfg.getInt("mob-spawn-animation.spawn-particle.count", 15),
                            offset, offset, offset,
                            cfg.getDouble("mob-spawn-animation.spawn-particle.speed", 0.1)
                    );
                } catch (Exception ignored) {}
            }


            if (useSound) {
                try {
                    String soundName = cfg.getString("mob-spawn-animation.spawn-sound.sound", "ENTITY_ZOMBIE_INFECT");
                    Sound s = Sound.valueOf(soundName);
                    actualLoc.getWorld().playSound(
                            actualLoc,
                            s,
                            (float) cfg.getDouble("mob-spawn-animation.spawn-sound.volume", 0.8),
                            (float) cfg.getDouble("mob-spawn-animation.spawn-sound.pitch", 1.0)
                    );
                } catch (Exception ignored) {}
            }


            if (stage.isAiEnabled()) {
                startReturnToHomeTask(am, actualLoc, stage);
            }
        }
    }
    public void applyDifficultyToEntity(org.bukkit.entity.LivingEntity entity) {
        if (entity == null) return;
        String worldName = entity.getWorld().getName();
        if (!worldName.startsWith("temp_")) return;

        String difficultyKey = getWorldDifficulty(worldName);
        FileConfiguration mainConfig = Main.getInstance().getConfig();
        String path = "difficulty-settings." + difficultyKey + ".";

        double hpMulti = mainConfig.getDouble(path + "health-multiplier", 1.0);
        double dmgMulti = mainConfig.getDouble(path + "damage-multiplier", 1.0);
        double speedMulti = mainConfig.getDouble(path + "speed-multiplier", 1.0);


        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (entity.isDead()) return;


            if (dmgMulti != 1.0) {
                var attack = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (attack != null) attack.setBaseValue(attack.getBaseValue() * dmgMulti);
            }


            if (speedMulti != 1.0) {
                var speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(speed.getBaseValue() * speedMulti);
            }


            if (hpMulti != 1.0) {
                var maxHp = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHp != null) {
                    double newValue = maxHp.getBaseValue() * hpMulti;
                    maxHp.setBaseValue(newValue);
                    entity.setHealth(newValue);
                }
            }
        }, 1L);
    }

    public Location parseLocationInWorld(World world, String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String[] p = str.split(",");
            if (p.length < 3) return null;
            double x = Double.parseDouble(p[0].trim());
            double y = Double.parseDouble(p[1].trim());
            double z = Double.parseDouble(p[2].trim());
            return new Location(world, x, y, z);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Dungeon] Lỗi parse location: " + str + " | " + e.getMessage());
            return null;
        }
    }
    public int getMaxLives(String dungeonId) {

        if (maxLivesCache.containsKey(dungeonId)) {
            return maxLivesCache.get(dungeonId);
        }


        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        if (!dungeonFile.exists()) {
            return 3;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);


        int maxLives = config.getInt("max-lives", 3);


        maxLivesCache.put(dungeonId, maxLives);

        return maxLives;
    }





    public void failDungeon(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();
        if (!worldName.startsWith("temp_")) return;





        for (Player p : world.getPlayers()) {
            p.setGameMode(GameMode.SPECTATOR);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }


        fullCleanupDungeon(worldName);
        shutdownDungeon(worldName, "FAIL");
    }




    public boolean isAnyMemberAlive(World world) {
        if (world == null) return false;
        for (Player p : world.getPlayers()) {

            if (p.getGameMode() != GameMode.SPECTATOR) {
                return true;
            }
        }
        return false;
    }
    public void loadRequireItems() {
        requireItems.clear();
        if (requireConfig.getConfigurationSection("items") == null) return;
        for (String key : requireConfig.getConfigurationSection("items").getKeys(false)) {
            ItemStack item = ItemSerializer.fromBase64(requireConfig.getString("items." + key));
            if (item != null) {
                requireItems.put(Integer.parseInt(key), item);
            }
        }
    }

    public void saveRequireToDB(int id, String base64) {
        requireConfig.set("items." + id, base64);
        saveRequireFile();
        loadRequireItems();
    }

    public void removeRequireFromDB(int id) {
        requireConfig.set("items." + id, null);
        saveRequireFile();
        loadRequireItems();
    }

    private void saveRequireFile() {
        try {
            requireConfig.save(requireFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Location parseLocation(String str) {
        if (str == null || !str.contains(",")) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        String[] p = str.split(",");
        World w = Bukkit.getWorld(p[0].trim());
        if (w == null) w = Bukkit.getWorlds().get(0);
        return new Location(w, Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()), Double.parseDouble(p[3].trim()));
    }

    public void executeCommands(Player player, List<String> commands) {
        if (commands == null) return;
        for (String cmd : commands) {
            String finalCmd = cmd.replace("%player%", player.getName());
            if (finalCmd.startsWith("[CONSOLE] ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd.substring(10));
            } else if (finalCmd.startsWith("[OP] ")) {
                boolean wasOp = player.isOp();
                player.setOp(true);
                player.performCommand(finalCmd.substring(5));
                player.setOp(wasOp);
            }
        }
    }


    public void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> s = Files.walk(path)) {
            s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * Tìm tất cả thư mục có thể chứa world tên `worldName`.
     * Một số plugin quản lý world (WorldGuard, Multiverse...) lưu world bên
     * trong plugins/<Plugin>/worlds/ thay vì thư mục gốc server, nên không thể
     * chỉ tin vào Bukkit.getWorldContainer(). Hàm này quét tất cả nơi khả nghi
     * và trả về danh sách những thư mục thực sự tồn tại trên đĩa.
     */
    private List<File> findAllWorldFolders(String worldName) {
        List<File> candidates = new ArrayList<>();

        // 1. Thư mục gốc server (chỗ mặc định Bukkit hay dùng)
        candidates.add(new File(Bukkit.getWorldContainer(), worldName));

        // 2. plugins/WorldGuard/worlds/<worldName>
        File pluginsFolder = Main.getInstance().getDataFolder().getParentFile();
        candidates.add(new File(pluginsFolder, "WorldGuard/worlds/" + worldName));

        // 3. Phòng trường hợp world đang load thật sự nằm ở nơi khác hẳn
        //    (ví dụ multiverse custom path) - lấy luôn từ World object nếu còn tồn tại
        World loadedWorld = Bukkit.getWorld(worldName);
        if (loadedWorld != null) {
            candidates.add(loadedWorld.getWorldFolder());
        }

        List<File> found = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        for (File f : candidates) {
            try {
                String canonical = f.getCanonicalPath();
                if (f.exists() && seenPaths.add(canonical)) {
                    found.add(f);
                }
            } catch (IOException e) {
                if (f.exists() && seenPaths.add(f.getAbsolutePath())) {
                    found.add(f);
                }
            }
        }
        return found;
    }

    /**
     * Xóa world ở mọi nơi tìm thấy (server root, plugins/WorldGuard/worlds/, v.v.)
     * Trả về số thư mục đã xóa thành công.
     */
    private int deleteWorldEverywhere(String worldName) {
        List<File> folders = findAllWorldFolders(worldName);
        int deletedCount = 0;

        if (folders.isEmpty()) {
            Bukkit.getLogger().warning("[Dungeon] Không tìm thấy thư mục world nào để xóa cho: " + worldName);
            return 0;
        }

        for (File folder : folders) {
            try {
                deleteDirectory(folder.toPath());
                Bukkit.getLogger().info("[Dungeon] Đã xóa world: " + worldName + " tại " + folder.getAbsolutePath());
                deletedCount++;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Dungeon] Không thể xóa world " + worldName + " tại "
                        + folder.getAbsolutePath() + ": " + e.getMessage());
            }
        }
        return deletedCount;
    }


    public void startTimeLimit(String worldName, int seconds) {
        if (activeTimerTasks.containsKey(worldName)) return;

        long startTime;
        long durationMs;


        if (timerConfig.contains("timers." + worldName)) {
            startTime = timerConfig.getLong("timers." + worldName + ".startTime");
            durationMs = timerConfig.getLong("timers." + worldName + ".durationMs");
        } else {
            startTime = System.currentTimeMillis();
            durationMs = seconds * 1000L;
            timerConfig.set("timers." + worldName + ".startTime", startTime);
            timerConfig.set("timers." + worldName + ".durationMs", durationMs);
            saveTimerConfig();
        }

        dungeonEndTime.put(worldName, startTime + durationMs);

        final long finalDurationMs = durationMs;

        BukkitRunnable task = new BukkitRunnable() {
            private boolean isEnding = false;

            @Override
            public void run() {
                long remainingMs = getRemainingTime(worldName);
                World world = Bukkit.getWorld(worldName);


                if (remainingMs <= 0) {
                    if (!isEnding) {
                        isEnding = true;
                        dungeonEndTime.remove(worldName);

                        String dungeonId = worldName.contains("_") ? worldName.split("_")[1] : "";
                        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);

                        int failTime = config.getInt("fail-time", 5);
                        Location exitLoc = parseLocation(config.getString("exit-location"));

                        if (world != null) {
                            for (Player p : world.getPlayers()) {
                                p.setGameMode(GameMode.SPECTATOR);
                            }
                        }

                        new BukkitRunnable() {
                            int ticks = failTime * 20;
                            int current = 0;

                            @Override
                            public void run() {
                                World currentWorld = Bukkit.getWorld(worldName);
                                if (current >= ticks || currentWorld == null) {
                                    if (currentWorld != null) {
                                        for (Player p : new ArrayList<>(currentWorld.getPlayers())) {
                                            removeTimeBar(p);
                                            fullCleanupDungeon(worldName);
                                            clearWorldCheckpoint(worldName);
                                            p.setGameMode(GameMode.SURVIVAL);
                                            p.teleport(exitLoc != null ? exitLoc : Bukkit.getWorlds().get(0).getSpawnLocation());
                                            executeCommands(p, config.getStringList("quit-commands"));
                                        }
                                        Bukkit.unloadWorld(currentWorld, false);
                                    }
                                    forceDeleteWorld(worldName);
                                    fullCleanupDungeon(worldName);
                                    this.cancel();
                                    return;
                                }

                                if (currentWorld != null) {
                                    int secsLeft = (ticks - current - 1) / 20 + 1;
                                    String title = ChatColor.translateAlternateColorCodes('&', Main.getInstance().getMessagesConfig().getString("dungeon.fail.title", "&c&lTHẤT BẠI"));
                                    String subtitle = ChatColor.translateAlternateColorCodes('&', Main.getInstance().getMessagesConfig().getString("dungeon.fail.subtitle", "&fRời đi sau %time%s")).replace("%time%", String.valueOf(secsLeft));

                                    for (Player p : currentWorld.getPlayers()) {
                                        p.sendTitle(title, subtitle, 0, 21, 0);
                                        if (current % 20 == 0) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                                    }
                                }
                                current++;
                            }
                        }.runTaskTimer(Main.getInstance(), 0L, 1L);

                        this.cancel();
                        activeTimerTasks.remove(worldName);
                    }
                    return;
                }


                if (world != null && !world.getPlayers().isEmpty()) {

                    boolean showBar = Main.getInstance().getConfig().getBoolean("dungeon-settings.show-time-bossbar", true);

                    int remainingSec = (int) (remainingMs / 1000);
                    double progress = (double) remainingMs / finalDurationMs;

                    for (Player p : world.getPlayers()) {
                        if (showBar) {
                            BossBar bar = timeBars.get(p.getUniqueId());
                            if (bar == null) {
                                bar = createNewTimeBar(worldName, remainingSec);
                                timeBars.put(p.getUniqueId(), bar);
                            }
                            if (!bar.getPlayers().contains(p)) bar.addPlayer(p);

                            bar.setProgress(Math.max(0.0, Math.min(progress, 1.0)));
                            String barTemplate = ChatColor.translateAlternateColorCodes('&',
                                    Main.getInstance().getMessagesConfig().getString("dungeon.time-limit-bossbar", "&fThời gian: &e%time%"));
                            bar.setTitle(barTemplate.replace("%time%", formatTime(remainingSec)));
                        } else {

                            removeTimeBar(p);
                        }
                    }
                }
            }
        };
        task.runTaskTimer(Main.getInstance(), 0L, 20L);
        activeTimerTasks.put(worldName, task);
    }

    public void spawnStageMobWithDelay(Player player, DungeonStage stage, String dungeonId, int stageIndex) {
        if (player == null || stage == null) return;


        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);


        int delaySeconds = config.getInt("stages." + stageIndex + ".delay", 0);

        if (delaySeconds > 0) {

            String delayMsg = Main.getInstance().getMessagesConfig()
                    .getString("dungeon.stage.waiting-next", "&e&l[!] &fĐợt quái tiếp theo sẽ xuất hiện sau &b%time%s&f.")
                    .replace("%time%", String.valueOf(delaySeconds));

            player.getWorld().getPlayers().forEach(p -> p.sendMessage(ChatColor.translateAlternateColorCodes('&', delayMsg)));


            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnStageMob(player, stage);
                }
            }.runTaskLater(Main.getInstance(), delaySeconds * 20L);
        } else {

            spawnStageMob(player, stage);
        }
    }

    private void forceDeleteWorld(String worldName) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            deleteWorldEverywhere(worldName);
        }, 40L);
    }



    public BossBar createNewTimeBar(String worldName, int remainingSec) {
        FileConfiguration msg = Main.getInstance().getMessagesConfig();
        String barTemplate = ChatColor.translateAlternateColorCodes('&', msg.getString("dungeon.time-limit-bossbar", "&fThời gian: &e%time%"));
        BossBar bar = Bukkit.createBossBar(barTemplate.replace("%time%", formatTime(remainingSec)), BarColor.RED, BarStyle.SOLID);
        return bar;
    }
    private void saveTimerConfig() {
        try {
            timerConfig.save(timerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getRemainingTime(String worldName) {
        if (!timerConfig.contains("timers." + worldName + ".startTime")) return -1;

        long startTime = timerConfig.getLong("timers." + worldName + ".startTime");
        long durationMs = timerConfig.getLong("timers." + worldName + ".durationMs");

        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, durationMs - elapsed);
    }

    public String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
    public void removeTimeBar(Player player) {
        BossBar bar = timeBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
    public int getStageProgress(String worldName) {
        return worldProgress.getOrDefault(worldName, 0);
    }

    public void setStageProgress(String worldName, int progress) {
        worldProgress.put(worldName, progress);
    }
    public boolean isLastStage(Player player, String dungeonId) {

        int currentStageIndex = getCurrentStageIndex(player);


        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.getConfigurationSection("stages") == null) return false;
        int totalStages = config.getConfigurationSection("stages").getKeys(false).size();


        return currentStageIndex >= totalStages;
    }
    public FileConfiguration getDungeonConfig(String dungeonId) {

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public void shutdownDungeon(String worldName, String reason) {
        Main.getInstance().getBaseManager().cleanup(worldName);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            forceDeleteWorld(worldName);
            return;
        }

        String dungeonId = worldName.split("_")[1];
        File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);
        FileConfiguration msgConfig = Main.getInstance().getMessagesConfig();

        int waitTime = config.getInt("quit-time", 5);
        Location exitLoc = parseLocation(config.getString("exit-location"));

        String title, subtitleTemplate;
        if (reason.equalsIgnoreCase("WIN")) {
            title = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.win.title", "&a&lCHIẾN THẮNG"));
            subtitleTemplate = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.win.subtitle", "&fRời đi sau %time%s"));
        } else if (reason.equalsIgnoreCase("FAIL")) {
            title = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.fail.title", "&c&lTHẤT BẠI"));
            subtitleTemplate = ChatColor.translateAlternateColorCodes('&', msgConfig.getString("dungeon.fail.subtitle", "&7Tự động thoát sau %time%s"));
        } else {
            title = "§e§lTHÔNG BÁO";
            subtitleTemplate = "§fPhó bản kết thúc sau %time%s";
        }

        new BukkitRunnable() {
            int seconds = waitTime;

            @Override
            public void run() {
                List<Player> players = new ArrayList<>(world.getPlayers());

                if (seconds <= 0 || players.isEmpty()) {

                    for (Player p : players) {
                        removeTimeBar(p);
                        p.setGameMode(GameMode.SURVIVAL);
                        p.teleport(exitLoc != null ? exitLoc : Bukkit.getWorlds().get(0).getSpawnLocation());
                        if (reason.equalsIgnoreCase("FAIL")) {
                            executeCommands(p, config.getStringList("quit-commands"));
                        }
                    }


                    fullCleanupDungeon(worldName);


                    Bukkit.unloadWorld(world, false);


                    Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                        deleteWorldEverywhere(worldName);
                    }, 100L);

                    cancel();
                    return;
                }

                String currentSubtitle = subtitleTemplate.replace("%time%", String.valueOf(seconds));
                for (Player p : players) {
                    p.sendTitle(title, currentSubtitle, 0, 25, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                }
                seconds--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }


    private void startReturnToHomeTask(io.lumine.mythic.core.mobs.ActiveMob am, Location homeLoc, DungeonStage stage) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (am == null || am.getEntity() == null || am.getEntity().isDead()) {
                    this.cancel();
                    return;
                }

                org.bukkit.entity.Entity entity = am.getEntity().getBukkitEntity();
                if (!(entity instanceof org.bukkit.entity.Mob mob)) return;

                String worldName = mob.getWorld().getName();
                BaseManager baseManager = Main.getInstance().getBaseManager();
                boolean isDefendMode = baseManager.isBaseEnabled(worldName);


                Location targetLoc;
                if (isDefendMode) {

                    targetLoc = baseManager.getBaseLocation(worldName);
                } else {

                    String aiTargetStr = stage.getAiTarget();
                    if (aiTargetStr != null && !aiTargetStr.isEmpty()) {
                        targetLoc = parseLocationInWorld(mob.getWorld(), aiTargetStr);
                    } else {
                        targetLoc = homeLoc;
                    }
                }

                if (targetLoc == null) return;
                targetLoc = targetLoc.clone();
                targetLoc.setWorld(mob.getWorld());

                double distanceToTargetSq = mob.getLocation().distanceSquared(targetLoc);


                if (isDefendMode && distanceToTargetSq <= 64) {
                    if (mob.getTarget() != null) mob.setTarget(null);

                    if (!baseManager.isInZone(worldName, mob.getLocation())) {
                        mob.getPathfinder().moveTo(targetLoc, 1.2);
                    } else {

                        mob.swingMainHand();



                        double realDamage = am.getDamage();





                        baseManager.damageBase(worldName, realDamage);

                        mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.1);
                    }
                    return;
                }


                boolean hasPlayerNearby = mob.getWorld().getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(mob.getLocation()) <= 144
                                && p.getGameMode() == org.bukkit.GameMode.SURVIVAL);

                if (!hasPlayerNearby) {

                    if (distanceToTargetSq > 16) {
                        mob.setTarget(null);
                        mob.getPathfinder().moveTo(targetLoc, 1.0);
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 40L);
    }

    public void startStageMobGuardian(String worldName, DungeonStage stage, String dungeonId) {

        if (stageGuardTasks.containsKey(worldName)) {
            stageGuardTasks.get(worldName).cancel();
        }


        Location spawnLoc = null;
        if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
            spawnLoc = parseLocationInWorld(Bukkit.getWorld(worldName), stage.getMultiTargets().get(0).getSpawnLocation());
        } else {
            spawnLoc = parseLocationInWorld(Bukkit.getWorld(worldName), stage.getLocation());
        }
        if (spawnLoc != null) stageSpawnLocations.put(worldName, spawnLoc.clone());

        BukkitRunnable guardian = new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(worldName);
                if (world == null || world.getPlayers().isEmpty()) {
                    cancelGuard(worldName);
                    return;
                }


                if (world.getPlayers().stream().noneMatch(p -> getPlayerState(p) != DungeonState.END)) {
                    cancelGuard(worldName);
                    return;
                }

                String mobId = stage.getTarget();
                if (mobId == null || mobId.isEmpty()) return;


                boolean hasAliveMob = world.getEntities().stream()
                        .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                        .anyMatch(e -> {
                            ActiveMob am = MythicBukkit.inst().getMobManager().getMythicMobInstance(e);
                            return am != null && am.getType().getInternalName().equalsIgnoreCase(mobId);
                        });

                if (!hasAliveMob) {
                    Location respawnLoc = stageSpawnLocations.getOrDefault(worldName, world.getSpawnLocation());

                    spawnSingleMythicMob(mobId, respawnLoc.clone(), stage, false, false);


                    String msg = "&c&l[⚠] &fMob đã bị mất (despawn). Đã respawn lại!";
                    world.getPlayers().forEach(p -> p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg)));
                }
            }
        };

        guardian.runTaskTimer(Main.getInstance(), 200L, 200L);
        stageGuardTasks.put(worldName, guardian);
    }

    private void cancelGuard(String worldName) {
        BukkitRunnable task = stageGuardTasks.remove(worldName);
        if (task != null) task.cancel();
        stageSpawnLocations.remove(worldName);
    }
    public void fullCleanupDungeon(String worldName) {

        dungeonLeader.remove(worldName);
        worldDifficulty.remove(worldName);
        dungeonPartyMembers.remove(worldName);
        dungeonPlayers.remove(worldName);
        dungeonLastActive.remove(worldName);
        worldProgress.keySet().removeIf(key -> key.startsWith(worldName));
        cancelGuard(worldName);

        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            for (org.bukkit.entity.Player p : world.getPlayers()) {
                java.util.UUID uuid = p.getUniqueId();


                org.ThienNguyen.core.DungeonScoreboard.removeScoreboard(p);

                playerDamageDealt.remove(uuid);
                deathCount.remove(uuid);
                currentStageIndex.remove(uuid);
                stageProgress.remove(uuid);
                playerState.remove(uuid);
                removeTimeBar(p);
            }
        }


        timerConfig.set("timers." + worldName, null);
        saveTimerConfig();

        org.bukkit.scheduler.BukkitRunnable task = activeTimerTasks.remove(worldName);
        if (task != null) {
            task.cancel();
        }
        autoNextTasks.remove(worldName);
    }
    public String getTimeLeft(String worldName) {

        long remainingMs = getRemainingTime(worldName);


        if (remainingMs <= 0) {
            return "";
        }


        int remainingSec = (int) (remainingMs / 1000);



        return formatTime(remainingSec);
    }
    /**
     * Kích hoạt phá hủy một nhóm block theo ID với Delay
     */
    public void triggerBreakGroup(String worldName, String dungeonId, String groupId, int delayTicks) {

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> locStrings = config.getStringList("break-groups." + groupId);

        if (locStrings.isEmpty()) return;


        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(worldName);
                if (world == null) return;

                for (String s : locStrings) {
                    try {
                        String[] p = s.split(",");
                        if (p.length < 3) continue;

                        double x = Double.parseDouble(p[0].trim());
                        double y = Double.parseDouble(p[1].trim());
                        double z = Double.parseDouble(p[2].trim());
                        Location loc = new Location(world, x, y, z);

                        Block block = loc.getBlock();
                        if (block.getType() != Material.AIR) {

                            world.spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 30, 0.2, 0.2, 0.2, 0.1, block.getBlockData());
                            world.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);

                            block.setType(Material.AIR);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[Dungeon] Lỗi tọa độ break-group: " + s);
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), (long) delayTicks);
    }

    public void triggerPlaceGroup(String worldName, String dungeonId, String groupId, int delayTicks) {

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> locStrings = config.getStringList("place-groups." + groupId);

        if (locStrings.isEmpty()) return;


        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld(worldName);
                if (world == null) return;

                for (String s : locStrings) {
                    try {
                        String[] p = s.split(",");
                        if (p.length < 4) continue;

                        double x = Double.parseDouble(p[0].trim());
                        double y = Double.parseDouble(p[1].trim());
                        double z = Double.parseDouble(p[2].trim());
                        Material mat = Material.matchMaterial(p[3].trim().toUpperCase());

                        if (mat == null) continue;

                        Location loc = new Location(world, x, y, z);
                        Block block = loc.getBlock();

                        block.setType(mat);


                        world.spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1, mat.createBlockData());
                        world.playSound(loc, Sound.BLOCK_STONE_PLACE, 1.0f, 1.2f);

                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[Dungeon] Lỗi tọa độ place-group: " + s);
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), (long) delayTicks);
    }
    public void addBlockToGroup(String dungeonId, String groupType, String groupId, String value) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);


        String path = groupType + "." + groupId;
        List<String> list = config.getStringList(path);

        if (!list.contains(value)) {
            list.add(value);
            config.set(path, list);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void saveBlockGroup(String dungeonId, String groupType, String groupId, List<String> locations) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set(groupType + "." + groupId, locations);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addDamageDealt(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        playerDamageDealt.put(uuid, playerDamageDealt.getOrDefault(uuid, 0.0) + amount);
    }

    public double getDamageDealt(Player player) {
        return playerDamageDealt.getOrDefault(player.getUniqueId(), 0.0);
    }
    public void sendDamageSummary(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;



        FileConfiguration msgCfg = Main.getInstance().getMessagesConfig();

        List<Map.Entry<Player, Double>> damageList = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            double damage = playerDamageDealt.getOrDefault(p.getUniqueId(), 0.0);
            damageList.add(new AbstractMap.SimpleEntry<>(p, damage));
        }

        damageList.sort((a, b) -> b.getValue().compareTo(a.getValue()));


        String header = msgCfg.getString("damage-summary.header", "&6&l⭐ BẢNG XẾP HẠNG SÁT THƯƠNG ⭐");
        String format = msgCfg.getString("damage-summary.format", "&e#%rank%. &b%player% &7- &f%damage% &csát thương");
        String footer = msgCfg.getString("damage-summary.footer", "");


        for (Player p : world.getPlayers()) {
            if (!header.isEmpty()) p.sendMessage(ChatColor.translateAlternateColorCodes('&', header));

            int rank = 1;
            for (Map.Entry<Player, Double> entry : damageList) {

                String line = format
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", entry.getKey().getName())
                        .replace("%damage%", String.format("%,.0f", entry.getValue()));

                p.sendMessage(ChatColor.translateAlternateColorCodes('&', line));

                rank++;
                if (rank > 5) break;
            }

            if (!footer.isEmpty()) p.sendMessage(ChatColor.translateAlternateColorCodes('&', footer));
        }
    }
    public void checkAndGiveDamageRewards(Player player, List<String> tiers) {
        double pDamage = getDamageDealt(player);
        Random random = new Random();

        for (String tier : tiers) {
            try {

                String[] parts = tier.split(":");
                if (parts.length < 3) continue;

                double requiredDmg = Double.parseDouble(parts[0]);
                double chance = Double.parseDouble(parts[1]);
                int itemId = Integer.parseInt(parts[2]);

                if (pDamage >= requiredDmg) {
                    double roll = random.nextDouble() * 100;
                    if (roll <= chance) {

                        ItemStack reward = getRequireItems().get(itemId);

                        if (reward != null) {
                            player.getInventory().addItem(reward.clone());
                        } else {

                            player.sendMessage("§c[Lỗi] ID vật phẩm " + itemId + " không tồn tại trong database!");
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Dungeon] Lỗi format reward: " + tier);
            }
        }
    }
    public void setWorldDifficulty(String worldName, String difficulty) {
        worldDifficulty.put(worldName, difficulty.toUpperCase());
    }
    public void setWorldCheckpoint(String worldName, Location loc) {
        worldCheckpoints.put(worldName, loc);
    }
    public Location getWorldCheckpoint(String worldName) {
        if (worldCheckpoints.containsKey(worldName)) {
            return worldCheckpoints.get(worldName);
        }
        return null;
    }
    public void clearWorldCheckpoint(String worldName) {
        worldCheckpoints.remove(worldName);
    }
    public String getWorldDifficulty(String worldName) {
        return worldDifficulty.getOrDefault(worldName, "EASY");
    }
}
