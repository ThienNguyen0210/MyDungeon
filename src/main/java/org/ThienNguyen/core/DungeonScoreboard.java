package org.ThienNguyen.core;

import me.clip.placeholderapi.PlaceholderAPI;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

public class DungeonScoreboard {

    
    public static void updateScoreboard(Player player, String dungeonId, int current, int goal, DungeonStage stage, int lives, String timeLeft) {
        FileConfiguration mainConfig = Main.getInstance().getConfig();
        if (!mainConfig.getBoolean("scoreboard.enabled", true)) return;

        Scoreboard board = player.getScoreboard();

        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("dungeon") == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("dungeon");
        if (objective == null) {
            objective = board.registerNewObjective("dungeon", Criteria.DUMMY, "Dungeon");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        DungeonManager dm = Main.getInstance().getDungeonManager();
        FileConfiguration dungeonConfig = dm.getDungeonConfig(dungeonId);
        String dungeonDisplayName = (dungeonConfig != null) ? dungeonConfig.getString("display-name", dungeonId) : dungeonId;

        
        String titleFormat = mainConfig.getString("scoreboard.title", "&lDUNGEON");
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', titleFormat.replace("%display_name%", dungeonDisplayName)));

        
        String questName = (stage != null) ? stage.getName() : "N/A";
        String stageMsg = (stage != null) ? stage.getMessage() : "";

        List<String> lines = mainConfig.getStringList("scoreboard.lines");
        int size = lines.size();

        for (int i = 0; i < size; i++) {
            String rawLine = lines.get(i);

            
            String formatted = rawLine
                    .replace("%quest_name%", questName)
                    .replace("%stage_msg%", stageMsg)
                    .replace("%current%", String.valueOf(current))
                    .replace("%goal%", String.valueOf(goal))
                    .replace("%lives%", String.valueOf(lives))
                    .replace("%time_left%", timeLeft);

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                formatted = PlaceholderAPI.setPlaceholders(player, formatted);
            }
            formatted = ChatColor.translateAlternateColorCodes('&', formatted);

            String teamName = "line_" + i;
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            String entry = ChatColor.values()[i].toString() + "§r";
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
                objective.getScore(entry).setScore(size - i);
            }

            if (!team.getPrefix().equals(formatted)) {
                team.setPrefix(formatted);
            }
        }
    }

    public static void removeScoreboard(Player player) {
        if (player == null) return;
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}