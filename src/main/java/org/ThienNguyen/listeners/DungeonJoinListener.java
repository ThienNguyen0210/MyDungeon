package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.DungeonScoreboard;
import org.ThienNguyen.core.DungeonStage;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class DungeonJoinListener implements Listener {

    private final DungeonManager dm = Main.getInstance().getDungeonManager();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!world.getName().startsWith("temp_")) return;

        String worldName = world.getName();
        String dungeonId = worldName.split("_")[1];

        
        long remainingMs = dm.getRemainingTime(worldName);

        
        if (remainingMs <= 0 && remainingMs != -1) {
            player.sendMessage("§cPhó bản đã kết thúc do hết thời gian!");

            File dungeonFile = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);
            Location exitLoc = dm.parseLocation(config.getString("exit-location"));

            player.teleport(exitLoc);
            
            dm.fullCleanupDungeon(worldName);
            return;
        }

        
        if (remainingMs > 0) {
            int sec = (int) (remainingMs / 1000);

            
            
            dm.startTimeLimit(worldName, sec);

            
            DungeonStage currentStage = dm.getCurrentStage(player, dungeonId);
            if (currentStage != null) {
                int progress = dm.getStageProgress(worldName);
                int goal = currentStage.getGoal();
                int lives = 8 - dm.getDeathCount(player);
                String timeStr = dm.formatTime(sec);

                DungeonScoreboard.updateScoreboard(player, dungeonId, progress, goal, currentStage, lives, timeStr);
            }

        }
    }
}