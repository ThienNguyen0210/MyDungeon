package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.DungeonRequireGUI;
import org.bukkit.entity.Player;

public class RequireCommand implements SubCommand {

    @Override
    public void execute(Player player, String[] args) {
        
        if (!player.hasPermission("mydungeon.admin.require")) {
            player.sendMessage("§cBạn không có quyền quản lý vật phẩm yêu cầu!");
            return;
        }

        
        
        
        try {
            DungeonRequireGUI.open(player, 1);
            player.sendMessage("§a§l[!] §fĐang mở trình quản lý vật phẩm yêu cầu...");
        } catch (Exception e) {
            player.sendMessage("§cLỗi khi mở giao diện Require! Hãy kiểm tra console.");
            e.printStackTrace();
        }
    }
}