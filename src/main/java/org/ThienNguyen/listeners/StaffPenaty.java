package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;
import java.util.UUID;

public class StaffPenaty implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUnauthorizedTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;

        String worldName = to.getWorld().getName();

        
        if (!worldName.startsWith("temp_")) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        DungeonManager dm = Main.getInstance().getDungeonManager();

        
        Set<UUID> authorizedPlayers = dm.getPlayersInDungeon(worldName);

        
        if (!authorizedPlayers.contains(uuid)) {
            
            event.setCancelled(true);

            
            String msg = Main.getInstance().getMessagesConfig()
                    .getString("dungeon.errors.unauthorized-access", "&c&lRA NGOÀI MAU!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

            
            Bukkit.getLogger().warning("[Dungeon Security] " + player.getName() + " đã cố gắng truy cập trái phép vào " + worldName);

            
            String dungeonId = worldName.split("_")[1];
            var dungeonConfig = dm.getDungeonConfig(dungeonId);
            if (dungeonConfig != null) {
                String exitStr = dungeonConfig.getString("exit-location");
                Location exitLoc = dm.parseLocation(exitStr);

                if (exitLoc != null) {
                    player.teleport(exitLoc);
                } else {
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }
            }
        }
    }
}