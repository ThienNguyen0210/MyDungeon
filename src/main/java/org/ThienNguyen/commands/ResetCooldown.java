package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public class ResetCooldown implements SubCommand {
    @Override
    public void execute(Player player, String[] args) {
        // Kiểm tra quyền (chỉ Admin mới được reset)
        if (!player.hasPermission("dungeon.admin")) {
            player.sendMessage("§cBạn không có quyền thực hiện lệnh này!");
            return;
        }

        // Kiểm tra tham số: /dungeon reset <player> <stage>
        if (args.length < 3) {
            player.sendMessage("§cSử dụng: /dungeon reset <tên_người_chơi> <số_tầng>");
            return;
        }

        String targetName = args[1];
        int stageNum;

        try {
            stageNum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cSố tầng phải là một con số!");
            return;
        }

        // Lấy UUID từ tên người chơi (ưu tiên người chơi đang online)
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Nếu offline, bạn có thể dùng OfflinePlayer (nhưng cần cẩn thận vì có thể gây lag nhẹ)
            targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        }

        // Thực hiện xóa trong Database
        Main.getInstance().getDatabase().resetPlayerCooldown(targetUUID, stageNum);

        player.sendMessage("§a[!] Đã reset lượt đánh tầng §e" + stageNum + " §acho người chơi §b" + targetName);

        if (target != null && target.isOnline()) {
            target.sendMessage("§a[!] Một quản trị viên đã reset lượt đánh tầng §e" + stageNum + " §acho bạn.");
        }
    }
}