package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.core.Party;
import org.ThienNguyen.core.PartyManager;
import org.ThienNguyen.gui.PartyGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class PartyListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.contains("PARTY:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPartyGUIClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.contains("PARTY:")) return;

        
        
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        
        
        if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                event.getAction() == InventoryAction.HOTBAR_SWAP ||
                event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            return;
        }

        
        if (event.getRawSlot() >= event.getInventory().getSize()) return;

        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        PartyManager pm = Main.getInstance().getPartyManager();
        Party party = pm.getParty(player);
        if (party == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        FileConfiguration msgConfig = Main.getInstance().getMessagesConfig();

        
        if (slot == 31) {
            UUID playerUUID = player.getUniqueId();
            party.toggleReady(playerUUID);

            boolean isReady = party.isReady(playerUUID);
            String statusText = isReady ?
                    msgConfig.getString("party.status-ready", "&aSẴN SÀNG") :
                    msgConfig.getString("party.status-not-ready", "&7CHƯA SẴN SÀNG");

            String template = msgConfig.getString("party.member-ready", "&6&l[!] &e%player% &fđang tham gia %id%: %status%");
            String message = ChatColor.translateAlternateColorCodes('&', template
                    .replace("%player%", player.getName())
                    .replace("%id%", party.getName())
                    .replace("%status%", statusText));

            for (UUID memberUUID : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(message);
                    member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, isReady ? 1.5f : 0.5f);
                }
            }
            PartyGUI.open(player);
            return;
        }

        
        if (slot >= 10 && slot <= 16 && item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null) return;
            UUID targetUUID = meta.getOwningPlayer().getUniqueId();

            if (!party.getLeader().equals(player.getUniqueId()) || targetUUID.equals(player.getUniqueId())) return;

            if (event.getClick() == ClickType.SHIFT_LEFT) { 
                party.getMembers().remove(targetUUID);
                pm.removePlayer(targetUUID);
                Player targetP = Bukkit.getPlayer(targetUUID);
                if (targetP != null) {
                    targetP.sendMessage(ChatColor.translateAlternateColorCodes('&', msgConfig.getString("party.kick-target", "&cBạn bị kick!")));
                    targetP.closeInventory();
                }
                PartyGUI.open(player);
            }
            else if (event.getClick() == ClickType.SHIFT_RIGHT) { 
                party.setLeader(targetUUID);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msgConfig.getString("party.promote-success", "&fĐã nhượng quyền cho %player%").replace("%player%", meta.getOwningPlayer().getName())));
                PartyGUI.open(player);
            }
        }

        
        if (slot == 35) {
            if (party.getLeader().equals(player.getUniqueId())) {
                pm.disbandParty(party);
            } else {
                party.getMembers().remove(player.getUniqueId());
                pm.removePlayer(player.getUniqueId());
            }
            player.closeInventory();
        }
    }
}