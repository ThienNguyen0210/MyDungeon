package org.ThienNguyen.core;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.PartyGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {
    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    
    private final Map<UUID, Party> invites = new HashMap<>();

    private String getMsg(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&',
                Main.getInstance().getMessagesConfig().getString(path, def));
    }

    public void createParty(Player leader, String name) {
        if (playerPartyMap.containsKey(leader.getUniqueId())) {
            leader.sendMessage(getMsg("party-manager.already-in-party", "§cBạn đã ở trong một tổ đội rồi!"));
            return;
        }
        Party party = new Party(leader.getUniqueId(), name);
        playerPartyMap.put(leader.getUniqueId(), party);

        
        party.setReady(leader.getUniqueId(), true);

        leader.sendMessage(getMsg("party-manager.create-success", "§aTạo tổ đội §e%name% §athành công!")
                .replace("%name%", name));

        
        PartyGUI.open(leader);
    }

    
    public void sendInvite(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(getMsg("party-manager.cannot-invite-self", "§cBạn không thể tự mời chính bản thân mình!"));
            return;
        }

        Party party = getParty(sender);
        if (party == null || !party.getLeader().equals(sender.getUniqueId())) {
            sender.sendMessage(getMsg("party-manager.only-leader-invite", "§cChỉ trưởng nhóm mới có quyền mời thành viên!"));
            return;
        }

        if (party.getMembers().contains(target.getUniqueId())) {
            sender.sendMessage(getMsg("party-manager.already-in-your-party", "§cNgười chơi này đã ở trong nhóm của bạn."));
            return;
        }

        invites.put(target.getUniqueId(), party);

        
        sender.sendMessage(getMsg("party-manager.invite-sent", "§aĐã gửi lời mời cho §e%target%")
                .replace("%target%", target.getName()));

        
        List<String> inviteLines = Main.getInstance().getMessagesConfig().getStringList("party-manager.invite-received");
        if (inviteLines.isEmpty()) {
            target.sendMessage("§b[Party] §fBạn nhận được lời mời vào nhóm §e" + party.getName());
            target.sendMessage("§aGõ §7/party accept §ađể đồng ý gia nhập.");
        } else {
            for (String line : inviteLines) {
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', line.replace("%name%", party.getName())));
            }
        }
    }

    public void acceptInvite(Player player) {
        Party party = invites.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(getMsg("party-manager.no-pending-invite", "§cBạn không có lời mời nào đang chờ."));
            return;
        }

        if (playerPartyMap.containsKey(player.getUniqueId())) {
            player.sendMessage(getMsg("party-manager.must-leave-first", "§cBạn phải rời nhóm hiện tại trước khi gia nhập nhóm mới!"));
            return;
        }

        
        joinParty(player, party);
        invites.remove(player.getUniqueId());

        
        String joinMsg = getMsg("party-manager.member-joined", "§e%player% §ađã gia nhập tổ đội!")
                .replace("%player%", player.getName());

        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(joinMsg);
            }
        }

        
        PartyGUI.open(player);
    }

    public void disbandParty(Party party) {
        String disbandMsg = getMsg("party-manager.party-disbanded", "§cTổ đội đã giải tán.");
        for (UUID uuid : new ArrayList<>(party.getMembers())) {
            playerPartyMap.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.closeInventory(); 
                p.sendMessage(disbandMsg);
            }
        }
    }

    public Party getParty(Player player) {
        return playerPartyMap.get(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        playerPartyMap.remove(uuid);
    }

    public void joinParty(Player player, Party party) {
        UUID uuid = player.getUniqueId();
        if (!party.getMembers().contains(uuid)) {
            party.getMembers().add(uuid);
        }
        playerPartyMap.put(uuid, party);

        
        party.setReady(uuid, true);
    }
}