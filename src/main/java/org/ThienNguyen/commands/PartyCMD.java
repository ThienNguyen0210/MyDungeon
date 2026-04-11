package org.ThienNguyen.commands;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.Party;
import org.ThienNguyen.core.PartyManager;
import org.ThienNguyen.gui.PartyGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCMD implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        PartyManager pm = Main.getInstance().getPartyManager();
        Party party = pm.getParty(player);

        if (args.length == 0) {
            if (party == null) {
                player.sendMessage("§c[!] Bạn chưa có tổ đội. Hãy dùng: /party create <tên>");
                return true;
            }
            PartyGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                if (party != null) {
                    player.sendMessage("§c[!] Bạn đã ở trong một tổ đội rồi!");
                    return true;
                }
                if (args.length > 1) {
                    String teamName = args[1];
                    if (teamName.length() < 3 || teamName.length() > 25) {
                        player.sendMessage("§c[!] Tên tổ đội phải từ 3-25 ký tự!");
                        return true;
                    }
                    if (!teamName.matches("^[a-zA-Z0-9]+$")) {
                        player.sendMessage("§c[!] Tên không được chứa dấu hoặc ký tự đặc biệt!");
                        return true;
                    }
                    pm.createParty(player, teamName);
                } else {
                    player.sendMessage("§eSử dụng: /party create <tên>");
                }
                break;

            case "invite":
                if (party == null) {
                    player.sendMessage("§c[!] Bạn phải tạo nhóm trước mới có thể mời!");
                    return true;
                }
                
                if (!party.getLeader().equals(player.getUniqueId())) {
                    player.sendMessage("§c[!] Chỉ trưởng nhóm mới có quyền mời thành viên!");
                    return true;
                }
                if (args.length > 1) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !target.isOnline()) {
                        player.sendMessage("§c[!] Người chơi này không online!");
                        return true;
                    }
                    if (target.equals(player)) {
                        player.sendMessage("§c[!] Bạn không thể tự mời chính mình!");
                        return true;
                    }
                    pm.sendInvite(player, target);
                } else {
                    player.sendMessage("§eSử dụng: /party invite <tên>");
                }
                break;

            case "accept":
                pm.acceptInvite(player);
                break;

            case "leave":
                if (party == null) {
                    player.sendMessage("§c[!] Bạn không ở trong tổ đội nào.");
                    return true;
                }
                
                if (party.getLeader().equals(player.getUniqueId())) {
                    pm.disbandParty(party);
                } else {
                    party.getMembers().remove(player.getUniqueId());
                    pm.removePlayer(player.getUniqueId());
                    player.sendMessage("§c[!] Bạn đã rời khỏi tổ đội.");
                    
                    for (java.util.UUID uuid : party.getMembers()) {
                        Player member = Bukkit.getPlayer(uuid);
                        if (member != null) member.sendMessage("§e" + player.getName() + " §fđã rời khỏi nhóm.");
                    }
                }
                break;

            case "reload": 
                if (player.hasPermission("party.admin")) {
                    Main.getInstance().reloadMessages();
                    player.sendMessage("§a[!] Đã reload messages.yml!");
                }
                break;

            default:
                if (party != null) PartyGUI.open(player);
                else player.sendMessage("§c[!] Lệnh không tồn tại. Dùng /party create để bắt đầu.");
                break;
        }

        return true;
    }
}