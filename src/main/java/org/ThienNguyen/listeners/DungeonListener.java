package org.ThienNguyen.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class DungeonListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        
        if (event.getPlayer().getWorld().getName().startsWith("temp_")) {
            
            if (!event.getPlayer().hasPermission("mydungeon.admin.break")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        
        if (event.getPlayer().getWorld().getName().startsWith("temp_")) {
            if (!event.getPlayer().hasPermission("mydungeon.admin.place")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cBạn không thể đặt block trong phó bản!");
            }
        }
    }
}