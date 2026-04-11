package org.ThienNguyen.listeners;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.DungeonStage;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.io.File;
import java.util.List;
import java.util.Map;

public class StageListener implements Listener {

    @EventHandler
    public void onMythicDeath(MythicMobDeathEvent event) {
        if (!(event.getKiller() instanceof Player killer)) return;

        World world = killer.getWorld();
        String worldName = world.getName();
        if (!worldName.startsWith("temp_")) return;

        DungeonManager dm = Main.getInstance().getDungeonManager();
        String dungeonId = worldName.split("_")[1];

        DungeonStage stage = dm.getCurrentStage(killer, dungeonId);
        if (stage == null || !stage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) return;

        String deadMobId = event.getMobType().getInternalName();
        boolean isValidTarget = false;

        if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
            isValidTarget = stage.getMultiTargets().stream()
                    .anyMatch(mt -> mt.getMobId().equalsIgnoreCase(deadMobId));
        } else {
            isValidTarget = deadMobId.equalsIgnoreCase(stage.getTarget());
        }

        if (!isValidTarget) return;

        
        int current = dm.getStageProgress(worldName) + 1;
        dm.setStageProgress(worldName, current);
        int goal = stage.getGoal();

        
        
        String progressFormat = Main.getInstance().getMessagesConfig().getString("dungeon.stage.progress", "&7Tiến độ: &e%current%/%goal%");

        
        String progressMsg = ChatColor.translateAlternateColorCodes('&', progressFormat
                .replace("%current%", String.valueOf(current))
                .replace("%goal%", String.valueOf(goal)));

        
        world.getPlayers().forEach(p -> p.sendMessage(progressMsg));
        

        if (current >= goal) {
            dm.setStageProgress(worldName, 0);
            if (dm.isLastStage(killer, dungeonId)) {
                if (killer != null && killer.isOnline()) {
                    dm.winDungeon(killer);
                }
            } else {
                Player leader = world.getPlayers().stream()
                        .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                        .findFirst()
                        .orElse(killer);

                
                for (Player p : world.getPlayers()) {
                    dm.executeCommands(p, stage.getCommands());
                    if (stage.getMessage() != null && !stage.getMessage().isEmpty()) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', stage.getMessage()));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    dm.nextStage(p);
                    int currentIdx = dm.getCurrentStageIndex(p);
                    sendStageTitle(p, dungeonId, currentIdx);
                }

                
                DungeonStage nextStage = dm.getCurrentStage(leader, dungeonId);
                if (nextStage != null && nextStage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) {
                    int nextIdx = dm.getCurrentStageIndex(leader); 
                    dm.spawnStageMobWithDelay(leader, nextStage, dungeonId, nextIdx);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();
        if (!worldName.startsWith("temp_")) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        DungeonManager dm = Main.getInstance().getDungeonManager();
        if (dm.getPlayerState(player) == DungeonManager.DungeonState.END) return;

        
        String dungeonId = worldName.split("_")[1];
        DungeonStage stage = dm.getCurrentStage(player, dungeonId);

        if (stage == null || !stage.getType().equalsIgnoreCase("REACH_LOCATION")) return;

        boolean reached = false;
        double distSq = stage.getDistance() * stage.getDistance();

        if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
            for (DungeonStage.MobTarget mt : stage.getMultiTargets()) {
                Location loc = parseLocationSimple(world, mt.getSpawnLocation());
                if (loc != null && player.getLocation().distanceSquared(loc) <= distSq) {
                    reached = true;
                    break;
                }
            }
        } else if (stage.getTarget() != null) {
            Location loc = parseLocationSimple(world, stage.getTarget());
            if (loc != null && player.getLocation().distanceSquared(loc) <= distSq) {
                reached = true;
            }
        }

        if (reached) {
            Player leader = world.getPlayers().iterator().next();
            int currentIdx = dm.getCurrentStageIndex(leader);
            dungeonId = worldName.split("_")[1];
            File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
            YamlConfiguration dungeonConfig = YamlConfiguration.loadConfiguration(dungeonFile);
            String stageKey = "stages." + currentIdx;
            if (dungeonConfig.getBoolean(stageKey + ".set-checkpoint", false)) {
                Location cpLoc = null;

                
                if (stage.getTarget() != null) {
                    cpLoc = parseLocationSimple(world, stage.getTarget());
                } else if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
                    cpLoc = parseLocationSimple(world, stage.getMultiTargets().get(0).getSpawnLocation());
                }

                if (cpLoc != null) {
                    dm.setWorldCheckpoint(worldName, cpLoc);
                    String msgFormat = Main.getInstance().getMessagesConfig()
                            .getString("dungeon.stage.checkpoint-updated", "&a&l[!] &fĐiểm hồi sinh của đội đã được cập nhật tại đây.");
                    String finalMsg = ChatColor.translateAlternateColorCodes('&', msgFormat);

                    world.getPlayers().forEach(p -> {
                        p.sendMessage(finalMsg);
                        p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.2f);
                    });
                }
            }

            
            if (dungeonConfig.contains(stageKey + ".break-actions")) {
                List<?> actions = dungeonConfig.getList(stageKey + ".break-actions");
                if (actions != null) {
                    for (Object obj : actions) {
                        if (obj instanceof Map<?, ?> action) {
                            String groupId = String.valueOf(action.get("id"));
                            
                            int delay = 0;
                            if (action.get("delay") != null) {
                                delay = ((Number) action.get("delay")).intValue();
                            }
                            dm.triggerBreakGroup(worldName, dungeonId, groupId, delay);
                        }
                    }
                }
            }

            
            if (dungeonConfig.contains(stageKey + ".place-actions")) {
                List<?> actions = dungeonConfig.getList(stageKey + ".place-actions");
                if (actions != null) {
                    for (Object obj : actions) {
                        if (obj instanceof Map<?, ?> action) {
                            String groupId = String.valueOf(action.get("id"));
                            int delay = 0;
                            if (action.get("delay") != null) {
                                delay = ((Number) action.get("delay")).intValue();
                            }
                            dm.triggerPlaceGroup(worldName, dungeonId, groupId, delay);
                        }
                    }
                }
            }

            
            if (dm.isLastStage(leader, dungeonId)) {
                dm.winDungeon(leader);
            } else {
                
                world.getPlayers().forEach(p -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    if (stage.getMessage() != null && !stage.getMessage().isEmpty()) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', stage.getMessage()));
                    }
                });

                
                dm.executeCommands(leader, stage.getCommands());

                
                for (Player p : world.getPlayers()) {
                    dm.nextStage(p);
                    int nextIdx = dm.getCurrentStageIndex(p);
                    sendStageTitle(p, dungeonId, nextIdx);
                }

                
                DungeonStage next = dm.getCurrentStage(leader, dungeonId);
                if (next != null && next.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) {
                    dm.spawnStageMob(leader, next);
                }
            }
        }
    }

    /**
     * Hàm parse tọa độ an toàn
     */
    private Location parseLocationSimple(World world, String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            String[] p = str.split(",");
            if (p.length < 3) return null; 

            double x = Double.parseDouble(p[0].trim());
            double y = Double.parseDouble(p[1].trim());
            double z = Double.parseDouble(p[2].trim());

            return new Location(world, x, y, z);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Dungeon] Tọa độ sai định dạng: " + str);
            return null;
        }
    }
    private void sendStageTitle(Player p, String dungeonId, int stageIndex) {
        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "stages." + stageIndex;

        if (config.contains(path)) {
            String title = config.getString(path + ".title");
            
            String subtitle = config.getString(path + ".subtitle", config.getString(path + ".message", ""));

            
            if (title != null) {
                p.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', title),
                        ChatColor.translateAlternateColorCodes('&', subtitle),
                        10, 40, 10
                );
            }
        } else {
            Bukkit.getLogger().info("[Dungeon] Debug: Khong tim thay path " + path + " cho dungeon " + dungeonId);
        }
    }
}