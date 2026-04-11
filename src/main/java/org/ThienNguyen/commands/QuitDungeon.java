package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.DungeonScoreboard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;

public class QuitDungeon implements SubCommand {
    @Override
    public void execute(Player player, String[] args) {
        final String worldName = player.getWorld().getName();
        final DungeonManager dm = Main.getInstance().getDungeonManager();

        
        if (!worldName.startsWith("temp_")) {
            if (dm.getPlayerState(player) != DungeonManager.DungeonState.NONE) {
                
                dm.setPlayerState(player, DungeonManager.DungeonState.NONE);
                dm.removeTimeBar(player);
                DungeonScoreboard.removeScoreboard(player);

                player.sendMessage("§eDữ liệu phó bản cũ của bạn đã được dọn dẹp.");
            } else {
                player.sendMessage("§cBạn không ở trong phó bản nào cả!");
            }
            return;
        }

        
        if (dm.getPlayerState(player) == DungeonManager.DungeonState.END) {
            return;
        }

        String[] parts = worldName.split("_");
        if (parts.length < 2) return;
        String dungeonId = parts[1];

        File file = new File(Main.getInstance().getDataFolder(), "Dungeons/" + dungeonId + ".yml");
        FileConfiguration msgConfig = Main.getInstance().getMessagesConfig();

        int tempQuitTime = 5;
        Location tempExitLoc = Bukkit.getWorlds().get(0).getSpawnLocation();
        List<String> tempQuitCmds = null;

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            tempQuitTime = config.getInt("quit-time", 5);
            tempQuitCmds = config.getStringList("quit-commands");

            String rawLoc = config.getString("exit-location");
            if (rawLoc != null && rawLoc.contains(",")) {
                tempExitLoc = dm.parseLocation(rawLoc);
            }
        }

        final int finalQuitTime = tempQuitTime;
        final Location finalExitLoc = tempExitLoc;
        final List<String> finalQuitCmds = tempQuitCmds;
        final String title = ChatColor.translateAlternateColorCodes('&',
                msgConfig.getString("dungeon.quit.title", "&c&lKẾT THÚC"));
        final String subtitleTemplate = ChatColor.translateAlternateColorCodes('&',
                msgConfig.getString("dungeon.quit.subtitle", "&fRời đi sau %time%s"));

        dm.setPlayerState(player, DungeonManager.DungeonState.END);

        new BukkitRunnable() {
            int totalTicks = finalQuitTime * 20;
            int currentTick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    dm.shutdownDungeon(worldName, "QUIT");
                    this.cancel();
                    return;
                }

                if (currentTick >= totalTicks) {
                    if (finalQuitCmds != null) {
                        dm.executeCommands(player, finalQuitCmds);
                    }
                    DungeonScoreboard.removeScoreboard(player);
                    dm.removeTimeBar(player);

                    long playersLeft = player.getWorld().getPlayers().stream()
                            .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                            .count();

                    if (playersLeft == 0) {
                        dm.fullCleanupDungeon(worldName);
                        dm.shutdownDungeon(worldName, "QUIT");
                    }

                    player.teleport(finalExitLoc);
                    dm.setPlayerState(player, DungeonManager.DungeonState.NONE);

                    this.cancel();
                    return;
                }

                int secondsLeft = (totalTicks - currentTick - 1) / 20 + 1;
                player.sendTitle(title, subtitleTemplate.replace("%time%", String.valueOf(secondsLeft)), 0, 22, 0);

                if (currentTick % 20 == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                }

                currentTick++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}