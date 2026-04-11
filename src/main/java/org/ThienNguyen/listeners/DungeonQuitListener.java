package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.DungeonManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DungeonQuitListener implements Listener {

    private final DungeonManager dm = Main.getInstance().getDungeonManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        dm.removeTimeBar(player);
        if (!world.getName().startsWith("temp_")) return;

        String worldName = world.getName();
        UUID quitUuid = player.getUniqueId();

        List<Player> onlinePlayers = world.getPlayers().stream()
                .filter(p -> !p.getUniqueId().equals(quitUuid) && p.isOnline())
                .collect(Collectors.toList());

        dm.updateDungeonLastActive(worldName);
        if (dm.getDungeonPartyMembers(worldName).size() <= 1 && onlinePlayers.isEmpty()) {
            endDungeon(world, worldName);
            return;
        }

        if (!onlinePlayers.isEmpty()) {
            for (Player p : onlinePlayers) {
                p.sendMessage("§c" + player.getName() + " đã rời game. Phó bản vẫn tiếp tục!");
            }
        }
    }

    private void endDungeon(World world, String worldName) {
        
        for (Player p : world.getPlayers()) {
            p.sendMessage("§cMọi người đã rời game. Phó bản sẽ bị hủy sau 5 phút nếu không ai quay lại!");
        }

        
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            World currentWorld = Bukkit.getWorld(worldName);

            
            if (currentWorld != null && currentWorld.getPlayers().isEmpty()) {

                
                dm.fullCleanupDungeon(worldName);

                Bukkit.unloadWorld(currentWorld, false);

                
                Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                    try {
                        dm.deleteDirectory(new File(Bukkit.getWorldContainer(), worldName).toPath());
                    } catch (Exception e) { e.printStackTrace(); }
                });

                Bukkit.getLogger().info("§cDa huy pho ban " + worldName + " do nguoi choi khong quay lai.");
            }
        }, 20L * 60 * 5); 
    }
}