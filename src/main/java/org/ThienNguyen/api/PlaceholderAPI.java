package org.ThienNguyen.api;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.ThienNguyen.core.DungeonStage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final Main plugin;
    private final DungeonManager dm;

    public PlaceholderAPI(Main plugin) {
        this.plugin = plugin;
        this.dm = plugin.getDungeonManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dungeon";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ThienNguyen";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        String worldName = player.getWorld().getName();
        if (!worldName.startsWith("temp_")) return "";

        String dungeonId = worldName.split("_")[1];
        DungeonStage stage = dm.getCurrentStage(player, dungeonId);
        if (stage == null) return "";

        FileConfiguration cfg = plugin.getConfig();
        String format;

        switch (identifier.toLowerCase()) {
            case "distance_to_reach":
                if (stage.getType().equalsIgnoreCase("REACH_LOCATION")) {
                    
                    Location targetLoc = null;
                    if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
                        for (DungeonStage.MobTarget mt : stage.getMultiTargets()) {
                            Location loc = dm.parseLocationInWorld(player.getWorld(), mt.getSpawnLocation());
                            if (loc != null) {
                                targetLoc = loc;
                                break;
                            }
                        }
                    } else if (stage.getTarget() != null) {
                        targetLoc = dm.parseLocationInWorld(player.getWorld(), stage.getTarget());
                    }

                    if (targetLoc == null) return "";
                    double distance = player.getLocation().distance(targetLoc);
                    format = cfg.getString("placeholders.distance-format", "&e%.1f blocks");
                    return ChatColor.translateAlternateColorCodes('&', String.format(format, distance));
                }

                
                else if (stage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) {
                    String mobId = stage.getTarget();
                    if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
                        mobId = stage.getMultiTargets().get(0).getMobId();
                    }

                    if (mobId == null) return "";

                    final String finalMobId = mobId;
                    
                    var nearestMob = MythicBukkit.inst().getMobManager().getActiveMobs().stream()
                            .filter(am -> am.getType().getInternalName().equalsIgnoreCase(finalMobId))
                            .filter(am -> am.getEntity().getBukkitEntity().getWorld().equals(player.getWorld()))
                            .filter(am -> !am.getEntity().isDead())
                            .min(Comparator.comparingDouble(am ->
                                    am.getEntity().getBukkitEntity().getLocation().distanceSquared(player.getLocation())))
                            .orElse(null);

                    if (nearestMob == null) return "&7N/A";

                    double dist = player.getLocation().distance(nearestMob.getEntity().getBukkitEntity().getLocation());
                    format = cfg.getString("placeholders.distance-format", "&e%.1f blocks");
                    return ChatColor.translateAlternateColorCodes('&', String.format(format, dist));
                }
                return "";

            case "mythic_targets_left":
                if (!stage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) return "";
                int remaining = stage.getGoal() - dm.getStageProgress(worldName);
                format = cfg.getString("placeholders.targets-left-format", "&c%d còn lại");
                return ChatColor.translateAlternateColorCodes('&', String.format(format, remaining));

            case "mythic_mob_name":
                if (!stage.getType().equalsIgnoreCase("KILL_MYTHIC_MOB")) return "";

                String mobId = stage.getTarget();
                if (stage.getMultiTargets() != null && !stage.getMultiTargets().isEmpty()) {
                    mobId = stage.getMultiTargets().get(0).getMobId();
                }

                if (mobId == null || mobId.isEmpty()) return "Không có mob";

                String displayName = mobId; 

                
                var mobType = MythicBukkit.inst().getMobManager().getMythicMob(mobId).orElse(null);

                if (mobType != null) {
                    
                    String configDisplay = mobType.getDisplayName().get();
                    if (configDisplay != null && !configDisplay.isEmpty()) {
                        displayName = configDisplay;
                    }
                }

                format = cfg.getString("placeholders.mob-name-format", "&f%s");
                return ChatColor.translateAlternateColorCodes('&', String.format(format, displayName));
            case "lives_left":
                
                worldName = player.getWorld().getName();
                int livesLeft = 0;

                if (worldName.startsWith("temp_")) {
                    
                    dungeonId = worldName.split("_")[1];

                    int maxLives = dm.getMaxLives(dungeonId);
                    int deaths = dm.getDeathCount(player);
                    livesLeft = Math.max(0, maxLives - deaths);
                }

                format = cfg.getString("placeholders.lives-left-format", "&a%d mạng còn lại");
                return ChatColor.translateAlternateColorCodes('&', String.format(format, livesLeft));
            case "damage_dealt":
                double totalDamage = dm.getDamageDealt(player);
                
                String damageFormat = cfg.getString("placeholders.damage-format", "&e%,.1f");
                return ChatColor.translateAlternateColorCodes('&', String.format(damageFormat, totalDamage));
            case "time_left":
                
                
                String timeLeft = dm.getTimeLeft(worldName);

                if (timeLeft == null || timeLeft.isEmpty()) {
                    return "N/A";
                }

                format = cfg.getString("placeholders.time-left-format", "&b%s");
                return ChatColor.translateAlternateColorCodes('&', String.format(format, timeLeft));
            case "difficulty":
                
                String diffKey = dm.getWorldDifficulty(worldName);

                
                
                String diffDisplay = cfg.getString("difficulty-settings." + diffKey + ".display", diffKey);

                return ChatColor.translateAlternateColorCodes('&', diffDisplay);
            default:
                return null;
        }
    }
}