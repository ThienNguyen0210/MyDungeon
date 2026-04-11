package org.ThienNguyen.listeners;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class DungeonMobListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            if (io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().isMythicMob(entity)) return;
        }

        applyDifficulty(entity);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            applyDifficulty(entity);
        }
    }

    private void applyDifficulty(LivingEntity entity) {
        String worldName = entity.getWorld().getName();
        if (!worldName.startsWith("temp_")) return;

        
        
        String difficultyKey = Main.getInstance().getDungeonManager().getWorldDifficulty(worldName);
        

        FileConfiguration config = Main.getInstance().getConfig();
        String path = "difficulty-settings." + difficultyKey + ".";

        
        double hpMulti = config.getDouble(path + "health-multiplier", 1.0);
        double dmgMulti = config.getDouble(path + "damage-multiplier", 1.0);
        double speedMulti = config.getDouble(path + "speed-multiplier", 1.0);

        

        
        if (hpMulti != 1.0) {
            AttributeInstance maxHp = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHp != null) {
                double newHp = maxHp.getBaseValue() * hpMulti;
                maxHp.setBaseValue(newHp);
                entity.setHealth(newHp);
            }
        }

        
        if (dmgMulti != 1.0) {
            AttributeInstance attackDmg = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackDmg != null) {
                attackDmg.setBaseValue(attackDmg.getBaseValue() * dmgMulti);
            }
        }

        
        if (speedMulti != 1.0) {
            AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * speedMulti);
            }
        }
    }
}