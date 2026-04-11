package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DungeonDamageListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterDamage(EntityDamageByEntityEvent event) {
        
        if (event.getDamager() instanceof Player player) {
            String worldName = player.getWorld().getName();

            
            if (worldName.startsWith("temp_")) {
                double damage = event.getFinalDamage();

                
                Main.getInstance().getDungeonManager().addDamageDealt(player, damage);
            }
        }
    }
}