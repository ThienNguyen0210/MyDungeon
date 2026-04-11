package org.ThienNguyen.gui;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyGUI {

    public static void open(Player player) {
        Party party = Main.getInstance().getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage("§cBạn chưa có tổ đội!");
            return;
        }

        FileConfiguration msg = Main.getInstance().getMessagesConfig();

        
        String title = ChatColor.translateAlternateColorCodes('&',
                msg.getString("gui.party-title", "&a&lPARTY: &e%name%")
                        .replace("%name%", party.getName()));

        Inventory inv = Bukkit.createInventory(null, 36, title);

        
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 36; i++) {
            
            if (i < 9 || i > 26 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, filler);
            }
        }

        
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;

        
        String leaderRole = msg.getString("gui.role-leader", "&6Trưởng Nhóm");
        String memberRole = msg.getString("gui.role-member", "&bThành Viên");
        String readyStatus = msg.getString("party.status-ready", "&aSẵn Sàng");
        String notReadyStatus = msg.getString("party.status-not-ready", "&7Chưa Sẵn Sàng");
        String adminActionsText = msg.getString("gui.admin-lore-kick", "");

        for (UUID uuid : party.getMembers()) {
            if (index >= memberSlots.length) break;

            Player p = Bukkit.getPlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) continue;

            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + (p != null ? p.getName() : "Ngoại Tuyến"));

            
            String roleStr = uuid.equals(party.getLeader()) ? leaderRole : memberRole;
            String statusStr = party.isReady(uuid) ? readyStatus : notReadyStatus;

            String adminActions = "";
            
            if (party.getLeader().equals(player.getUniqueId()) && !uuid.equals(player.getUniqueId())) {
                adminActions = adminActionsText;
            }

            
            List<String> lore = new ArrayList<>();
            for (String line : msg.getStringList("gui.member-lore")) {
                
                String formattedLine = line
                        .replace("%role%", roleStr)
                        .replace("%status%", statusStr)
                        .replace("%id%", party.getName()) 
                        .replace("%admin_actions%", adminActions);

                
                if (formattedLine.contains("\n")) {
                    for (String subLine : formattedLine.split("\n")) {
                        if (!subLine.isEmpty()) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', subLine));
                        }
                    }
                } else {
                    
                    if (!line.contains("%admin_actions%") || !adminActions.isEmpty()) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', formattedLine));
                    }
                }
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(memberSlots[index], head);
            index++;
        }

        
        boolean isReady = party.isReady(player.getUniqueId());
        ItemStack ready = new ItemStack(isReady ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta rMeta = ready.getItemMeta();
        if (rMeta != null) {
            String readyText = isReady ?
                    msg.getString("party.button-ready", "&a&l✔ Sẵn Sàng") :
                    msg.getString("party.button-not-ready", "&c&l✘ Chưa Sẵn Sàng");
            rMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', readyText));
            ready.setItemMeta(rMeta);
        }
        inv.setItem(31, ready);

        
        ItemStack leave = new ItemStack(Material.BARRIER);
        ItemMeta lMeta = leave.getItemMeta();
        if (lMeta != null) {
            String leaveText = party.getLeader().equals(player.getUniqueId()) ?
                    msg.getString("party.button-disband", "&c&lGiải Tán Nhóm") :
                    msg.getString("party.button-leave", "&c&lThoát Nhóm");
            lMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', leaveText));
            leave.setItemMeta(lMeta);
        }
        inv.setItem(35, leave);

        player.openInventory(inv);
    }
}