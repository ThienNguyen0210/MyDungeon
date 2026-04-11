package org.ThienNguyen.listeners;

import org.ThienNguyen.Main;
import org.ThienNguyen.gui.DungeonRequireGUI;
import org.ThienNguyen.utils.ItemSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DungeonRequireListener implements Listener {

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§0Require Manager - Trang ")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        
        int currentPage = 1;
        try {
            currentPage = Integer.parseInt(title.replace("§0Require Manager - Trang ", ""));
        } catch (NumberFormatException e) {
            currentPage = 1;
        }

        
        if (slot == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            DungeonRequireGUI.open(player, currentPage - 1);
            return;
        }
        if (slot == 52 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            DungeonRequireGUI.open(player, currentPage + 1);
            return;
        }

        
        if (slot == 49) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage("§cBạn phải cầm item trên tay!");
                return;
            }

            int nextId = 1;
            while (Main.getInstance().getDungeonManager().getRequireItems().containsKey(nextId)) {
                nextId++;
            }

            Main.getInstance().getDungeonManager().saveRequireToDB(nextId, ItemSerializer.toBase64(hand));
            player.sendMessage("§aĐã thêm item ID: " + nextId);
            
            DungeonRequireGUI.open(player, currentPage);
            return;
        }

        
        if (event.isShiftClick() && event.isLeftClick() && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int id = (currentPage - 1) * 45 + slot + 1;
            Main.getInstance().getDungeonManager().removeRequireFromDB(id);
            player.sendMessage("§cĐã xóa item ID: " + id);
            
            DungeonRequireGUI.open(player, currentPage);
        }
    }
}