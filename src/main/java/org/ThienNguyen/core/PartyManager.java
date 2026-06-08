package org.ThienNguyen.core;

import org.ThienNguyen.Main;
import org.ThienNguyen.database.Database;
import org.ThienNguyen.gui.PartyGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    
    private final Map<UUID, Party> playerPartyMap = new ConcurrentHashMap<>();
    private final Map<UUID, Party> invites = new ConcurrentHashMap<>();

    private Database getDB() {
        return Main.getInstance().getDatabase(); 
    }

    private String getMsg(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&',
                Main.getInstance().getMessagesConfig().getString(path, def));
    }

    
    public void createParty(Player leader, String name) {
        if (playerPartyMap.containsKey(leader.getUniqueId())) {
            leader.sendMessage(getMsg("party-manager.already-in-party", "§cBạn đã ở trong một tổ đội rồi!"));
            return;
        }

        
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            int partyId = getDB().createParty(leader.getUniqueId(), name);
            
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                Party party = new Party(leader.getUniqueId(), name);
                party.setDatabaseId(partyId); 

                playerPartyMap.put(leader.getUniqueId(), party);
                party.setReady(leader.getUniqueId(), true);

                leader.sendMessage(getMsg("party-manager.create-success", "§aTạo tổ đội §e%name% §athành công!")
                        .replace("%name%", name));
                PartyGUI.open(leader);
            });
        });
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

        
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            getDB().addMemberToParty(player.getUniqueId(), party.getDatabaseId());

            
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                joinParty(player, party);
                invites.remove(player.getUniqueId());

                String joinMsg = getMsg("party-manager.member-joined", "§e%player% §ađã gia nhập tổ đội!")
                        .replace("%player%", player.getName());

                for (UUID uuid : party.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) p.sendMessage(joinMsg);
                }
                PartyGUI.open(player);
            });
        });
    }
    public void loadAllPartiesFromDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            String sqlParties = "SELECT * FROM parties";
            try (PreparedStatement ps = getDB().getConnection().prepareStatement(sqlParties)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("party_id");
                    UUID leaderUuid = UUID.fromString(rs.getString("leader_uuid"));

                    
                    String partyName = rs.getString("party_name");
                    if (partyName == null || partyName.isEmpty()) {
                        partyName = "Party-" + id; 
                    }

                    Party party = new Party(leaderUuid, partyName);
                    party.setDatabaseId(id);

                    loadMembersForParty(party);
                    playerPartyMap.put(leaderUuid, party);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadMembersForParty(Party party) {
        String sqlMembers = "SELECT member_uuid FROM party_members WHERE party_id = ?";
        try (PreparedStatement ps = getDB().getConnection().prepareStatement(sqlMembers)) {
            ps.setInt(1, party.getDatabaseId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuidStr = rs.getString("member_uuid");
                if (uuidStr == null || uuidStr.isEmpty()) continue;

                UUID memberUuid = UUID.fromString(uuidStr);

                
                if (!party.getMembers().contains(memberUuid)) {
                    party.getMembers().add(memberUuid);
                }

                
                playerPartyMap.put(memberUuid, party);

                
                party.setReady(memberUuid, true);
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("Lỗi khi load thành viên cho Party ID " + party.getDatabaseId() + ": " + e.getMessage());
        }
    }
    
    public void disbandParty(Party party) {
        
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            getDB().deleteParty(party.getDatabaseId());

            
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                String disbandMsg = getMsg("party-manager.party-disbanded", "§cTổ đội đã giải tán.");
                for (UUID uuid : new ArrayList<>(party.getMembers())) {
                    playerPartyMap.remove(uuid);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.closeInventory();
                        p.sendMessage(disbandMsg);
                    }
                }
            });
        });
    }

    

    public Party getParty(Player player) {
        return playerPartyMap.get(player.getUniqueId());
    }

    public void joinParty(Player player, Party party) {
        UUID uuid = player.getUniqueId();
        if (!party.getMembers().contains(uuid)) {
            party.getMembers().add(uuid);
        }
        playerPartyMap.put(uuid, party);
        party.setReady(uuid, true);
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

        invites.put(target.getUniqueId(), party);
        sender.sendMessage(getMsg("party-manager.invite-sent", "§aĐã gửi lời mời cho §e%target%")
                .replace("%target%", target.getName()));

        target.sendMessage("§b[Party] §fBạn nhận được lời mời vào nhóm §e" + party.getName());
    }
    public void removePlayer(UUID uuid) {
        playerPartyMap.remove(uuid);
    }
}